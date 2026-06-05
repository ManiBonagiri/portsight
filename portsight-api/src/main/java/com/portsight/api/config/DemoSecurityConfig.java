package com.portsight.api.config;

import com.portsight.api.domains.auth.User;
import com.portsight.api.domains.auth.UserRepository;
import com.portsight.api.domains.auth.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration("demoSecurityConfig")
@Slf4j
@RequiredArgsConstructor
public class DemoSecurityConfig {

    @Bean
    @Order(2)
    public CommandLineRunner demoUserSeeder(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            seedUser(userRepository, passwordEncoder,
                    "investor@portsight.com", "Alex", "Investor", UserRole.ROLE_USER);
            seedUser(userRepository, passwordEncoder,
                    "manager@portsight.com", "Recruiter", "Demo", UserRole.ROLE_ANALYST);
            seedUser(userRepository, passwordEncoder,
                    "admin@portsight.com", "Admin", "User", UserRole.ROLE_ADMIN);
        };
    }

    private void seedUser(UserRepository repo, PasswordEncoder encoder,
            String email, String firstName, String lastName, UserRole role) {
        if (repo.findByEmail(email).isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .passwordHash(encoder.encode("Demo@1234"))
                    .firstName(firstName)
                    .lastName(lastName)
                    .role(role)
                    .status("ACTIVE")
                    .build();
            repo.save(user);
            log.info("Demo user created: {} ({})", email, role);
        } else {
            log.info("Demo user already exists: {}", email);
        }
    }
}