package com.mse.edu.forum.security;

import com.mse.edu.forum.domain.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("contentSecurity")
public class ContentSecurity {

	public boolean canEdit(Long authorId) {
		ForumUserDetails current = currentUser();
		if (current == null) {
			return false;
		}
		UserRole role = current.getDomainRole();
		if (role == UserRole.ADMIN || role == UserRole.MODERATOR) {
			return true;
		}
		return authorId != null && authorId == current.getId();
	}

	public static ForumUserDetails currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof ForumUserDetails details)) {
			return null;
		}
		return details;
	}

	public static ForumUserDetails requireCurrentUser() {
		ForumUserDetails user = currentUser();
		if (user == null) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return user;
	}
}
