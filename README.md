# ShortStory

A small app for writing and reading short stories.

- **Back-end:** Spring Boot 2.7 (Java 8), Spring Data JPA, SQLite
- **Front-end:** plain HTML/CSS/JavaScript in `src/main/resources/static`, served by Spring Boot (no build step)

## Run

```
mvn spring-boot:run
```

Then open http://localhost:8080. The database file is created at `data/shortstory.db`.

You need an account: sign up at `/signup` with an email and a password of at least 8 characters, then log in. Signed-out visitors are redirected to `/login`, and the API returns 401 until you log in. API calls that change data must send the `XSRF-TOKEN` cookie value in an `X-XSRF-TOKEN` header.

## Test

```
mvn test
```

## REST API

| Method | Path                    | Description                              |
|--------|-------------------------|------------------------------------------|
| GET    | `/api/stories?q=term`   | List stories, newest first; `q` searches title or author (optional) |
| GET    | `/api/stories/{id}`     | Get one story                            |
| POST   | `/api/stories`          | Create `{ "title", "author", "body" }`   |
| PUT    | `/api/stories/{id}`     | Update a story                           |
| DELETE | `/api/stories/{id}`     | Delete a story                           |

All three fields are required. The title can be up to 200 characters and the author up to 100.
