package com.javatechie.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${stripe.secretKey}")
    private String stringSecretKey;

    //At the start of the project, this will initialize the secret key
    @PostConstruct
    public void initSecretKey() {
        Stripe.apiKey = stringSecretKey;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8080") // Adjust for your frontend URL
                .allowedMethods("POST");
    }
}
