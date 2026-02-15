package org.example.rbac.model;

public record Permission(String name, String resource, String description) {

    public Permission {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name не может быть пустым.");
        }
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Name не должно содержать пробелов.");
        }
        name = name.toUpperCase();

        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("Resource не может быть пустым.");
        }
        resource = resource.toLowerCase();

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description не может быть пустым.");
        }
    }

    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatch = namePattern == null || name.contains(namePattern) || name.matches(namePattern);
        boolean resourceMatch = resourcePattern == null || resource.contains(resourcePattern) || resource.matches(resourcePattern);

        return nameMatch && resourceMatch;
    }
}