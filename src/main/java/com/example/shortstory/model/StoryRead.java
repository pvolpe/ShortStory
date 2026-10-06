package com.example.shortstory.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

// One row per (story, user) pair: records that a user has read a story, so each user counts once
@Entity
@Table(name = "story_read")
public class StoryRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain ids rather than relations: reads are only ever looked up by story and user
    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public StoryRead() {
    }

    public StoryRead(Long storyId, Long userId) {
        this.storyId = storyId;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public Long getStoryId() {
        return storyId;
    }

    public Long getUserId() {
        return userId;
    }
}
