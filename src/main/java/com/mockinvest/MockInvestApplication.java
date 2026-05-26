package com.mockinvest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MockInvestApplication {

	public static void main(String[] args) {
		SpringApplication.run(MockInvestApplication.class, args);
	}

}
