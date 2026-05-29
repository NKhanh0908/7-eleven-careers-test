package com.testround.seven_eleven;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SevenElevenApplication {

	public static void main(String[] args) {
		SpringApplication.run(SevenElevenApplication.class, args);
	}

}

