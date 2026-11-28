package com.greenproject.classway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ClasswayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClasswayApplication.class, args);
	}

}
