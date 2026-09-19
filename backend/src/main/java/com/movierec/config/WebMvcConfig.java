package com.movierec.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${movie.poster-dir:./posters}")
    private String posterDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(posterDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), posterDir);
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String absolutePath = dir.getAbsolutePath().replace("\\", "/");
        registry.addResourceHandler("/api/posters/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
