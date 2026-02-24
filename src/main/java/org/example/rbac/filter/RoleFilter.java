package org.example.rbac.filter;

import org.example.rbac.model.Role;

@FunctionalInterface
public interface RoleFilter {

    boolean test(Role role);

    default RoleFilter and(RoleFilter other) {
        return role -> test(role) && other.test(role);
    }

    default RoleFilter or(RoleFilter other) {
        return role -> test(role) || other.test(role);
    }
}