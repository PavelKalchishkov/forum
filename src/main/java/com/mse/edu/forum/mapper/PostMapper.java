package com.mse.edu.forum.mapper;

import com.mse.edu.forum.api.generated.model.AuthorSummary;
import com.mse.edu.forum.api.generated.model.CreatePostRequest;
import com.mse.edu.forum.api.generated.model.PostResponse;
import com.mse.edu.forum.api.generated.model.UpdatePostRequest;
import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.domain.UserEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PostMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "viewCount", ignore = true)
	@Mapping(target = "author", ignore = true)
	@Mapping(target = "title", source = "title", qualifiedByName = "trimmed")
	@Mapping(target = "content", source = "content", qualifiedByName = "trimmed")
	PostEntity toEntity(CreatePostRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "viewCount", ignore = true)
	@Mapping(target = "author", ignore = true)
	@Mapping(target = "title", source = "title", qualifiedByName = "trimmed")
	@Mapping(target = "content", source = "content", qualifiedByName = "trimmed")
	void applyUpdate(UpdatePostRequest request, @MappingTarget PostEntity entity);

	@Mapping(target = "author", source = "author", qualifiedByName = "toAuthorSummary")
	@Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToOffset")
	@Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "instantToOffset")
	PostResponse toResponse(PostEntity entity);

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
