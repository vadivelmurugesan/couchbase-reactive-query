package com.github.vadivelmurugesan.couchbase.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for better serialization performance.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Module afterburnerModule() {
        return new AfterburnerModule();
    }
}
