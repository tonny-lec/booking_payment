package com.booking.iam.adapter.web.security;

import com.booking.iam.adapter.web.config.JwtProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * IAM security configuration.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(401)
                ))
                .anonymous(Customizer.withDefaults());

        jwtAuthenticationFilterProvider.ifAvailable(
                jwtFilter -> http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        );

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.security.jwt", name = "public-key-pem")
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtProperties jwtProperties) {
        return new JwtAuthenticationFilter(
                JwtAuthenticationFilter.parsePublicKey(jwtProperties.getPublicKeyPem()),
                jwtProperties.getIssuer(),
                jwtProperties.getAudience()
        );
    }
}
