package org.example.rbac.manager;

import org.example.rbac.filter.RoleFilter;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {

    private final Map<String, Role> byId = new HashMap<>();
    private final Map<String, Role> byName = new HashMap<>();
    private Predicate<Role> inUseChecker = role -> false;

    public void setInUseChecker(Predicate<Role> inUseChecker) {
        this.inUseChecker = inUseChecker != null ? inUseChecker : role -> false;
    }

    @Override
    public void add(Role item) {
        if (item == null) {
            throw new IllegalArgumentException();
        }
        if (byId.containsKey(item.getId()) || byName.containsKey(item.getName())) {
            throw new IllegalArgumentException();
        }
        byId.put(item.getId(), item);
        byName.put(item.getName(), item);
    }

    @Override
    public boolean remove(Role item) {
        if (item == null || !byId.containsKey(item.getId())) {
            return false;
        }
        if (inUseChecker.test(item)) {
            throw new IllegalStateException();
        }
        byId.remove(item.getId());
        byName.remove(item.getName());
        return true;
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public int count() {
        return byId.size();
    }

    @Override
    public void clear() {
        byId.clear();
        byName.clear();
    }

    public Optional<Role> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return byId.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        if (filter == null || sorter == null) {
            throw new IllegalArgumentException();
        }
        return byId.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String name) {
        if (name == null) {
            return false;
        }
        return byName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        if (roleName == null || permission == null) {
            throw new IllegalArgumentException();
        }
        Role role = byName.get(roleName);
        if (role == null) {
            throw new IllegalArgumentException();
        }
        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        if (roleName == null || permission == null) {
            throw new IllegalArgumentException();
        }
        Role role = byName.get(roleName);
        if (role == null) {
            throw new IllegalArgumentException();
        }
        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) {
            throw new IllegalArgumentException();
        }
        return byId.values().stream()
                .filter(role -> role.hasPermission(permissionName, resource))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleManager that = (RoleManager) o;
        return Objects.equals(byId, that.byId) && Objects.equals(byName, that.byName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(byId, byName);
    }
}