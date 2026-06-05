package com.portsight.api.shared.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final RequestMappingHandlerMapping handlerMapping;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(
            StringRedisTemplate redisTemplate,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.handlerMapping = handlerMapping;
        this.objectMapper = objectMapper;
    }

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String REDIS_PREFIX = "idempotency:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        Idempotent annotation = null;
        try {
            HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
            if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod handlerMethod) {
                annotation = handlerMethod.getMethodAnnotation(Idempotent.class);
            }
        } catch (Exception e) {
            log.warn("Failed to resolve handler annotation for idempotency check: {}", e.getMessage());
        }

        if (annotation == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            sendErrorResponse(response, HttpStatus.BAD_REQUEST, "Missing required header: " + IDEMPOTENCY_KEY_HEADER);
            return;
        }

        String redisKey = REDIS_PREFIX + key;
        String existingStatus = redisTemplate.opsForValue().get(redisKey);

        if (existingStatus != null) {
            if ("IN_PROGRESS".equals(existingStatus)) {
                sendErrorResponse(response, HttpStatus.CONFLICT,
                        "A request with the same idempotency key is already in progress.");
                return;
            } else {
                String cachedResponse = redisTemplate.opsForValue().get(redisKey + ":response");
                if (cachedResponse != null) {
                    log.info("Returning cached response for idempotency key: {}", key);
                    response.setStatus(HttpStatus.OK.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(cachedResponse);
                    return;
                }
            }
        }

        redisTemplate.opsForValue().set(redisKey, "IN_PROGRESS", 2, TimeUnit.MINUTES);

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, wrappedResponse);

            int status = wrappedResponse.getStatus();
            if (status >= 200 && status < 300) {
                byte[] responseBody = wrappedResponse.getContentAsByteArray();
                String bodyStr = new String(responseBody, wrappedResponse.getCharacterEncoding());
                redisTemplate.opsForValue().set(redisKey, "COMPLETED", Duration.ofSeconds(annotation.ttl()));
                redisTemplate.opsForValue().set(redisKey + ":response", bodyStr, Duration.ofSeconds(annotation.ttl()));
            } else {
                redisTemplate.delete(redisKey);
            }
        } catch (Exception ex) {
            redisTemplate.delete(redisKey);
            throw ex;
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = String.format("{\"success\":false,\"error\":{\"code\":\"IDEMPOTENCY_ERROR\",\"message\":\"%s\"}}",
                message);
        response.getWriter().write(json);
    }
}