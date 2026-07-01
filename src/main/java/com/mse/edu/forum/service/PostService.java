package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreatePostRequest;
import com.mse.edu.forum.api.generated.model.PostResponse;
import com.mse.edu.forum.api.generated.model.UpdatePostRequest;
import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.mapper.PostMapper;
import com.mse.edu.forum.repo.PostRepository;
import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.security.ContentSecurity;
import com.mse.edu.forum.security.ForumUserDetails;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final PostMapper postMapper;
	private final ContentSecurity contentSecurity;

	public PostService(
			PostRepository postRepository,
			UserRepository userRepository,
			PostMapper postMapper,
			ContentSecurity contentSecurity) {
		this.postRepository = postRepository;
		this.userRepository = userRepository;
		this.postMapper = postMapper;
		this.contentSecurity = contentSecurity;
	}

	@Transactional(readOnly = true)
	public List<PostResponse> findAll() {
		return postRepository.findAll().stream().map(postMapper::toResponse).toList();
	}

	@Transactional
	public Optional<PostResponse> findById(Long id, Boolean trackView) {
		Optional<PostEntity> post = postRepository.findById(id);
		if (post.isEmpty()) {
			return Optional.empty();
		}
		PostEntity entity = post.get();
		if (trackView == null || trackView) {
			entity.setViewCount(entity.getViewCount() + 1);
			entity = postRepository.save(entity);
		}
		return Optional.of(postMapper.toResponse(entity));
	}

	@Transactional
	public PostResponse create(CreatePostRequest request) {
		String title = request.getTitle().trim();
		if (postRepository.existsByTitleIgnoreCase(title)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Title already in use");
		}
		ForumUserDetails current = ContentSecurity.requireCurrentUser();
		UserEntity author = userRepository
				.findById(current.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		PostEntity postEntity = postMapper.toEntity(request);
		postEntity.setAuthor(author);
		postEntity.setViewCount(0);
		PostEntity saved = postRepository.save(postEntity);
		return postMapper.toResponse(saved);
	}

	@Transactional
	public Optional<PostResponse> update(Long id, UpdatePostRequest request) {
		Optional<PostEntity> existing = postRepository.findById(id);
		if (existing.isEmpty()) {
			return Optional.empty();
		}
		PostEntity entity = existing.get();
		Long authorId = entity.getAuthor() != null ? entity.getAuthor().getId() : null;
		if (!contentSecurity.canEdit(authorId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to edit this topic");
		}
		String title = request.getTitle().trim();
		if (postRepository.existsByTitleIgnoreCaseAndIdNot(title, id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Title already in use");
		}
		postMapper.applyUpdate(request, entity);
		PostEntity saved = postRepository.save(entity);
		return Optional.of(postMapper.toResponse(saved));
	}

	@Transactional
	public boolean delete(Long id) {
		Optional<PostEntity> existing = postRepository.findById(id);
		if (existing.isEmpty()) {
			return false;
		}
		PostEntity entity = existing.get();
		Long authorId = entity.getAuthor() != null ? entity.getAuthor().getId() : null;
		if (!contentSecurity.canEdit(authorId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this topic");
		}
		postRepository.delete(entity);
		return true;
	}
}
