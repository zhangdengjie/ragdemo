package com.bxmdm.ragdemo.config;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TikaDocumentConfig {

    @Bean
    public Tika tika() {
        TikaConfig config = TikaConfig.getDefaultConfig();
        return new Tika(config);
    }
}