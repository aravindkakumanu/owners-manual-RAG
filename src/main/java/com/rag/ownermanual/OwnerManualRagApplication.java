package com.rag.ownermanual;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class OwnerManualRagApplication {

	public static void main(String[] args) {
		SpringApplication.run(OwnerManualRagApplication.class, args);
	}

}
