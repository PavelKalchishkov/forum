package com.mse.edu.forum.repo;

import com.mse.edu.forum.domain.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<PostEntity, Long> {}
