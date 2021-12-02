package com.farao_community.farao.gridcapa_core_valid.app;

import org.springframework.stereotype.Component;

@Component
public class RaoRunner {
    private final MinioAdapter minioAdapter;

    public RaoRunner(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

}
