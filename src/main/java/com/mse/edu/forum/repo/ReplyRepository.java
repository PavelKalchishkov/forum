package com.mse.edu.forum.repo;

import com.mse.edu.forum.domain.ReplyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplyRepository extends JpaRepository<ReplyEntity, Long> {

	Page<ReplyEntity> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);
}
