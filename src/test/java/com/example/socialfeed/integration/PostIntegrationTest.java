package com.example.socialfeed.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostIntegrationTest extends IntegrationTestBase {

    @Test
    void createPostReturns201() throws Exception {
        String token = registerAndGetToken("alice");

        createPost(token, "hello world")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.content").value("hello world"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void emptyContentReturns400() throws Exception {
        String token = registerAndGetToken("alice");

        createPost(token, "").andExpect(status().isBadRequest());
    }

    @Test
    void myPostsReturnsOnlyMine() throws Exception {
        String alice = registerAndGetToken("alice");
        String bob = registerAndGetToken("bob");
        createPost(alice, "from alice");
        createPost(bob, "from bob");

        mockMvc.perform(get("/api/posts/my").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("from alice"));
    }

    /**
     * ZNSM-10: walk the whole feed page by page with the cursor.
     * Every post must appear exactly once, newest first, and the last page has no cursor.
     */
    @Test
    void cursorPagingWalksAllPostsExactlyOnce() throws Exception {
        String token = registerAndGetToken("alice");
        for (int i = 1; i <= 5; i++) {
            createPost(token, "post " + i).andExpect(status().isCreated());
        }

        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            String query = "?limit=2" + (cursor == null ? "" : "&cursor=" + cursor);
            String body = getPosts(token, query)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> contents = JsonPath.read(body, "$.items[*].content");
            seen.addAll(contents);
            cursor = JsonPath.read(body, "$.nextCursor");
            pages++;
        } while (cursor != null);

        assertThat(pages).isEqualTo(3);   // 2 + 2 + 1
        assertThat(seen).containsExactly("post 5", "post 4", "post 3", "post 2", "post 1");
    }

    @Test
    void lastPageHasNullCursor() throws Exception {
        String token = registerAndGetToken("alice");
        createPost(token, "only post");

        getPosts(token, "?limit=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    @Test
    void invalidCursorReturns400() throws Exception {
        String token = registerAndGetToken("alice");

        getPosts(token, "?cursor=garbage")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid cursor"));
    }
}
