package com.example.socialfeed.integration;

import com.example.socialfeed.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests: the whole app (all beans, the security filter chain,
 * Flyway, Hibernate) against a real MySQL in Docker.
 *
 * Every subclass uses the same annotations, so Spring builds the context and starts the
 * container once and reuses them for all test classes.
 */
@SpringBootTest                                // start the full application context
@AutoConfigureMockMvc                          // MockMvc: send HTTP requests without a real port
@Import(TestcontainersConfiguration.class)     // MySQL 8.4 container wired into the DataSource
abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Tests share one database, so each test starts from empty tables.
    // Posts first, because of the foreign key to users.
    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM users");
    }

    protected ResultActions register(String username, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"%s"}
                        """.formatted(username, email, password)));
    }

    // Registers a user and returns its JWT.
    protected String registerAndGetToken(String username) throws Exception {
        String body = register(username, username + "@example.com", "secret123")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    protected ResultActions createPost(String token, String content) throws Exception {
        return mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"content":"%s"}
                        """.formatted(content)));
    }

    protected ResultActions getPosts(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/posts" + query)
                .header("Authorization", "Bearer " + token));
    }
}
