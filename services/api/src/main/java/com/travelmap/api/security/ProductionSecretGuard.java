package com.travelmap.api.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class ProductionSecretGuard implements ApplicationRunner {
    private final Environment environment;
    public ProductionSecretGuard(Environment environment) { this.environment = environment; }
    @Override public void run(ApplicationArguments args) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            requireStrong("travelmap.security.jwt-secret");
            requireStrong("travelmap.payment.webhook-secret");
        }
    }
    private void requireStrong(String key) {
        String value = environment.getProperty(key, "");
        if (value.length() < 32 || value.toLowerCase().contains("local") || value.toLowerCase().contains("change"))
            throw new IllegalStateException(key + " must be supplied as a strong production secret");
    }
}
