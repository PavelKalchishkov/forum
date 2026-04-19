package com.mse.edu.forum.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

	public boolean isSelf(Long userId) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return false;
		}
		if (!(auth.getPrincipal() instanceof ForumUserDetails details)) {
			return false;
		}
		return userId != null && userId == details.getId();
	}
}
