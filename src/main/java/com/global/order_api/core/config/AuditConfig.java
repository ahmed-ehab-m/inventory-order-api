package com.global.order_api.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
// turn on auditing
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
// AuditorAware => class responsible to define who is do action now in db
// to save it in createdBy , updatedBy
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // for test only
        // soon security and JWT
        return () -> Optional.of("SYSTEM");
        // lambda expression
        // () => function don't take any parameters
        // -> means that write the result direct
        // optinal.of means that i sure there a value
        // because this function return object not string to prevent nullpointerexception
    }
}
