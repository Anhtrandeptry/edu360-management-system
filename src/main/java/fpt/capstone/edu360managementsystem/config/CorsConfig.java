package fpt.capstone.edu360managementsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@ConfigurationProperties
public class CorsConfig {
    @Value("${cors.accepted:*}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigure() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedMethods(allowedOrigins)
                        .allowedOrigins(allowedOrigins).allowedHeaders(allowedOrigins);
            }
        };
    }
}
