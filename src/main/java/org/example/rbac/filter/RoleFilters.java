package org.example.rbac.filter;

import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.util.ValidationUtils;

public class RoleFilters {

    private RoleFilters() {}

    public static RoleFilter byName(String name) {
        ValidationUtils.requireNonEmpty(name, "Role name");
        String norm = ValidationUtils.normalizeString(name);
        return role -> role.getName().equals(norm);
    }

    public static RoleFilter byNameContains(String substring) {
        ValidationUtils.requireNonEmpty(substring, "Substring");
        String norm = ValidationUtils.normalizeString(substring).toLowerCase();
        return role -> role.getName().toLowerCase().contains(norm);
    }

    public static RoleFilter hasPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }
        return role -> role.hasPermission(permission);
    }

    public static RoleFilter hasPermission(String permissionName, String resource) {
        ValidationUtils.requireNonEmpty(permissionName, "Permission name");
        ValidationUtils.requireNonEmpty(resource, "Resource");
        String normName = ValidationUtils.normalizeString(permissionName).toUpperCase();
        String normRes = ValidationUtils.normalizeString(resource).toLowerCase();
        return role -> role.hasPermission(normName, normRes);
    }

    public static RoleFilter hasAtLeastNPermissions(int n) {
        return role -> role.getPermissions().size() >= n;
    }
}