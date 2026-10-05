package com.example.shortstory.controller;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class SignupForm {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Display name is required.")
    @Size(max = 100, message = "Display name must be 100 characters or fewer.")
    private String displayName;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters.")
    private String password;

    private String confirmPassword;

    public String getEmail() {
        return email;
    }

    // Trim before validation so " a@b.com " is accepted
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getDisplayName() {
        return displayName;
    }

    // Trimmed like the email, so "  Ada  " is stored as "Ada"
    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? null : displayName.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
