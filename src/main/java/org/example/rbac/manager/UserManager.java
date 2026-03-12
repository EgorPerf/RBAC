package org.example.rbac.manager;

import org.example.rbac.filter.UserFilter;
import org.example.rbac.model.User;
import org.example.rbac.util.ValidationUtils;

import java.util.*;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {

    private final Map<String, User> users = new HashMap<>();

    @Override
    public void add(User item) {
        if (item == null) {
            throw new IllegalArgumentException();
        }
        if (users.containsKey(item.username())) {
            throw new IllegalArgumentException();
        }
        users.put(item.username(), item);
    }

    @Override
    public boolean remove(User item) {
        if (item == null || !users.containsKey(item.username())) {
            return false;
        }
        return users.remove(item.username(), item);
    }

    @Override
    public Optional<User> findById(String id) {
        return findByUsername(id);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(ValidationUtils.normalizeString(username)));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        String normEmail = ValidationUtils.normalizeString(email);
        return users.values().stream()
                .filter(u -> u.email().equals(normEmail))
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return users.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        if (filter == null || sorter == null) {
            throw new IllegalArgumentException();
        }
        return users.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return users.containsKey(ValidationUtils.normalizeString(username));
    }

    public void update(String username, String newFullName, String newEmail) {
        ValidationUtils.requireNonEmpty(username, "Username");
        String normUsername = ValidationUtils.normalizeString(username);

        if (!users.containsKey(normUsername)) {
            throw new IllegalArgumentException();
        }
        User updatedUser = User.create(normUsername, newFullName, newEmail);
        users.put(normUsername, updatedUser);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserManager that = (UserManager) o;
        return Objects.equals(users, that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }
}