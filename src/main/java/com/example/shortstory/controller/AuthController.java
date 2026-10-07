package com.example.shortstory.controller;

import com.example.shortstory.model.AppUser;
import com.example.shortstory.repository.AppUserRepository;
import com.example.shortstory.repository.StoryReadRepository;
import com.example.shortstory.security.SecurityConfig;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class AuthController {

    private final AppUserRepository users;
    private final StoryReadRepository reads;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserRepository users, StoryReadRepository reads, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.reads = reads;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        return isLoggedIn(auth) ? "redirect:/" : "login";
    }

    @GetMapping("/signup")
    public String signupPage(Authentication auth, @ModelAttribute("form") SignupForm form) {
        return isLoggedIn(auth) ? "redirect:/" : "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") SignupForm form, BindingResult result) {
        String email = SecurityConfig.normalizeEmail(form.getEmail());

        if (!result.hasFieldErrors("password") && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }
        if (!result.hasFieldErrors("email") && users.existsByEmail(email)) {
            result.rejectValue("email", "taken", "An account with this email already exists.");
        }
        if (result.hasErrors()) {
            return "signup";
        }

        try {
            users.save(new AppUser(email, form.getDisplayName(), passwordEncoder.encode(form.getPassword())));
        } catch (DataAccessException e) {
            // Lost a race with a concurrent sign-up for the same email (UNIQUE constraint)
            if (!users.existsByEmail(email)) {
                throw e;
            }
            result.rejectValue("email", "taken", "An account with this email already exists.");
            return "signup";
        }
        return "redirect:/login?registered";
    }

    // email: shown in the top bar. name: the author name new stories will get.
    // storiesRead: how many different stories this user has read.
    @GetMapping("/api/me")
    @ResponseBody
    public Map<String, Object> me(Authentication auth) {
        AppUser user = users.findByEmail(auth.getName()).orElse(null);
        Map<String, Object> me = new LinkedHashMap<>();
        me.put("email", auth.getName());
        me.put("name", user == null ? auth.getName() : user.authorName());
        me.put("storiesRead", user == null ? 0 : reads.countByUserId(user.getId()));
        return me;
    }

    private static boolean isLoggedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
