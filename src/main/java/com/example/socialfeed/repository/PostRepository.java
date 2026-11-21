package com.example.socialfeed.repository;

import com.example.socialfeed.entity.Post;
import com.example.socialfeed.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = "user")
    List<Post> findByUserOrderByCreatedAtDesc(User user);
    @EntityGraph(attributePaths = "user")
    List<Post> findAllByOrderByCreatedAtDesc();
}