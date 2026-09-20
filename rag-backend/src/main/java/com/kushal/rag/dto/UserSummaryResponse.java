package com.kushal.rag.dto;

/**
 * What we expose to admins about a user — deliberately
 * excludes the password hash. Never return the raw User
 * entity directly from a controller; always map to a DTO
 * like this so sensitive fields can't leak by accident.
 */

public class UserSummaryResponse {

    private String id;
    private String email;
    private String role;
    private boolean enabled;

    public UserSummaryResponse(String id, String email, String role, boolean enabled) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }
}