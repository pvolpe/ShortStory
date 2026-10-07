package com.example.shortstory.repository;

import com.example.shortstory.model.StoryRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface StoryReadRepository extends JpaRepository<StoryRead, Long> {

    boolean existsByStoryIdAndUserId(Long storyId, Long userId);

    // One row per (story, user), so this is the count of distinct stories the user has read
    long countByUserId(Long userId);

    // Derived deletes need a transaction; without one Spring Data throws
    @Transactional
    void deleteByStoryId(Long storyId);
}
