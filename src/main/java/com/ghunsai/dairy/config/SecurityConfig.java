package com.ghunsai.dairy.config;

import com.ghunsai.dairy.security.JsonSecurityHandlers;
import com.ghunsai.dairy.security.JwtAuthConverter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Single place that defines WHO can call WHAT.
 *
 * <pre>
 *  public ........ login, /api/home, /actuator/health
 *  OWNER only .... users, dashboard, reports, products create/update/price, stock purchase / adjust /
 *                  movements, sales list, customer edit, all-customer balances
 *  OWNER + WORKER  billing counter: create sale, view own bills, products (read), stock level (read),
 *                  customers (list / add / balance), udhaar payments
 *  anything else . denied
 * </pre>
 * Rules are evaluated top to bottom, the first match wins.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppProperties.class)
public class SecurityConfig {

    private static final String OWNER = "OWNER";
    private static final String WORKER = "WORKER";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtDecoder jwtDecoder,
                                            JwtAuthConverter jwtAuthConverter,
                                            JsonSecurityHandlers handlers,
                                            CorsConfigurationSource corsSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ---- public ----
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/home").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // ---- OWNER only (must stay above the shared rules) ----
                        .requestMatchers("/api/users", "/api/users/**").hasRole(OWNER)
                        .requestMatchers("/api/dashboard/**").hasRole(OWNER)
                        .requestMatchers(HttpMethod.GET, "/api/sales", "/api/sales/reports/daily").hasRole(OWNER)
                        .requestMatchers(HttpMethod.GET, "/api/stock/movements").hasRole(OWNER)
                        .requestMatchers(HttpMethod.POST, "/api/stock/purchase", "/api/stock/adjust").hasRole(OWNER)
                        .requestMatchers(HttpMethod.GET, "/api/products/*/price-history").hasRole(OWNER)
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole(OWNER)
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole(OWNER)
                        .requestMatchers(HttpMethod.GET, "/api/customers/balances").hasRole(OWNER)
                        .requestMatchers(HttpMethod.PUT, "/api/customers/**").hasRole(OWNER)

                        // ---- OWNER + WORKER ----
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole(OWNER, WORKER)
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-password").hasAnyRole(OWNER, WORKER)
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*").hasAnyRole(OWNER, WORKER)
                        .requestMatchers(HttpMethod.GET, "/api/customers", "/api/customers/*/balance").hasAnyRole(OWNER, WORKER)
                        .requestMatchers(HttpMethod.POST, "/api/customers").hasAnyRole(OWNER, WORKER)
                        .requestMatchers(HttpMethod.GET, "/api/customer-payments/*").hasAnyRole(OWNER, WORKER)
                        .requestMatchers(HttpMethod.POST, "/api/customer-payments/*").hasAnyRole(OWNER, WORKER)
                        .requestMatchers(HttpMethod.POST, "/api/sales").hasAnyRole(OWNER, WORKER)
                        .requestMatchers(HttpMethod.GET, "/api/sales/*", "/api/sales/reports/my-daily").hasAnyRole(OWNER, WORKER)
                        .requestMatchers(HttpMethod.GET, "/api/stock/report", "/api/stock/*").hasAnyRole(OWNER, WORKER)

                        // ---- default: deny ----
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthConverter::convert))
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    JwtEncoder jwtEncoder(AppProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(props)));
    }

    @Bean
    JwtDecoder jwtDecoder(AppProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(props))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // validates signature + expiry (default validators) + issuer
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.jwt().issuer()));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = props.cors() == null || props.cors().allowedOrigins() == null
                ? List.of()
                : props.cors().allowedOrigins().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (!origins.isEmpty()) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(origins);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
            config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
            config.setMaxAge(3600L);
            source.registerCorsConfiguration("/api/**", config);
        }
        return source;
    }

    private static SecretKey secretKey(AppProperties props) {
        byte[] bytes = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
