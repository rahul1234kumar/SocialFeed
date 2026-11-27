package com.example.socialfeed.repository;

import com.example.socialfeed.entity.Post;
import com.example.socialfeed.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = "user")
    List<Post> findByUserOrderByCreatedAtDesc(User user);
    @EntityGraph(attributePaths = "user")
    List<Post> findAllByOrderByCreatedAtDesc();

    // First page: no cursor yet, start from the newest post.
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT p FROM Post p
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<Post> findFirstPage(Pageable pageable);

    // Next pages: posts strictly after (createdAt, id) in the feed order.
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT p FROM Post p
            WHERE p.createdAt < :createdAt
               OR (p.createdAt = :createdAt AND p.id < :id)
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<Post> findPageAfter(@Param("createdAt") LocalDateTime createdAt,
                             @Param("id") Long id,
                             Pageable pageable);
}