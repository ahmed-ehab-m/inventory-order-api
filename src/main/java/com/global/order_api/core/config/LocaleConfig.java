package com.global.order_api.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Arrays;
import java.util.Locale;

@Configuration
public class LocaleConfig {
    @Bean
    public LocaleResolver localeResolver() {
        // class to see headers in api and find Accept-language
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();

        // set default english if front-end doesn't send header
        resolver.setDefaultLocale(Locale.ENGLISH);

        // set languages which our system support
        resolver.setSupportedLocales(Arrays.asList(
                new Locale("en"),
                new Locale("ar")
        ));

        return resolver;
    }
}
