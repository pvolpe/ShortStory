# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Run the app: `mvn spring-boot:run` (serves http://localhost:8080)
- Run all tests: `mvn test`
- Run a single test: `mvn test -Dtest=ShortStoryControllerTest#crudRoundTrip`
- Build a jar: `mvn package` (produces `target/shortstory-0.0.1-SNAPSHOT.jar`)

There is no linter, and the front-end has no build step.

## Toolchain constraints

The machine has only **Java 8** and **Maven 3.5.4**, and the user chose to stay on them. That is why the project uses **Spring Boot 2.7.x**. Do not upgrade to Spring Boot 3 (it needs Java 17+) unless the user asks. Consequences:
- Use `javax.persistence` / `javax.validation` imports, not `jakarta.*`.
- Only Java 8 language features: no `var`, records, text blocks or `List.of`.
- Hibernate 5.6 has no built-in SQLite dialect, so the project uses `com.github.gwenn:sqlite-dialect` (`org.sqlite.hibernate.dialect.SQLiteDialect`).

## Architecture

The app is a single Spring Boot module (`com.example.shortstory`) with three layers: the `ShortStory` JPA entity, a Spring Data `JpaRepository`, and a REST controller at `/api/stories`. There is no service layer. The controller calls the repository directly, and `@Valid` on the entity's bean-validation annotations handles validation (invalid input returns 400, and a missing id raises `ResponseStatusException` 404).

**Database (SQLite):**
- The file lives at `data/shortstory.db`, relative to the working directory, and is git-ignored.
- `ShortStoryApplication.main` creates the `data/` directory, because SQLite won't create parent directories.
- The schema comes from `spring.jpa.hibernate.ddl-auto=update`. There are no migration scripts.
- The Hikari pool size is 1 on purpose, because SQLite allows only one writer and more connections cause "database is locked" errors.

**Front-end:** a vanilla JS single-page app in `src/main/resources/static/`, served by Spring Boot as static resources.
- `index.html` holds `<template>` elements for each view: list, view and form.
- `app.js` has a hash router (`#/`, `#/new`, `#/story/{id}`, `#/edit/{id}`) that clones those templates and calls the REST API with `fetch`.
- User content is inserted with `textContent` (not `innerHTML`) to avoid XSS. Keep it that way.
- Form fields are accessed with `form.elements.<name>`, because `form.title` would clash with the built-in `HTMLElement.title`.
- Colors are CSS custom properties on `:root`, with a `prefers-color-scheme: dark` override.

**Authentication (Spring Security 5.7, session-based):** `security/SecurityConfig.java` holds the whole security setup.
- Everything requires login except `/login`, `/signup`, `/styles.css` and `/error`.
- Logged-out requests are handled by a `DelegatingAuthenticationEntryPoint`: `/api/**` gets a 401, and any other path is redirected to `/login`. `app.js` turns a 401 into a redirect to the login page.
- `/login` and `/signup` are Thymeleaf templates (`src/main/resources/templates`) rendered by `AuthController`, so they get the CSRF hidden field automatically. The stories page stays static.
- CSRF uses `CookieCsrfTokenRepository.withHttpOnlyFalse()`. The SPA reads the `XSRF-TOKEN` cookie and sends it as the `X-XSRF-TOKEN` header, which every mutating `fetch` needs, including `POST /logout`.
- Users are stored as `AppUser` (table `app_user`) with BCrypt hashes. Emails are trimmed and lower-cased on both sign-up and login (`SecurityConfig.normalizeEmail`). The login form field is `email`, not `username`.
- Stories are shared across all users. Each story records its creator in `author`, which is set server-side on create to that user's display name (collected at sign-up; accounts without one fall back to their email, see `AppUser.authorName`). It isn't changed by `PUT`, and the front-end shows the author as a read-only field.

**SQLite + Hibernate quirks:**
- `@Column(unique = true)` fails, because Hibernate emits `ALTER TABLE ... ADD CONSTRAINT`, which SQLite doesn't support. Put `UNIQUE` in `columnDefinition` instead (see `AppUser.email`).
- The dialect doesn't translate constraint violations into `DataIntegrityViolationException`. They surface as a generic `JpaSystemException`, so catch `DataAccessException`.
- `ddl-auto=update` never adds constraints to tables that already exist.

**Tests:** `ShortStoryControllerTest` (annotated `@WithMockUser`, and its mutating requests use `.with(csrf())`) and `AuthTest` is a `@SpringBootTest` with MockMvc. It overrides the datasource to `target/test.db` with `ddl-auto=create-drop`, so tests never touch the real database.
