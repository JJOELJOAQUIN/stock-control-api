package com.jowi.stock.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!firebaseEnabled) {
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .addFilterBefore(new DevAdminAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                    .build();
        }

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/actuator/**",
                                "/api/public/**")
                        .permitAll()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Lectura de productos (catálogo, with-stock, scan): ADMIN + COSMETOLOGA.
                        .requestMatchers(HttpMethod.GET, "/api/products/**")
                        .hasAnyRole("ADMIN", "COSMETOLOGA")
                        // Alta / edición / baja de productos: solo ADMIN.
                        .requestMatchers("/api/products/**").hasRole("ADMIN")

                        // Compra de productos: fuera del alcance de COSMETOLOGA (igual que en el front).
                        .requestMatchers(HttpMethod.POST, "/api/business/purchase")
                        .hasAnyRole("ADMIN", "USER")
                        // Resto de operaciones de negocio (venta por código de barras, etc.).
                        .requestMatchers("/api/business/**").hasAnyRole("ADMIN", "USER", "COSMETOLOGA")

                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "USER", "COSMETOLOGA")
                        .requestMatchers("/api/stock/**").hasAnyRole("ADMIN", "COSMETOLOGA")
                        .anyRequest().authenticated())
                .addFilterBefore(new FirebaseAuthenticationFilter(appUserService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}