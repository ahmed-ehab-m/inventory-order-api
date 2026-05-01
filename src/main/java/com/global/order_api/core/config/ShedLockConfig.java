package com.global.order_api.core.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {
	@Bean
	// lockProvider is the object responsible for locking db
	// JdbcTemplateLockProvider => to know shedlock that we use SQL 
	// JDBC template is a tool in spring make sql statements more easier
	// because shedlock library wants to read in db quickly
	// so programmers of shedlock instead of writing code of old jdbc
	// use the template directly
	public LockProvider lockProvider(DataSource dataSource)
	{
		return new JdbcTemplateLockProvider(
			JdbcTemplateLockProvider.Configuration.builder()
			.withJdbcTemplate(new JdbcTemplate(dataSource))
			.usingDbTime().build()); // db time => to prevent time servers conflict
	}
}
