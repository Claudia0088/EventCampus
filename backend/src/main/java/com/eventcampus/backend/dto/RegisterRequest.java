package com.eventcampus.backend.dto;

import jakarta.validation.constraints.*;

public class RegisterRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;
    
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @NotBlank
    @Size(max = 100)
    private String lastName;

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @NotBlank
    @Email
    @Size(max = 191)
    private String email;

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}