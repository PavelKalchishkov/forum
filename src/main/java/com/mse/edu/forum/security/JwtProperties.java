package com.mse.edu.forum.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("app.jwt.secret must be set (env JWT_SECRET)");
		}
		if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException(
					"JWT_SECRET must be at least 32 bytes for HS256. "
							+ "Update .env — see .env.dev.example.");
		}
	}
}
