package com.global.order_api.core.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Log4j2
public class DummyDataSeederConfig implements CommandLineRunner {@Override
	public void run(String... args) throws Exception {
		
		
	}
	
}
