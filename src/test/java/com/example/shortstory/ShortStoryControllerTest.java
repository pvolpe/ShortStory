package com.example.shortstory;

import com.example.shortstory.model.AppUser;
import com.example.shortstory.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@WithMockUser(username = "ada@example.com")
class ShortStoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @BeforeEach
    void ensureLoggedInUserExists() {
        if (!users.existsByEmail("ada@example.com")) {
            users.save(new AppUser("ada@example.com", "Ada", "x"));
        }
    }

    @Test
    void crudRoundTrip() throws Exception {
        MvcResult created = mvc.perform(post("/api/stories").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"The Lamp\",\"body\":\"Once upon a time.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.author").value("Ada"))
                .andReturn();

        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(created.getResponse().getContentAsString());
        m.find();
        String id = m.group(1);

        mvc.perform(get("/api/stories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("The Lamp"));

        mvc.perform(get("/api/stories").param("q", "ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].author").value("Ada"));

        // Author is not editable: a changed author in the request is ignored
        mvc.perform(put("/api/stories/" + id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"The Lantern\",\"author\":\"Mallory\",\"body\":\"Once upon a time.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("The Lantern"))
                .andExpect(jsonPath("$.author").value("Ada"));

        mvc.perform(delete("/api/stories/" + id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/stories/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void markReadIncrementsCounter() throws Exception {
        MvcResult created = mvc.perform(post("/api/stories").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"The Lamp\",\"body\":\"Once upon a time.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.readCount").value(0))
                .andReturn();

        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(created.getResponse().getContentAsString());
        m.find();
        String id = m.group(1);

        mvc.perform(post("/api/stories/" + id + "/read").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readCount").value(1));

        mvc.perform(post("/api/stories/" + id + "/read").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readCount").value(2));

        mvc.perform(get("/api/stories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readCount").value(2))
                .andExpect(jsonPath("$.readerCount").value(1));
    }

    @Test
    void readersCountEachUserOnce() throws Exception {
        if (!users.existsByEmail("grace@example.com")) {
            users.save(new AppUser("grace@example.com", "Grace", "x"));
        }
        MvcResult created = mvc.perform(post("/api/stories").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"The Clock\",\"body\":\"Tick.\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(created.getResponse().getContentAsString());
        m.find();
        String id = m.group(1);

        // Ada reads twice, Grace once: 3 reads by 2 readers
        mvc.perform(post("/api/stories/" + id + "/read").with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/stories/" + id + "/read").with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/stories/" + id + "/read").with(csrf()).with(user("grace@example.com")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/stories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readCount").value(3))
                .andExpect(jsonPath("$.readerCount").value(2));

        mvc.perform(delete("/api/stories/" + id).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void rejectsBlankFields() throws Exception {
        mvc.perform(post("/api/stories").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"body\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }
}
