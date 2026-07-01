package com.mse.edu.forum.api;

import com.mse.edu.forum.api.generated.RepliesApi;
import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyPageResponse;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.api.generated.model.UpdateReplyRequest;
import com.mse.edu.forum.service.ReplyService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RepliesApiController implements RepliesApi {

	private static final Logger log = LogManager.getLogger(RepliesApiController.class);

	private final ReplyService replyService;

	public RepliesApiController(ReplyService replyService) {
		this.replyService = replyService;
	}

	@Override
	public ResponseEntity<ReplyPageResponse> listRepliesForPost(Long postId, Integer page, Integer size) {
		log.debug("listRepliesForPost postId={} page={} size={}", postId, page, size);
		return ResponseEntity.ok(replyService.findByPostId(postId, page, size));
	}

	@Override
	public ResponseEntity<ReplyResponse> getReplyById(Long id) {
		log.debug("getReplyById id={}", id);
		return replyService
				.findById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ReplyResponse> createReply(Long postId, @Valid CreateReplyRequest createReplyRequest) {
		log.debug("createReply postId={}", postId);
		ReplyResponse created = replyService.create(postId, createReplyRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ReplyResponse> updateReply(Long id, @Valid UpdateReplyRequest updateReplyRequest) {
		log.debug("updateReply id={}", id);
		return replyService
				.update(id, updateReplyRequest)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> deleteReply(Long id) {
		log.debug("deleteReply id={}", id);
		if (replyService.delete(id)) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}
}
