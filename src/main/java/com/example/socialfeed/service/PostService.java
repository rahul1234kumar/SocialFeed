package com.example.socialfeed.service;

import com.example.socialfeed.dto.request.CreatePostRequest;
import com.example.socialfeed.dto.response.PostResponse;
import com.example.socialfeed.entity.Post;
import com.example.socialfeed.entity.User;
import com.example.socialfeed.repository.PostRepository;
import com.example.socialfeed.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

    public List<PostResponse> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public List<PostResponse> getMyPosts(String username) {
        return postRepository.findByUserOrderByCreatedAtDesc(findUser(username))
                .stream().map(this::toResponse).toList();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
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