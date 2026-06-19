package com.automatica.fakenews.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path workspacePath = Paths.get("smol_fetch_news_and_classify_app", "workspace");
        String absolutePath = workspacePath.toFile().getAbsolutePath();
        
        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }

        registry.addResourceHandler("/ai-images/**")
                .addResourceLocations("file:" + absolutePath);
    }
}
