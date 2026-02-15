package org.example.rbac.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Role {

    private static final Set<String> USED_NAMES = new HashSet<>();

    private final String id;
    private final String name;
    private final String description;
    private final Set<Permission> permissions;

    public Role(String name, String description, Set<Permission> permissions) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name не может быть пустым.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description не может быть пустым.");
        }
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions не может быть null.");
        }

        String trimmedName = name.trim();
        if (!USED_NAMES.add(trimmedName)) {
            throw new IllegalArgumentException("Роль с таким именем уже существует.");
        }

        this.id = "role_" + UUID.randomUUID().toString();
        this.name = trimmedName;
        this.description = description.trim();
        this.permissions = permissions;
    }

    public void addPermission(Permission permission) {
        if (permission != null) {
            permissions.add(permission);
        }
    }

    public void removePermission(Permission permission) {
        if (permission != null) {
            permissions.remove(permission);
        }
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) return false;

        return permissions.stream()
                .anyMatch(p -> p.matches(permissionName, resource));
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", permissionsCount=" + permissions.size() +
                '}';
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Role: %s [ID: %s]\n", name, id));
        sb.append(String.format("Description: %s\n", description));
        sb.append(String.format("Permissions (%d):", permissions.size()));

        for (Permission p : permissions) {
            sb.append("\n- ").append(p.format());
        }

        return sb.toString();
    }
}