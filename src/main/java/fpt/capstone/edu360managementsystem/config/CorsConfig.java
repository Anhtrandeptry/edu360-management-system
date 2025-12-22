package fpt.capstone.edu360managementsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration for the application.
 * Defines allowed origins, methods, headers, and credentials for cross-origin requests.
 *
 * @author 360edu
 * @version 1.0
 */
@Configuration
public class CorsConfig {

    /**
     * Creates and configures the CORS configuration source.
     * Allowed origins: localhost:8386, 360edu.online, www.360edu.online.
     * Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS.
     * Allowed headers: Authorization, Content-Type, X-Requested-With, Accept, Origin.
     * Exposed headers: Set-Cookie, Authorization.
     * Max age: 3600 seconds (1 hour).
     *
     * @return the configured {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // ✅ FIXED: 2025-12-22 - Cập nhật CORS cho production deployment
        // Vấn đề: Chỉ allow localhost:8386, thiếu các port khác và subdomain
        // Giải pháp: Thêm wildcard cho localhost và subdomain
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:*",           // ADDED: Allow all localhost ports (dev)
                "https://360edu.online",
                "https://www.360edu.online",
                "https://*.360edu.online"        // ADDED: Allow subdomains (api.360edu.online, etc.)
        ));

        cfg.setAllowCredentials(true);

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        cfg.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}

