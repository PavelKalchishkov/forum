package com.mse.edu.forum.mapper;

import com.mse.edu.forum.api.generated.model.AuthorSummary;
import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.api.generated.model.UpdateReplyRequest;
import com.mse.edu.forum.domain.ReplyEntity;
import com.mse.edu.forum.domain.UserEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ReplyMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "author", ignore = true)
	@Mapping(target = "postId", source = "postId")
	@Mapping(target = "content", source = "request.content", qualifiedByName = "trimmed")
	ReplyEntity toEntity(CreateReplyRequest request, Long postId);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "author", ignore = true)
	@Mapping(target = "postId", ignore = true)
	@Mapping(target = "content", source = "content", qualifiedByName = "trimmed")
	void applyUpdate(UpdateReplyRequest request, @MappingTarget ReplyEntity entity);

	@Mapping(target = "author", source = "author", qualifiedByName = "toAuthorSummary")
	@Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToOffset")
	@Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "instantToOffset")
	ReplyResponse toResponse(ReplyEntity entity);

	@Named("toAuthorSummary")
	default AuthorSummary toAuthorSummary(UserEntity author) {
		if (author == null) {
			return null;
		}
		return new AuthorSummary(author.getId(), author.getUsername());
	}

	@Named("trimmed")
	default String trimmed(String value) {
		return value == null ? null : value.trim();
	}

	@Named("instantToOffset")
	default OffsetDateTime instantToOffset(Instant instant) {
		return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
	}
}
