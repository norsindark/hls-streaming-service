package com.hls.streaming.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "HLS Streaming Service API",
                version = "1.0.0",
                description = "API documentation for the HLS Streaming Platform (Mini-YouTube).",
                contact = @Contact(
                        name = "SinD",
                        email = "norsinlight@gmail.com")),
        security = {
                @SecurityRequirement(name = "bearerAuth")
        })
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Authorization header using the Bearer scheme. Enter your token here (Password Verification Token or Access Token).",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {}
