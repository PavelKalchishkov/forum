package com.mse.edu.forum.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("app.jwt.secret must be set");
		}
	}
}
