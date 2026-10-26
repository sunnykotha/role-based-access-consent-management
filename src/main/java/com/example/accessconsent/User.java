package com.example.accessconsent;

import java.util.Objects;

public record User(Long id, String name, UserRole role) {
    public User {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name is required");
        }
        Objects.requireNonNull(role, "User role is required");
    }
}