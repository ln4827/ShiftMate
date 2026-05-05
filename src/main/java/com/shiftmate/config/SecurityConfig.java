package com.shiftmate.config;

import com.shiftmate.security.ShiftMateUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for ShiftMate.
 *
 * <p>Designed for a stateless REST API consumed by a React SPA. Key decisions:
 * <ul>
 *   <li>CSRF disabled — the frontend uses CORS and session cookies are SameSite.</li>
 *   <li>CORS allows {@code localhost:5173} with credentials so the Vite dev server
 *       can communicate with the backend during development.</li>
 *   <li>Unauthenticated API requests receive a JSON 401 body rather than an HTML
 *       redirect, so the frontend can handle them programmatically.</li>
 *   <li>Static assets and the React SPA shell are public; API routes require auth.</li>
 *   <li>Method-level security ({@code @PreAuthorize}) is enabled for fine-grained
 *       role checks at the controller level.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ShiftMateUserDetailsService userDetailsService;

    /**
     * BCrypt password encoder with strength 12, balancing security and performance.
     *
     * @return the configured {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Exposes the {@link AuthenticationManager} as a Spring bean so it can be
     * injected into {@link com.shiftmate.controller.AuthController}.
     *
     * @param config Spring's authentication configuration
     * @return the application's {@link AuthenticationManager}
     * @throws Exception if the manager cannot be built
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Wires the custom {@link ShiftMateUserDetailsService} and password encoder
     * into a DAO authentication provider.
     *
     * @return the configured {@link DaoAuthenticationProvider}
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * CORS configuration that allows the Vite development server at
     * {@code localhost:5173} to send credentialed requests to the backend.
     *
     * @return the {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Defines the HTTP security filter chain. Public paths are limited to the
     * auth endpoints and static frontend assets; all other API paths require
     * an authenticated session.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the built {@link org.springframework.security.web.SecurityFilterChain}
     * @throws Exception if the filter chain cannot be configured
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/", "/index.html", "/assets/**", "/*.js", "/*.css", "/*.ico", "/*.svg").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Forbidden\"}");
                })
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(3)
            )
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
