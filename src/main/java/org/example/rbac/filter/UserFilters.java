package org.example.rbac.filter;

import org.example.rbac.model.User;
import org.example.rbac.util.ValidationUtils;

public class UserFilters {

    private UserFilters() {}

    public static UserFilter byUsername(String username) {
        ValidationUtils.requireNonEmpty(username, "Username");
        String norm = ValidationUtils.normalizeString(username);
        return user -> user.username().equals(norm);
    }

    public static UserFilter byUsernameContains(String substring) {
        ValidationUtils.requireNonEmpty(substring, "Substring");
        String norm = ValidationUtils.normalizeString(substring).toLowerCase();
        return user -> user.username().toLowerCase().contains(norm);
    }

    public static UserFilter byEmail(String email) {
        ValidationUtils.requireNonEmpty(email, "Email");
        String norm = ValidationUtils.normalizeString(email);
        return user -> user.email().equals(norm);
    }

    public static UserFilter byEmailDomain(String domain) {
        ValidationUtils.requireNonEmpty(domain, "Domain");
        String norm = ValidationUtils.normalizeString(domain);
        return user -> user.email().endsWith(norm);
    }

    public static UserFilter byFullNameContains(String substring) {
        ValidationUtils.requireNonEmpty(substring, "Substring");
        String norm = ValidationUtils.normalizeString(substring).toLowerCase();
        return user -> user.fullName().toLowerCase().contains(norm);
    }
}