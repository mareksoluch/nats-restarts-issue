package com.ocado.brokertester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EntityScan(basePackages = {"com.ocado.brokertester"})
@EnableAsync
public class BrokerTesterApplication {

	public static void main(String[] args) {
		SpringApplication.run(BrokerTesterApplication.class, args);
	}
}
