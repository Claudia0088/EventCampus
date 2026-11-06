
package com.eventcampus.backend.dto;

import java.time.LocalDateTime;

public class AdminUserResponse {

    private final Integer idUser;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String status;
    private final String role;
    private final LocalDateTime createdAt;

    public AdminUserResponse(
            Integer idUser,
            String firstName,
            String lastName,
            String email,
            String status,
            String role,
            LocalDateTime createdAt) {

        this.idUser = idUser;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
