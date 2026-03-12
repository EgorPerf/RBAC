package org.example.rbac.model;

import org.example.rbac.util.ValidationUtils;

public record User(String username, String fullName, String email) {

    public static User create(String username, String fullName, String email) {
        ValidationUtils.requireNonEmpty(username, "Username");
        ValidationUtils.requireNonEmpty(fullName, "FullName");
        ValidationUtils.requireNonEmpty(email, "Email");

        String normUsername = ValidationUtils.normalizeString(username);
        String normFullName = ValidationUtils.normalizeString(fullName);
        String normEmail = ValidationUtils.normalizeString(email);

        if (!ValidationUtils.isValidUsername(normUsername)) {
            throw new IllegalArgumentException("Username должен быть от 3 до 20 символов и содержать только латиницу, цифры или подчёркивание.");
        }

        if (!ValidationUtils.isValidEmail(normEmail)) {
            throw new IllegalArgumentException("Email имеет неверный формат.");
        }

        return new User(normUsername, normFullName, normEmail);
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}