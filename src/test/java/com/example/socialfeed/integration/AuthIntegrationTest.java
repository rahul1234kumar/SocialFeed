package com.example.socialfeed.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerReturnsToken() throws Exception {
        register("alice", "alice@example.com", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        register("alice", "alice@example.com", "secret123").andExpect(status().isOk());

        register("alice", "other@example.com", "secret123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        register("alice", "alice@example.com", "secret123").andExpect(status().isOk());

        register("bob", "alice@example.com", "secret123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    void invalidRegisterBodyReturns400() throws Exception {
        // username too short, email invalid, password too short
        register("al", "not-an-email", "123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    /**
     * ZNSM-7: ten identical registers at the same instant.
     * Exactly one may win; the other nine must get 409, never 500.
     */
    @Test
    void concurrentRegisterCreatesExactlyOneUser() throws Exception {
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        // Every thread waits at the gate, then all are released together,
        // so the requests really overlap instead of running one after another.
        CountDownLatch gate = new CountDownLatch(1);

        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                gate.await();
                return register("race", "race@example.com", "secret123")
                        .andReturn().getResponse().getStatus();
            }));
        }
        gate.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) {
            statuses.add(f.get());
        }
        pool.shutdown();

        assertThat(statuses).containsOnly(200, 409);
        assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = 'race'", Integer.class);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void loginWithCorrectPasswordReturnsToken() throws Exception {
        register("alice", "alice@example.com", "secret123").andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        register("alice", "alice@example.com", "secret123").andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
