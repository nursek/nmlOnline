package com.mg.nmlonline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Cache porté par le resource handler (appliqué seulement si la ressource est servie) : un 404 /boards n'est pas immuable. */
@Configuration
public class StaticResourceCacheConfig implements WebMvcConfigurer {

    private final String[] locations;

    public StaticResourceCacheConfig(
            @Value("${spring.web.resources.static-locations:classpath:/static/,file:/app/static/}") String[] locations) {
        this.locations = locations;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/boards/**")
                .addResourceLocations(subLocations("boards"))
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(subLocations("assets"))
                .setCacheControl(CacheControl.noCache());
    }

    private String[] subLocations(String sub) {
        return Arrays.stream(locations)
                .map(location -> location.endsWith("/") ? location + sub + "/" : location + "/" + sub + "/")
                .toArray(String[]::new);
    }
}
