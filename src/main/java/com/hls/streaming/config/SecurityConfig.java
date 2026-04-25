package com.hls.streaming.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.config.properties.AuthorizationRuleConfigProperties;
import com.hls.streaming.security.component.TokenClaimExtractor;
import com.hls.streaming.security.entrypoint.RestAccessDeniedHandler;
import com.hls.streaming.security.entrypoint.RestAuthenticationEntryPoint;
import com.hls.streaming.security.jwt.JwtAuthenticationFilter;
import com.hls.streaming.security.jwt.JwtAuthenticationVerifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableMethodSecurity(proxyTargetClass = true)
public class SecurityConfig {

    @Value("${io.hls.api-security.enabled:true}")
    private boolean globalApiSecurityEnabled;

    private final HandlerExceptionResolver exceptionResolver;
    private final AuthorizationRuleConfigProperties authorizationRuleConfig;
    private final ObjectMapper objectMapper;
    private final TokenClaimExtractor tokenClaimExtractor;
    private final JwtAuthenticationVerifier authenticationVerifier;
    private final ErrorCodeConfig errorCodeConfig;

    public SecurityConfig(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
            AuthorizationRuleConfigProperties authorizationRuleConfig,
            ObjectMapper objectMapper,
            TokenClaimExtractor tokenClaimExtractor,
            JwtAuthenticationVerifier authenticationVerifier,
            ErrorCodeConfig errorCodeConfig) {
        this.exceptionResolver = exceptionResolver;
        this.authorizationRuleConfig = authorizationRuleConfig;
        this.objectMapper = objectMapper;
        this.tokenClaimExtractor = tokenClaimExtractor;
        this.authenticationVerifier = authenticationVerifier;
        this.errorCodeConfig = errorCodeConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        if (BooleanUtils.isTrue(globalApiSecurityEnabled)) {

            log.info("Spring Global Security enabled");

            http
                    .cors(cors -> cors
                            .configurationSource(corsConfigurationSource()))
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(authorizationRuleConfig
                                    .getSkippedAuthorization()
                                    .getSkippedApis()
                                    .toArray(String[]::new))
                            .permitAll()
                            .anyRequest().authenticated())
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                            .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)));

            http.addFilterBefore(
                    new JwtAuthenticationFilter(
                            exceptionResolver,
                            authorizationRuleConfig,
                            tokenClaimExtractor,
                            authenticationVerifier,
                            errorCodeConfig),
                    UsernamePasswordAuthenticationFilter.class);
        } else {
            log.warn("Security disabled");
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowCredentials(false);
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
