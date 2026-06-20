package com.capitalgiromath.capitalgiromathengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CapitalGiroMathEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(CapitalGiroMathEngineApplication.class, args);
    }
}