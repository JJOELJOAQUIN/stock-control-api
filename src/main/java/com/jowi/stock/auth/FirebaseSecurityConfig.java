package com.jowi.stock.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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

    @Value("${security.firebase.enabled:true}")
    private boolean firebaseEnabled;

    private static final Logger log = LoggerFactory.getLogger(FirebaseSecurityConfig.class);

    private final CorsConfigurationSource corsConfigurationSource;
    private final AppUserService appUserService;
    private final Environment environment;

    public FirebaseSecurityConfig(
            CorsConfigurationSource corsConfigurationSource,
            AppUserService appUserService,
            Environment environment) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.appUserService = appUserService;
        this.environment = environment;
    }

    @PostConstruct
    public void logSecurityMode() {
        log.info("security.firebase.enabled = {}", firebaseEnabled);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!firebaseEnabled) {
            // Autenticación DESACTIVADA: solo se tolera en el perfil 'local'.
            // El filtro dev autentica a cualquiera como ADMIN, así que en
            // cualquier otro entorno esto es un agujero total. Antes el default
            // era fail-open; ahora falla RUIDOSAMENTE si se intenta arrancar sin
            // auth fuera de local, en vez de abrir la puerta en silencio.
            boolean isLocal = java.util.Arrays.asList(environment.getActiveProfiles())
                    .contains("local");
            if (!isLocal) {
                throw new IllegalStateException(
                    "security.firebase.enabled=false solo se permite con el perfil 'local'. "
                    + "Se abortó el arranque para no exponer la API sin autenticación.");
            }
            log.warn("AUTENTICACIÓN DESACTIVADA (perfil local): DevAdminAuthenticationFilter "
                    + "autentica todo como ADMIN. Nunca usar fuera de local.");
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
                                // "/actuator/**" quitado: no hay dependencia de
                                // actuator y, si se agrega, debe requerir auth.
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

                        // Tratamientos / pacientes / pagos (peeling y futuros protocolos).
                        .requestMatchers("/api/treatments/**").hasAnyRole("ADMIN", "COSMETOLOGA")

                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "USER", "COSMETOLOGA")
                        .anyRequest().authenticated())
                .addFilterBefore(new FirebaseAuthenticationFilter(appUserService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}