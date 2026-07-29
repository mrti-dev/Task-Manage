/*
 * Author: Hỏi Dân IT - @hoidanit 
 *
 * This source code is developed for the course
 * "Java Spring Siêu Tốc - Tự Học Java Spring Từ Số 0 Dành Cho Beginners từ A tới Z".
 * It is intended for educational purposes only.
 * Unauthorized distribution, reproduction, or modification is strictly prohibited.
 *
 * Copyright (c) 2025 Hỏi Dân IT. All Rights Reserved.
 */

package com.tiendev.task_management_api.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenAPIConfig {

	private SecurityScheme createBearerScheme() {
		// @formatter:off
		return new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.bearerFormat("JWT")
				.scheme("bearer");
		// @formatter:on
	}

	private Server createServer(String url, String description) {
		Server server = new Server();
		server.setUrl(url);
		server.setDescription(description);
		return server;
	}

	private Contact createContact() {
		// @formatter:off
		return new Contact()
				.email("vumanhtien2605@gmail.com")
				.name("VU MANH TIEN");
		// @formatter:on
	}

	private License createLicense() {
		return new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");
	}

	private Info createApiInfo() {
		// @formatter:off
		return new Info()
				.title("Task Management")
				.version("1.0")
				.contact(createContact())
				.description("This API exposes all endpoints (springsieutoc)")
				.license(createLicense());
		// @formatter:on
	}

	@Bean
	OpenAPI myOpenAPI() {
		// @formatter:off
		return new OpenAPI()
				.info(createApiInfo())
				.servers(List.of(
						createServer("http://localhost:8080",
								"Server URL in Development environment"),
						createServer("https://uat.example.com",
								"Server URL in Testing environment"),
						createServer("https://uat.example.com",
								"Server URL in Production environment")))
				.addSecurityItem(
						new SecurityRequirement().addList("Bearer Authentication"))
				.components(new Components()
						.addSecuritySchemes("Bearer Authentication", createBearerScheme()));
		// @formatter:on
	}
}