package org.example.rbac.filter;

import org.example.rbac.model.User;

import java.util.Comparator;

public class UserSorters {

    private UserSorters() {}

    public static Comparator<User> byUsername() {
        return Comparator.comparing(User::username);
    }

    public static Comparator<User> byFullName() {
        return Comparator.comparing(User::fullName);
    }

    public static Comparator<User> byEmail() {
        return Comparator.comparing(User::email);
    }
}