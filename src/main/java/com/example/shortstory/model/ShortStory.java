package com.example.shortstory.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Entity
@Table(name = "short_story")
public class ShortStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    // Set by the server from the logged-in user on create and never changed by clients,
    // so it isn't @NotBlank: a request body doesn't have to carry it.
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String author;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    // How many times a reader has scrolled a story all the way to the end.
    // DEFAULT 0 in columnDefinition matters here: SQLite's ALTER TABLE ADD COLUMN
    // (what ddl-auto=update emits for this new field on an existing table) rejects
    // a NOT NULL column with no default, and rows already in the table need a value.
    @Column(name = "read_count", nullable = false, columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    private long readCount = 0;

    // How many different users have read the story (each user counts once). Same DEFAULT 0 reason as readCount.
    @Column(name = "reader_count", nullable = false, columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    private long readerCount = 0;

    public ShortStory() {
    }

    public ShortStory(String title, String author, String body) {
        this.title = title;
        this.author = author;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getReadCount() {
        return readCount;
    }

    public void setReadCount(long readCount) {
        this.readCount = readCount;
    }

    public long getReaderCount() {
        return readerCount;
    }

    public void setReaderCount(long readerCount) {
        this.readerCount = readerCount;
    }
}
