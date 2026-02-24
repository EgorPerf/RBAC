package org.example.rbac.filter;

import org.example.rbac.model.Role;

import java.util.Comparator;

public class RoleSorters {

    private RoleSorters() {}

    public static Comparator<Role> byName() {
        return Comparator.comparing(Role::getName);
    }

    public static Comparator<Role> byPermissionCount() {
        return Comparator.comparingInt(role -> role.getPermissions().size());
    }
}