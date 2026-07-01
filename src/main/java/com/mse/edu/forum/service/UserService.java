package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreateUserRequest;
import com.mse.edu.forum.api.generated.model.RegisterRequest;
import com.mse.edu.forum.api.generated.model.UpdateUserRequest;
import com.mse.edu.forum.api.generated.model.UserResponse;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.domain.UserRole;
import com.mse.edu.forum.mapper.UserMapper;
import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.security.ForumUserDetails;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<UserResponse> findAll() {
		return userRepository.findAll().stream().map(userMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public Optional<UserResponse> findById(Long id) {
		return userRepository.findById(id).map(userMapper::toResponse);
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		UserEntity entity = new UserEntity();
		entity.setUsername(userMapper.trimmed(request.getUsername()));
		entity.setEmail(userMapper.normalizeEmail(request.getEmail()));
		entity.setRole(UserRole.USER);
		validateUniqueCredentials(entity);
		entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		return userMapper.toResponse(userRepository.save(entity));
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		UserEntity entity = userMapper.toEntity(request);
		validateUniqueCredentials(entity);
		entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		UserEntity saved = userRepository.save(entity);
		return userMapper.toResponse(saved);
	}

	@Transactional
	public Optional<UserResponse> update(Long id, UpdateUserRequest request) {
		Optional<UserEntity> existing = userRepository.findById(id);
		if (existing.isEmpty()) {
			return Optional.empty();
		}
		UserEntity entity = existing.get();
		if (!isAdmin()) {
			if (!userMapper.toApiRole(entity.getRole()).equals(request.getRole())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can change roles");
			}
		}
		String newUsername = userMapper.trimmed(request.getUsername());
		if (userRepository.existsByUsernameAndIdNot(newUsername, id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
		}
		String newEmail = userMapper.normalizeEmail(request.getEmail());
		if (newEmail != null && userRepository.existsByEmailAndIdNot(newEmail, id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
		}
		String pwd = request.getPassword();
		if (pwd != null && !pwd.isBlank()) {
			entity.setPasswordHash(passwordEncoder.encode(pwd));
		}
		userMapper.applyUpdate(request, entity);
		UserEntity saved = userRepository.save(entity);
		return Optional.of(userMapper.toResponse(saved));
	}

	@Transactional
	public boolean delete(Long id) {
		if (!userRepository.existsById(id)) {
			return false;
		}
		userRepository.deleteById(id);
		return true;
	}

	private void validateUniqueCredentials(UserEntity entity) {
		if (userRepository.existsByUsername(entity.getUsername())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
		}
		if (entity.getEmail() != null && userRepository.existsByEmail(entity.getEmail())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
		}
	}

	private static boolean isAdmin() {
		Authentication a = SecurityContextHolder.getContext().getAuthentication();
		if (a == null || !(a.getPrincipal() instanceof ForumUserDetails u)) {
			return false;
		}
		return u.getAuthorities().stream().anyMatch(x -> "ROLE_ADMIN".equals(x.getAuthority()));
	}
}
