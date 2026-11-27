package com.example.socialfeed.controller;

import com.example.socialfeed.dto.request.CreatePostRequest;
import com.example.socialfeed.dto.response.CursorPageResponse;
import com.example.socialfeed.dto.response.PostResponse;
import com.example.socialfeed.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(request, userDetails.getUsername()));
    }

    private static final int MAX_LIMIT = 100;

    // GET /api/posts?limit=20                 -> first page
    // GET /api/posts?limit=20&cursor=<token>  -> the page after <token>
    @GetMapping
    public ResponseEntity<CursorPageResponse<PostResponse>> getAllPosts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);
        return ResponseEntity.ok(postService.getPosts(cursor, safeLimit));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PostResponse>> getMyPosts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(postService.getMyPosts(userDetails.getUsername()));
    }
}