package com.mse.edu.forum.security;

import com.mse.edu.forum.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ForumUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public ForumUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepository
				.findByUsername(username)
				.map(ForumUserDetails::fromEntity)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
	}
}
