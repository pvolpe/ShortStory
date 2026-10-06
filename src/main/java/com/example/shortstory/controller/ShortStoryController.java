package com.example.shortstory.controller;

import com.example.shortstory.model.AppUser;
import com.example.shortstory.model.ShortStory;
import com.example.shortstory.model.StoryRead;
import com.example.shortstory.repository.AppUserRepository;
import com.example.shortstory.repository.ShortStoryRepository;
import com.example.shortstory.repository.StoryReadRepository;
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
    private final StoryReadRepository reads;

    public ShortStoryController(ShortStoryRepository repository, AppUserRepository users, StoryReadRepository reads) {
        this.repository = repository;
        this.users = users;
        this.reads = reads;
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
        AppUser user = currentUser(auth);
        story.setId(null);
        story.setAuthor(user.authorName());
        // Read counts are only ever changed by POST /read, never by the client
        story.setReadCount(0);
        story.setReaderCount(0);
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
    // only ever increments, it never sets an arbitrary count. Every call adds a
    // read; readerCount only goes up the first time a given user reads the story.
    @PostMapping("/{id}/read")
    public ShortStory markRead(@PathVariable Long id, Authentication auth) {
        ShortStory story = findOrThrow(id);
        Long userId = currentUser(auth).getId();
        if (!reads.existsByStoryIdAndUserId(id, userId)) {
            reads.save(new StoryRead(id, userId));
            story.setReaderCount(story.getReaderCount() + 1);
        }
        story.setReadCount(story.getReadCount() + 1);
        return repository.save(story);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ShortStory story = findOrThrow(id);
        reads.deleteByStoryId(id);
        repository.delete(story);
        return ResponseEntity.noContent().build();
    }

    private AppUser currentUser(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }

    private ShortStory findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story " + id + " not found"));
    }
}
