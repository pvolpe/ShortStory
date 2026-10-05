package com.example.shortstory.controller;

import com.example.shortstory.model.AppUser;
import com.example.shortstory.model.ShortStory;
import com.example.shortstory.repository.AppUserRepository;
import com.example.shortstory.repository.ShortStoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/stories")
public class ShortStoryController {

    private final ShortStoryRepository repository;
    private final AppUserRepository users;

    public ShortStoryController(ShortStoryRepository repository, AppUserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    @GetMapping
    public List<ShortStory> list(@RequestParam(required = false) String q) {
        if (q == null || q.trim().isEmpty()) {
            return repository.findAllByOrderByIdDesc();
        }
        String term = q.trim();
        return repository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrderByIdDesc(term, term);
    }

    @GetMapping("/{id}")
    public ShortStory get(@PathVariable Long id) {
        return findOrThrow(id);
    }

    // The author is always the logged-in user; any author sent by the client is ignored.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShortStory create(@Valid @RequestBody ShortStory story, Authentication auth) {
        AppUser user = users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
        story.setId(null);
        story.setAuthor(user.authorName());
        return repository.save(story);
    }

    // Title and body only: the author is fixed when the story is created.
    @PutMapping("/{id}")
    public ShortStory update(@PathVariable Long id, @Valid @RequestBody ShortStory input) {
        ShortStory story = findOrThrow(id);
        story.setTitle(input.getTitle());
        story.setBody(input.getBody());
        return repository.save(story);
    }

    // Called once by the front-end when a reader scrolls a story all the way to
    // the end (or the whole story already fit on screen). No request body: this
    // only ever increments, it never sets an arbitrary count.
    @PostMapping("/{id}/read")
    public ShortStory markRead(@PathVariable Long id) {
        ShortStory story = findOrThrow(id);
        story.setReadCount(story.getReadCount() + 1);
        return repository.save(story);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.delete(findOrThrow(id));
        return ResponseEntity.noContent().build();
    }

    private ShortStory findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story " + id + " not found"));
    }
}
