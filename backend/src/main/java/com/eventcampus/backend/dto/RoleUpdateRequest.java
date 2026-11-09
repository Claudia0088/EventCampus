
package com.eventcampus.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoleUpdateRequest {

    @Size(max = 50)
    private String roleName;

    @Size(max = 50)
    private String role;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEffectiveRole() {
        if (role != null && !role.isBlank()) {
            return role;
        }
        return roleName;
    }
}
