package com.example.shortstory.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored trimmed and lower-cased so lookups are case-insensitive.
    // UNIQUE is inlined because SQLite can't "alter table add constraint", which unique = true generates.
    @Column(nullable = false, columnDefinition = "VARCHAR(254) UNIQUE")
    private String email;

    // BCrypt hash, never the raw password
    @Column(nullable = false, length = 100)
    private String passwordHash;

    // Shown as the author of this user's stories. Nullable because accounts created
    // before this field existed don't have one.
    @Column(name = "display_name", length = 100)
    private String displayName;

    public AppUser() {
    }

    public AppUser(String email, String displayName, String passwordHash) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Name to show as a story's author: the display name, or the email for accounts that don't have one
    public String authorName() {
        return displayName == null || displayName.isEmpty() ? email : displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
