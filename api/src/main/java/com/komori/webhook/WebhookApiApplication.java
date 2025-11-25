package com.komori.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WebhookApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebhookApiApplication.class, args);
	}

}
