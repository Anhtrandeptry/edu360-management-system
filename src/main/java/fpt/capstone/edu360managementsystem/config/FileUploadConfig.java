package fpt.capstone.edu360managementsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for serving uploaded files.
 * Maps the /uploads/** URL path to the local uploads directory,
 * allowing static file access for uploaded resources.
 *
 * @author 360edu
 * @version 1.0
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    /**
     * Configures resource handlers for serving uploaded files.
     * Maps /uploads/** requests to the file:uploads/ directory.
     *
     * @param registry the resource handler registry
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
