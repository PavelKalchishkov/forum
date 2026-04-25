package com.mse.edu.forum.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.repo.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PostsApiControllerTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("forum")
			.withUsername("admin")
			.withPassword("admin");

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostRepository postRepository;

	@BeforeEach
	void setUp() {
		postRepository.deleteAll();
	}

	@Test
	void createPostAndGetPosts() throws Exception {
		String token = loginAndGetToken("admin", "admin");

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "My first post",
								  "content": "Hello from MockMvc + Testcontainers"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("My first post"))
				.andExpect(jsonPath("$.content").value("Hello from MockMvc + Testcontainers"))
				.andExpect(jsonPath("$.createdAt").exists());

		mockMvc.perform(get("/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title", hasItem("My first post")))
				.andExpect(jsonPath("$[*].content", hasItem("Hello from MockMvc + Testcontainers")));
	}

	@Test
	void createPost_requiresAuthentication() throws Exception {
		mockMvc.perform(post("/posts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "No token",
								  "content": "Should fail"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void getPostById_returnsCreatedPost() throws Exception {
		String token = loginAndGetToken("admin", "admin");

		MvcResult createResult = mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Single post",
								  "content": "For get by id"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		long id = extractLongField(createResult.getResponse().getContentAsString(), "id");

		mockMvc.perform(get("/posts/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.title").value("Single post"))
				.andExpect(jsonPath("$.content").value("For get by id"));
	}

	@Test
	void getPostById_returns404WhenMissing() throws Exception {
		mockMvc.perform(get("/posts/{id}", 999999L))
				.andExpect(status().isNotFound());
	}

	@Test
	void listPosts_isPublic() throws Exception {
		String token = loginAndGetToken("admin", "admin");
		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Visible post",
								  "content": "Public read"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title", hasItem("Visible post")))
				.andExpect(jsonPath("$[*].title", not(hasItem("No token"))));
	}

	@Test
	void listPosts_returnsInsertionOrder() throws Exception {
		String token = loginAndGetToken("admin", "admin");

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Post A",
								  "content": "A"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Post B",
								  "content": "B"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Post A"))
				.andExpect(jsonPath("$[1].title").value("Post B"));
	}

	private String loginAndGetToken(String username, String password) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "%s"
								}
								""".formatted(username, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andReturn();

		String body = loginResult.getResponse().getContentAsString();
		String marker = "\"accessToken\":\"";
		int start = body.indexOf(marker);
		if (start < 0) {
			throw new IllegalStateException("accessToken not found in login response: " + body);
		}
		start += marker.length();
		int end = body.indexOf('"', start);
		if (end < 0) {
			throw new IllegalStateException("Invalid login response: " + body);
		}
		return body.substring(start, end);
	}

	private long extractLongField(String json, String fieldName) {
		Pattern pattern = Pattern.compile("\"" + fieldName + "\":(\\d+)");
		Matcher matcher = pattern.matcher(json);
		if (!matcher.find()) {
			throw new IllegalStateException("Field not found: " + fieldName + " in " + json);
		}
		return Long.parseLong(matcher.group(1));
	}
}
