package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.LoginRequest;
import com.mse.edu.forum.api.generated.model.LoginResponse;
import com.mse.edu.forum.security.ForumUserDetails;
import com.mse.edu.forum.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	public LoginResponse login(LoginRequest request) {
		var token =
				UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword());
		var auth = authenticationManager.authenticate(token);
		var user = (ForumUserDetails) auth.getPrincipal();
		String jwt = jwtService.createToken(user.getId(), user.getUsername(), user.getDomainRole());
		return new LoginResponse(jwt, "Bearer", jwtService.getExpiresInSeconds());
	}
}
