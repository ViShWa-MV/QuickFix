package com.example.QuickFix.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Error responses carry status, error, message and path only. The stack trace
 * is never sent to clients, whatever the error was.
 */
@Configuration
public class ErrorResponseConfig {

    @Bean
    public DefaultErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(
                    WebRequest webRequest,
                    ErrorAttributeOptions options) {
                return super.getErrorAttributes(
                        webRequest,
                        options.excluding(ErrorAttributeOptions.Include.STACK_TRACE));
            }
        };
    }
}
