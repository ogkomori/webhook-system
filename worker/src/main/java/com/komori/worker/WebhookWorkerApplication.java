package com.komori.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WebhookWorkerApplication {

    static void main(String[] args) {
        SpringApplication.run(WebhookWorkerApplication.class, args);
    }

}
