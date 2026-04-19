package com.mse.edu.forum.security;

import com.mse.edu.forum.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
			String token = header.substring(7).trim();
			if (!token.isEmpty()) {
				try {
					Claims claims = jwtService.parseAndValidate(token);
					String username = claims.getSubject();
					Long uid = claims.get("uid", Long.class);
					String roleName = claims.get("role", String.class);
					if (username != null && uid != null && roleName != null) {
						UserRole role = UserRole.valueOf(roleName);
						ForumUserDetails principal = ForumUserDetails.fromJwt(uid, username, role);
						var auth = new UsernamePasswordAuthenticationToken(
								principal, null, principal.getAuthorities());
						SecurityContextHolder.getContext().setAuthentication(auth);
					}
				} catch (JwtException ignored) {
					SecurityContextHolder.clearContext();
				}
			}
		}
		filterChain.doFilter(request, response);
	}
}
