package org.example.rbac.model;

import org.example.rbac.util.ValidationUtils;

public record Permission(String name, String resource, String description) {

    public Permission {
        ValidationUtils.requireNonEmpty(name, "Permission name");
        ValidationUtils.requireNonEmpty(resource, "Resource");
        ValidationUtils.requireNonEmpty(description, "Description");

        name = ValidationUtils.normalizeString(name).toUpperCase();
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Name не должно содержать пробелов.");
        }

        resource = ValidationUtils.normalizeString(resource).toLowerCase();
        description = ValidationUtils.normalizeString(description);
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