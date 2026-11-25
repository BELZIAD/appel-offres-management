package com.aomanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupSecurityValidator implements ApplicationRunner {
    private static final List<String> WEAK_VALUES = List.of(
            "change-this-development-secret-change-this-development-secret",
            "change_me",
            "CHANGE_ME",
            "appel_password",
            "Admin123!",
            "password",
            "secret"
    );

    @Value("${app.version.environment:PRODUCTION}")
    private String environment;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${app.dev-admin.password:}")
    private String devAdminPassword;

    @Override
    public void run(ApplicationArguments args) {
        boolean sensitive = !"DEV".equalsIgnoreCase(environment) && !"DEMO".equalsIgnoreCase(environment) && !"TEST".equalsIgnoreCase(environment);
        validateSecret("APP_JWT_SECRET", jwtSecret, sensitive, 32);
        validateSecret("POSTGRES_PASSWORD / SPRING_DATASOURCE_PASSWORD", datasourcePassword, sensitive, 12);
        validateSecret("APP_DEV_ADMIN_PASSWORD", devAdminPassword, sensitive, 12);
    }

    private void validateSecret(String name, String value, boolean sensitive, int minLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " est obligatoire; configurez-le hors du code source.");
        }
        boolean weak = value.length() < minLength || WEAK_VALUES.stream().anyMatch(value::equalsIgnoreCase);
        if (sensitive && weak) {
            throw new IllegalStateException(name + " est absent ou trop faible pour cet environnement.");
        }
    }
}
