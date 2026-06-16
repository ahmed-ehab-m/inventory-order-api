package com.global.order_api.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;


@Configuration
/// to make spring boot read this class when starting
@EnableWebSecurity
/// to edit default security filter chain
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;


    @Bean
    /// save this return object in context
    /// HTTP security => object to edit default security filter chain
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                /// Disable CSRF FILTER because i will not depend on JWT Filter
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/docs",
                                "/docs/**",
                                "/api-docs-json",
                                "/api-docs-json/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/v1/payments/webhook"
                        ).permitAll()
                        /// any one can login or sign up
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        /// any another endpoint must be authenticated
                        .anyRequest().authenticated()
                )
                /// means that => server will not store session id because will be Stateless
                /// depends on Jwt Token
                .sessionManagement((session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ))
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((oAuth2SuccessHandler)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManger(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /// FOT GITHUB
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
