package org.example.rbac.manager;

import org.example.rbac.filter.AssignmentFilter;
import org.example.rbac.model.Permission;
import org.example.rbac.model.PermanentAssignment;
import org.example.rbac.model.Role;
import org.example.rbac.model.RoleAssignment;
import org.example.rbac.model.TemporaryAssignment;
import org.example.rbac.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();
    private final UserManager userManager;
    private final RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        if (userManager == null || roleManager == null) {
            throw new IllegalArgumentException();
        }
        this.userManager = userManager;
        this.roleManager = roleManager;

        this.roleManager.setInUseChecker(role -> !findByRole(role).isEmpty());
    }

    @Override
    public synchronized void add(RoleAssignment item) {
        if (item == null) {
            throw new IllegalArgumentException();
        }
        if (!userManager.exists(item.user().username())) {
            throw new IllegalArgumentException();
        }
        if (!roleManager.exists(item.role().getName())) {
            throw new IllegalArgumentException();
        }
        if (assignments.containsKey(item.assignmentId())) {
            throw new IllegalArgumentException();
        }

        boolean hasDuplicate = assignments.values().stream()
                .anyMatch(a -> a.isActive() &&
                        a.user().equals(item.user()) &&
                        a.role().equals(item.role()));
        if (hasDuplicate) {
            throw new IllegalStateException();
        }

        assignments.put(item.assignmentId(), item);
    }

    @Override
    public synchronized boolean remove(RoleAssignment item) {
        if (item == null || !assignments.containsKey(item.assignmentId())) {
            return false;
        }
        assignments.remove(item.assignmentId());
        return true;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public synchronized void clear() {
        assignments.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        return assignments.values().stream()
                .filter(a -> a.user().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return Collections.emptyList();
        }
        return assignments.values().stream()
                .filter(a -> a.role().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return assignments.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        if (filter == null || sorter == null) {
            throw new IllegalArgumentException();
        }
        return assignments.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignments.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignments.values().stream()
                .filter(a -> a instanceof TemporaryAssignment)
                .map(a -> (TemporaryAssignment) a)
                .filter(TemporaryAssignment::isExpired)
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }
        return assignments.values().stream()
                .anyMatch(a -> a.isActive() && a.user().equals(user) && a.role().equals(role));
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }
        return assignments.values().stream()
                .filter(a -> a.isActive() && a.user().equals(user))
                .anyMatch(a -> a.role().hasPermission(permissionName, resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return Collections.emptySet();
        }
        return assignments.values().stream()
                .filter(a -> a.isActive() && a.user().equals(user))
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public synchronized void revokeAssignment(String assignmentId) {
        if (assignmentId == null || !assignments.containsKey(assignmentId)) {
            throw new IllegalArgumentException();
        }
        RoleAssignment a = assignments.get(assignmentId);
        if (a instanceof PermanentAssignment pa) {
            pa.revoke();
        } else {
            throw new IllegalStateException();
        }
    }

    public synchronized void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        if (assignmentId == null || !assignments.containsKey(assignmentId)) {
            throw new IllegalArgumentException();
        }
        RoleAssignment a = assignments.get(assignmentId);
        if (a instanceof TemporaryAssignment ta) {
            ta.extend(newExpirationDate);
        } else {
            throw new IllegalStateException();
        }
    }

    public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return assignments.values().parallelStream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public void processExpirations(AuditLog auditLog) {
        assignments.values().forEach(a -> {
            if (a instanceof TemporaryAssignment ta) {
                if (ta.isActive() && ta.isExpired()) {
                    if (ta.isAutoRenew()) {
                        auditLog.log("ASSIGNMENT_AUTORENEW", "SYSTEM", ta.user().username(), "Role: " + ta.role().getName());
                    } else {
                        auditLog.log("ASSIGNMENT_EXPIRED", "SYSTEM", ta.user().username(), "Role: " + ta.role().getName());
                    }
                }
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentManager that = (AssignmentManager) o;
        return Objects.equals(assignments, that.assignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignments);
    }
}