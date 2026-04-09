package com.global.order_api.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.javafaker.Faker;

@Configuration
public class FakerConfig {
	@Bean
	public Faker faker()
	{
		return new Faker();
	}
}
