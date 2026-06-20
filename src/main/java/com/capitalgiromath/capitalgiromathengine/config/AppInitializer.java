package com.capitalgiromath.capitalgiromathengine.config;

import com.capitalgiromath.capitalgiromathengine.service.CdiService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppInitializer implements CommandLineRunner {
    private final CdiService cdiService;

    public AppInitializer(CdiService cdiService) {
        this.cdiService = cdiService;
    }

    @Override
    public void run(String... args) throws Exception {
        cdiService.carregarSerieCdi();
    }
}