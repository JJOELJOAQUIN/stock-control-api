package com.jowi.stock.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableMethodSecurity
public class FirebaseSecurityConfig {

    @Value("${security.firebase.enabled:false}")
    private boolean firebaseEnabled;

    private final CorsConfigurationSource corsConfigurationSource;
    private final AppUserService appUserService;

    public FirebaseSecurityConfig(
            CorsConfigurationSource corsConfigurationSource,
            AppUserService appUserService) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.appUserService = appUserService;
    }

    @PostConstruct
    public void logSecurityMode() {
        System.out.println(">>> security.firebase.enabled = " + firebaseEnabled);
    }

    @Bean
    public FirebaseAuthenticationFilter firebaseAuthenticationFilter() {
        return new FirebaseAuthenticationFilter(appUserService);
    }

    @Bean
    public DevAdminAuthenticationFilter devAdminAuthenticationFilter() {
        return new DevAdminAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FirebaseAuthenticationFilter firebaseAuthenticationFilter,
            DevAdminAuthenticationFilter devAdminAuthenticationFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!firebaseEnabled) {
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .addFilterBefore(devAdminAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                    .build();
        }

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/actuator/**",
                                "/api/public/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/business/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/products/**").authenticated()
                        .requestMatchers("/api/stock/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}