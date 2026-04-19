package com.mse.edu.forum.mapper;

import com.mse.edu.forum.api.generated.model.CreatePostRequest;
import com.mse.edu.forum.api.generated.model.PostResponse;
import com.mse.edu.forum.domain.PostEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PostMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "title", source = "title", qualifiedByName = "trimmed")
	@Mapping(target = "content", source = "content", qualifiedByName = "trimmed")
	PostEntity toEntity(CreatePostRequest request);

	@Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToOffset")
	PostResponse toResponse(PostEntity entity);

	@Named("trimmed")
	default String trimmed(String value) {
		return value == null ? null : value.trim();
	}

	@Named("instantToOffset")
	default OffsetDateTime instantToOffset(Instant instant) {
		return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
	}
}
