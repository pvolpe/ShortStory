package com.example.shortstory;

import com.example.shortstory.model.AppUser;
import com.example.shortstory.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Test
    void unauthenticatedPageRedirectsToLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void unauthenticatedApiReturns401() throws Exception {
        mvc.perform(get("/api/stories")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginAndSignupPagesArePublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/signup")).andExpect(status().isOk());
    }

    @Test
    void signupThenLogin() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                        .param("email", "  Reader@Example.com ")
                        .param("displayName", "  Reader  ")
                        .param("password", "correct-horse")
                        .param("confirmPassword", "correct-horse"))
                .andExpect(redirectedUrl("/login?registered"));

        // Email is matched case-insensitively
        mvc.perform(post("/login").with(csrf())
                        .param("email", "reader@example.COM")
                        .param("password", "correct-horse"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("reader@example.com"));

        mvc.perform(post("/login").with(csrf())
                        .param("email", "reader@example.com")
                        .param("password", "wrong-password"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());

        // Same email again is rejected
        mvc.perform(post("/signup").with(csrf())
                        .param("email", "reader@example.com")
                        .param("password", "another-pass")
                        .param("confirmPassword", "another-pass"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("form", "email", "taken"));
    }

    @Test
    void databaseEnforcesUniqueEmail() {
        users.save(new AppUser("dup@example.com", "Dup", "x"));
        assertThrows(DataAccessException.class,
                () -> users.save(new AppUser("dup@example.com", "Dup", "y")));
    }

    @Test
    void signupValidatesInput() throws Exception {
        mvc.perform(post("/signup").with(csrf())
                        .param("email", "not-an-email")
                        .param("password", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "email", "password"));

        mvc.perform(post("/signup").with(csrf())
                        .param("email", "a@example.com")
                        .param("password", "long-enough")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("form", "confirmPassword", "mismatch"));
    }
}
