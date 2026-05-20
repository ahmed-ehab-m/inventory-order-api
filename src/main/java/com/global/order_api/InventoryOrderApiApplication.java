package com.global.order_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.CachingConfigurationSelector;
import org.springframework.cache.annotation.EnableCaching;



@EnableCaching
@SpringBootApplication
public class InventoryOrderApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryOrderApiApplication.class, args);
	}

}
