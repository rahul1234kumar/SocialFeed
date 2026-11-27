package com.example.socialfeed.service;

import com.example.socialfeed.dto.request.CreatePostRequest;
import com.example.socialfeed.dto.response.CursorPageResponse;
import com.example.socialfeed.dto.response.PostResponse;
import com.example.socialfeed.entity.Post;
import com.example.socialfeed.entity.User;
import com.example.socialfeed.repository.PostRepository;
import com.example.socialfeed.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostResponse createPost(CreatePostRequest request, String username) {
        User user = findUser(username);
        Post post = postRepository.save(Post.builder()
                .content(request.getContent())
                .user(user)
                .build());
        return toResponse(post);
    }

    public CursorPageResponse<PostResponse> getPosts(String cursor, int limit) {
        // Ask for one extra row: if it comes back, there is a next page.
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Post> posts;
        if (cursor == null || cursor.isBlank()) {
            posts = postRepository.findFirstPage(pageable);
        } else {
            Cursor c = decodeCursor(cursor);
            posts = postRepository.findPageAfter(c.createdAt(), c.id(), pageable);
        }

        boolean hasNext = posts.size() > limit;
        List<Post> page = hasNext ? posts.subList(0, limit) : posts;
        // The cursor points at the last post the client has seen.
        String nextCursor = hasNext ? encodeCursor(page.getLast()) : null;

        return CursorPageResponse.<PostResponse>builder()
                .items(page.stream().map(this::toResponse).toList())
                .nextCursor(nextCursor)
                .build();
    }

    public List<PostResponse> getMyPosts(String username) {
        return postRepository.findByUserOrderByCreatedAtDesc(findUser(username))
                .stream().map(this::toResponse).toList();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // Position in the feed order (createdAt DESC, id DESC). id breaks ties
    // when two posts share the same createdAt.
    private record Cursor(LocalDateTime createdAt, Long id) {}

    // "2026-11-28T10:15:30.123456|42" -> URL-safe Base64. Opaque to clients,
    // so the format can change later without breaking the API.
    private String encodeCursor(Post post) {
        String raw = post.getCreatedAt() + "|" + post.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 2);
            return new Cursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException ex) {
            // GlobalExceptionHandler maps IllegalArgumentException to 400.
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    private PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .username(post.getUser().getUsername())
                .createdAt(post.getCreatedAt())
                .build();
    }
}