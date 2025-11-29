package com.komori.api.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.komori.persistence.repository")
@EntityScan(basePackages = "com.komori.persistence.entity")
public class PersistenceConfig {
}
