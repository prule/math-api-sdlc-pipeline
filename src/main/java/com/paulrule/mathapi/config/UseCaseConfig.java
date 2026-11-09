package com.paulrule.mathapi.config;

import com.paulrule.mathapi.application.PerformCalculation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    PerformCalculation performCalculation() {
        return new PerformCalculation();
    }
}
