package org.example.rbac.model;

public record User(String username, String fullName, String email) {

    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$";
    private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    public static User validate(String username, String fullName, String email) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("FullName не может быть пустым.");
        }

        if (username == null || !username.matches(USERNAME_REGEX)) {
            throw new IllegalArgumentException("Username должен быть от 3 до 20 символов и содержать только латиницу, цифры или подчёркивание.");
        }

        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Email имеет неверный формат.");
        }

        return new User(username, fullName.trim(), email.trim());
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}