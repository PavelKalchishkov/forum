package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyPageResponse;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.api.generated.model.UpdateReplyRequest;
import com.mse.edu.forum.domain.ReplyEntity;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.mapper.ReplyMapper;
import com.mse.edu.forum.repo.PostRepository;
import com.mse.edu.forum.repo.ReplyRepository;
import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.security.ContentSecurity;
import com.mse.edu.forum.security.ForumUserDetails;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReplyService {

	private static final int DEFAULT_PAGE_SIZE = 10;

	private final ReplyRepository replyRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final ReplyMapper replyMapper;
	private final ContentSecurity contentSecurity;

	public ReplyService(
			ReplyRepository replyRepository,
			PostRepository postRepository,
			UserRepository userRepository,
			ReplyMapper replyMapper,
			ContentSecurity contentSecurity) {
		this.replyRepository = replyRepository;
		this.postRepository = postRepository;
		this.userRepository = userRepository;
		this.replyMapper = replyMapper;
		this.contentSecurity = contentSecurity;
	}

	@Transactional(readOnly = true)
	public ReplyPageResponse findByPostId(Long postId, Integer page, Integer size) {
		if (!postRepository.existsById(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
		}
		int pageNumber = page == null ? 0 : Math.max(0, page);
		int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(100, Math.max(1, size));
		Page<ReplyEntity> result =
				replyRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(pageNumber, pageSize));
		return new ReplyPageResponse(
				result.getContent().stream().map(replyMapper::toResponse).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	@Transactional(readOnly = true)
	public Optional<ReplyResponse> findById(Long id) {
		return replyRepository.findById(id).map(replyMapper::toResponse);
	}

	@Transactional
	public ReplyResponse create(Long postId, CreateReplyRequest request) {
		if (!postRepository.existsById(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
		}
		ForumUserDetails current = ContentSecurity.requireCurrentUser();
		UserEntity author = userRepository
				.findById(current.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		ReplyEntity entity = replyMapper.toEntity(request, postId);
		entity.setAuthor(author);
		ReplyEntity saved = replyRepository.save(entity);
		return replyMapper.toResponse(saved);
	}

	@Transactional
	public Optional<ReplyResponse> update(Long id, UpdateReplyRequest request) {
		Optional<ReplyEntity> existing = replyRepository.findById(id);
		if (existing.isEmpty()) {
			return Optional.empty();
		}
		ReplyEntity entity = existing.get();
		Long authorId = entity.getAuthor() != null ? entity.getAuthor().getId() : null;
		if (!contentSecurity.canEdit(authorId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to edit this reply");
		}
		replyMapper.applyUpdate(request, entity);
		ReplyEntity saved = replyRepository.save(entity);
		return Optional.of(replyMapper.toResponse(saved));
	}

	@Transactional
	public boolean delete(Long id) {
		Optional<ReplyEntity> existing = replyRepository.findById(id);
		if (existing.isEmpty()) {
			return false;
		}
		ReplyEntity entity = existing.get();
		Long authorId = entity.getAuthor() != null ? entity.getAuthor().getId() : null;
		if (!contentSecurity.canEdit(authorId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this reply");
		}
		replyRepository.delete(entity);
		return true;
	}
}
