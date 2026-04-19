package com.mse.edu.forum.repo;

import com.mse.edu.forum.domain.ReplyEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplyRepository extends JpaRepository<ReplyEntity, Long> {

	List<ReplyEntity> findByPostIdOrderByCreatedAtAsc(Long postId);
}
