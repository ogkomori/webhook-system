package com.komori.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.komori.persistence.repository")
@EntityScan(basePackages = "com.komori.persistence.entity")
public class WebhookWorkerApplication {

    static void main(String[] args) {
        SpringApplication.run(WebhookWorkerApplication.class, args);
    }
}
