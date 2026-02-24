package org.example.rbac.model;

import java.util.regex.Pattern;

public record User(String username, String fullName, String email) {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static User create(String username, String fullName, String email) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("FullName не может быть пустым.");
        }

        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Username должен быть от 3 до 20 символов и содержать только латиницу, цифры или подчёркивание.");
        }

        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email имеет неверный формат.");
        }

        return new User(username, fullName.trim(), email.trim());
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}