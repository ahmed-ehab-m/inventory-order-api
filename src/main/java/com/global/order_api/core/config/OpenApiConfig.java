package com.global.order_api.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
		@Bean
		public OpenAPI customOpenApi()
		{
			// reference key for authorization
			// instead of write the token in every request during testing the APIS
			// bearer auth is default name
			final String securitySchemeName= "bearerAuth";
			// info => header of the page , simple info about the api
			return new OpenAPI().info(new Info().title("Order Management API")
					.version("1.0.0")
					.description("Enterprise API documentation for Order Management System.")
					.contact(new Contact().name("BackEnd Team").email("team@global.com")))
					// Create lock on my endpoints
					// here bearerauth (the security role) must be applied on any request
					.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
					// components => like a storage in swagger put into reusable settings
					// we will put security settings
					// add security schema (security system) and give it our key (bearerAuth)
					.components(new Components().addSecuritySchemes(securitySchemeName, 
					// add internal name to security schema to make swagger link this security schema
					// with security requirement
							new SecurityScheme().name(securitySchemeName)
							// the security type is http header authentication 
							// because there are many other types of auth
							.type(SecurityScheme.Type.HTTP)
							// when i send my token then swagger add "bearer" behind it
							// Authorization: Bearer 12345
							// to make server accept my token
							.scheme("bearer")
							// Documented Line 
							// JWT in swagger page as a Hint for front-end developer
							// to know him that the bearer token should be sent in JWT format
							.bearerFormat("JWT")));
		}
}
