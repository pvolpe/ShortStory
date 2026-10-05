package com.example.shortstory.repository;

import com.example.shortstory.model.ShortStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShortStoryRepository extends JpaRepository<ShortStory, Long> {

    List<ShortStory> findAllByOrderByIdDesc();

    List<ShortStory> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrderByIdDesc(String title, String author);
}
