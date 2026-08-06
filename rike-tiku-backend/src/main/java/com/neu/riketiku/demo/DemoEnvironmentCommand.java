package com.neu.riketiku.demo;

import com.neu.riketiku.RikeTikuBackendApplication;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Explicit local command entry point. Normal application startup never invokes demo seeding. */
public final class DemoEnvironmentCommand {
    private DemoEnvironmentCommand() {
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RikeTikuBackendApplication.class);
        application.setDefaultProperties(Map.of(
                "spring.main.banner-mode", "off",
                "server.port", "0",
                "springdoc.api-docs.enabled", "false"));
        try (ConfigurableApplicationContext context = application.run(args)) {
            DemoDataService service = context.getBean(DemoDataService.class);
            String action = context.getEnvironment().getProperty("demo.action", "validate");
            switch (action) {
                case "migrate" -> service.validateSchema();
                case "seed" -> service.seed();
                case "validate" -> service.validateSeed();
                case "clean" -> service.clean();
                default -> throw new IllegalArgumentException("不支持的演示环境操作: " + action);
            }
        }
    }
}
