package com.hls.streaming.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.config.properties.AuthorizationRuleConfigProperties;
import com.hls.streaming.security.component.TokenClaimExtractor;
import com.hls.streaming.security.entrypoint.RestAccessDeniedHandler;
import com.hls.streaming.security.entrypoint.RestAuthenticationEntryPoint;
import com.hls.streaming.security.jwt.JwtAuthenticationFilter;
import com.hls.streaming.security.jwt.JwtAuthenticationVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${io.hls.api-security.enabled:true}")
    private boolean globalApiSecurityEnabled;

    private final AuthorizationRuleConfigProperties authorizationRuleConfig;
    private final ObjectMapper objectMapper;
    private final TokenClaimExtractor tokenClaimExtractor;
    private final JwtAuthenticationVerifier authenticationVerifier;
    private final ErrorCodeConfig errorCodeConfig;

    @Bean
    public org.springframework.security.web.SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        if (BooleanUtils.isTrue(globalApiSecurityEnabled)) {
            log.info("Spring Global Security enabled");
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(authorizationRuleConfig
                                    .getSkippedAuthorization()
                                    .getSkippedApis()
                                    .toArray(String[]::new)
                            ).permitAll()
                            .anyRequest().authenticated()
                    )
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                            .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper))
                    );

            // JWT Authentication Filter
            http.addFilterBefore(
                    new JwtAuthenticationFilter(
                            authorizationRuleConfig,
                            tokenClaimExtractor,
                            authenticationVerifier,
                            errorCodeConfig
                    ), UsernamePasswordAuthenticationFilter.class);
        } else {
            log.warn("Security disabled");
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }
}
