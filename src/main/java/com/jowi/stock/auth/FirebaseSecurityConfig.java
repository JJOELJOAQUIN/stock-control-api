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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
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
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Cabeceras de seguridad. Spring ya manda X-Content-Type-Options:
                // nosniff y X-Frame-Options por default; acá lo hacemos explícito
                // y sumamos HSTS (fuerza HTTPS en el navegador durante 1 año) y
                // una Referrer-Policy conservadora.
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

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

        // Swagger / OpenAPI: público solo en dev o local. En prod se deniega el
        // acceso anónimo al mapa del API (no exponer endpoints ni schemas).
        boolean docsExposed = java.util.Arrays.stream(environment.getActiveProfiles())
                .anyMatch(prof -> prof.equals("local") || prof.equals("dev"));

        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // "/actuator/**" no se lista: no hay dependencia de actuator y,
                    // si se agrega, debe requerir auth (cae en anyRequest().authenticated()).
                    if (docsExposed) {
                        auth.requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**").permitAll();
                    } else {
                        auth.requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**").denyAll();
                    }

                    auth.requestMatchers("/api/public/**").permitAll();
                    auth.requestMatchers("/api/auth/me").authenticated();
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");

                    // Productos: ADMIN y COSMETOLOGA pueden ver, crear, editar y
                    // desactivar (una sola regla cubre GET/POST/PUT/DELETE).
                    auth.requestMatchers("/api/products/**")
                            .hasAnyRole("ADMIN", "COSMETOLOGA");

                    // Compra de productos: fuera del alcance de COSMETOLOGA (igual que en el front).
                    auth.requestMatchers(HttpMethod.POST, "/api/business/purchase")
                            .hasAnyRole("ADMIN", "USER");
                    // Resto de operaciones de negocio (venta por código de barras, etc.).
                    auth.requestMatchers("/api/business/**").hasAnyRole("ADMIN", "USER", "COSMETOLOGA");

                    auth.requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "USER", "COSMETOLOGA");
                    auth.requestMatchers("/api/stock/**").hasAnyRole("ADMIN", "COSMETOLOGA");

                    // Tratamientos / pacientes / pagos (peeling y futuros protocolos).
                    auth.requestMatchers("/api/treatments/**").hasAnyRole("ADMIN", "COSMETOLOGA");

                    // Cierre por defecto: todo lo no matcheado exige autenticación.
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(new FirebaseAuthenticationFilter(appUserService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
