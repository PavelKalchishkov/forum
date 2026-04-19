package com.mse.edu.forum.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
			throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.formLogin(AbstractHttpConfigurer::disable);
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/auth/login").permitAll()
				.requestMatchers(HttpMethod.GET, "/posts", "/posts/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/replies", "/replies/**").permitAll()
				.requestMatchers("/v3/api-docs/**", "/scalar/**", "/docs").permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest()
				.authenticated());
		http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
