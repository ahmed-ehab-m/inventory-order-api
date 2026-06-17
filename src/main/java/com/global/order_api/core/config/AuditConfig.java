    package com.global.order_api.core.config;

    import com.global.order_api.core.security.SecurityUtils;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.data.domain.AuditorAware;
    import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
    import org.springframework.security.core.Authentication;

    import java.util.Optional;

    @Configuration
    // turn on auditing
    @EnableJpaAuditing(auditorAwareRef = "auditorProvider")
    // AuditorAware => class responsible to define who is do action now in db
    // to save it in createdBy , updatedBy
    public class AuditConfig {

        @Bean
        public AuditorAware<String> auditorProvider() {
            /// admin || user || scheduled job (SYSTEM)
            return () -> {
                /// 1 => Use the SAFE method so it doesn't throw exceptions for background jobs
                Long userId = SecurityUtils.getCurrentUserIdSafe();

                /// 2 => if background process or no user logged in
                if (userId == null) {
                    return Optional.of("SYSTEM");
                }

                /// 3 => if user exists
                return Optional.of(userId.toString());
            };
        }
            // lambda expression
            // () => function don't take any parameters
            // -> means that write the result direct
            // optinal.of means that i sure there a value
            // because this function return object not string to prevent nullpointerexception
        }
