# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# Uses Maven image directly — no Maven wrapper (mvnw) needed
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy pom first — layer cache means deps only re-download if pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src/ src/
RUN mvn clean package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S portsight && adduser -S portsight -G portsight

# Copy the built JAR from builder stage
COPY --from=builder /build/target/portsight-api-1.0.0.jar app.jar

RUN chown portsight:portsight app.jar

USER portsight

EXPOSE 8080

# Health check via Spring Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]