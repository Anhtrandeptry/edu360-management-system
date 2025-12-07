//package fpt.capstone.edu360managementsystem.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@ConfigurationProperties
//public class CorsConfig {
//    @Value("${cors.accepted:*}")
//    private String allowedOrigins;
//
//    @Bean
//    public WebMvcConfigurer corsConfigure() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
//                registry.addMapping("/**").allowedMethods(allowedOrigins)
//                        .allowedOrigins(allowedOrigins).allowedHeaders(allowedOrigins);
//            }
//        };
//    }
//}

package fpt.capstone.edu360managementsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // CHỈ rõ origin FE (không dùng "*")
        cfg.setAllowedOriginPatterns(List.of("http://localhost:8386")); // CRA: npm start
        // Cho phép gửi cookie
        cfg.setAllowCredentials(true);
        // Method & Header cho preflight
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        // Expose Set-Cookie và Authorization cho FE
        cfg.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}

