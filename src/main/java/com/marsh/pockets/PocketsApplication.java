package com.marsh.pockets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PocketsApplication {

	public static void main(String[] args) {
		SpringApplication.run(PocketsApplication.class, args);
	}

}
