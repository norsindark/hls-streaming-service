package com.hls.streaming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
})
@EnableAspectJAutoProxy
@EnableRetry
public class HlsApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(HlsApplication.class, args);
        logApplicationStartup(context.getEnvironment());
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path"))
                .filter(s -> !s.isBlank())
                .orElse("/");

        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("Cannot determine host address, fallback to localhost");
        }

        String appName = env.getProperty("spring.application.name", "application");

        log.info("""
            ----------------------------------------------------------
            Application '{}' is running!
            Local:      {}://localhost:{}{}
            External:   {}://{}:{}{}
            Profile(s): {}
            ----------------------------------------------------------
            """,
                appName,
                protocol, serverPort, contextPath,
                protocol, hostAddress, serverPort, contextPath,
                Arrays.toString(env.getActiveProfiles())
        );
    }
}
