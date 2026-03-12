package org.example.rbac.command;

import org.example.rbac.filter.UserFilter;
import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.PermanentAssignment;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.RoleAssignment;
import org.example.rbac.model.TemporaryAssignment;
import org.example.rbac.model.User;
import org.example.rbac.report.ReportGenerator;
import org.example.rbac.util.ConsoleUtils;
import org.example.rbac.util.FormatUtils;
import org.example.rbac.util.ValidationUtils;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class CommandRegistry {

    private CommandRegistry() {}

    private record NamedItem<T>(String name, T item) {
        @Override
        public String toString() {
            return name;
        }
    }

    public static void registerUserCommands(CommandParser parser) {

        parser.registerCommand("user-list", "List all users or filter by keyword", (scanner, system) -> {
            String filterStr = ConsoleUtils.promptString(scanner, "Enter filter keyword", false);
            UserFilter filter = filterStr.isEmpty() ? null : user ->
                    user.username().contains(filterStr) ||
                            user.fullName().contains(filterStr) ||
                            user.email().contains(filterStr);
            printUsersTable(system.getUserManager().findByFilter(filter));
        });

        parser.registerCommand("user-create", "Create a new user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            String fullName = ConsoleUtils.promptString(scanner, "Enter full name", true);
            String email = ConsoleUtils.promptString(scanner, "Enter email", true);
            try {
                User user = User.create(username, fullName, email);
                system.getUserManager().add(user);
                system.getAuditLog().log("USER_CREATE", system.getCurrentUser(), user.username(), "Created user");
                System.out.println(ConsoleUtils.ANSI_GREEN + "User created successfully." + ConsoleUtils.ANSI_RESET);
            } catch (IllegalArgumentException e) {
                System.out.println(ConsoleUtils.ANSI_RED + "Error: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
            }
        });

        parser.registerCommand("user-view", "View user info", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                System.out.println(ConsoleUtils.ANSI_YELLOW + FormatUtils.formatHeader("User Info") + ConsoleUtils.ANSI_RESET);

                String userInfo = String.format("Username: %s\nFull Name: %s\nEmail: %s", user.username(), user.fullName(), user.email());
                System.out.println(FormatUtils.formatBox(userInfo));

                List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
                System.out.println(ConsoleUtils.ANSI_CYAN + "Roles:" + ConsoleUtils.ANSI_RESET);
                if (assignments.isEmpty()) System.out.println("  None");
                else assignments.stream().filter(RoleAssignment::isActive).forEach(a -> System.out.println("  - " + a.role().getName()));

                Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user);
                System.out.println(ConsoleUtils.ANSI_CYAN + "Permissions:" + ConsoleUtils.ANSI_RESET);
                if (permissions.isEmpty()) System.out.println("  None");
                else permissions.forEach(p -> System.out.println("  - " + p.name() + ":" + p.resource()));
            }, () -> System.out.println(ConsoleUtils.ANSI_RED + "User not found." + ConsoleUtils.ANSI_RESET));
        });

        parser.registerCommand("user-update", "Update user data", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username to update", true);
            if (!system.getUserManager().exists(username)) {
                System.out.println(ConsoleUtils.ANSI_RED + "User not found." + ConsoleUtils.ANSI_RESET);
                return;
            }
            String fullName = ConsoleUtils.promptString(scanner, "Enter new full name", true);
            String email = ConsoleUtils.promptString(scanner, "Enter new email", true);
            try {
                system.getUserManager().update(username, fullName, email);
                System.out.println(ConsoleUtils.ANSI_GREEN + "User updated successfully." + ConsoleUtils.ANSI_RESET);
            } catch (IllegalArgumentException e) {
                System.out.println(ConsoleUtils.ANSI_RED + "Error: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
            }
        });

        parser.registerCommand("user-delete", "Delete a user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username to delete", true);
            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                if (ConsoleUtils.promptYesNo(scanner, "Are you sure you want to delete user " + username + "?")) {
                    new ArrayList<>(system.getAssignmentManager().findByUser(user))
                            .forEach(system.getAssignmentManager()::remove);
                    system.getUserManager().remove(user);
                    system.getAuditLog().log("USER_DELETE", system.getCurrentUser(), user.username(), "Deleted user and assignments");
                    System.out.println(ConsoleUtils.ANSI_GREEN + "User deleted successfully." + ConsoleUtils.ANSI_RESET);
                } else {
                    System.out.println("Deletion cancelled.");
                }
            }, () -> System.out.println(ConsoleUtils.ANSI_RED + "User not found." + ConsoleUtils.ANSI_RESET));
        });

        parser.registerCommand("user-search", "Search users by specific filters", (scanner, system) -> {
            List<String> options = List.of("By username", "By email", "By email domain", "By full name");
            String choice = ConsoleUtils.promptChoice(scanner, "Select search filter", options);
            String query = ConsoleUtils.promptString(scanner, "Enter search string", true);

            UserFilter filter = switch (options.indexOf(choice)) {
                case 0 -> user -> user.username().contains(query);
                case 1 -> user -> user.email().contains(query);
                case 2 -> user -> user.email().endsWith(query);
                case 3 -> user -> user.fullName().contains(query);
                default -> null;
            };

            printUsersTable(system.getUserManager().findByFilter(filter));
        });
    }

    public static void registerRoleCommands(CommandParser parser) {

        parser.registerCommand("role-list", "List all roles", (scanner, system) -> {
            printRolesTable(system.getRoleManager().findAll());
        });

        parser.registerCommand("role-create", "Create a new role", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            String description = ConsoleUtils.promptString(scanner, "Enter role description", true);

            Set<Permission> permissions = new HashSet<>();
            while (ConsoleUtils.promptYesNo(scanner, "Add a permission?")) {
                try {
                    String pName = ConsoleUtils.promptString(scanner, "Permission name (e.g. READ)", true);
                    String pResource = ConsoleUtils.promptString(scanner, "Resource (e.g. DATA)", true);
                    String pDesc = ConsoleUtils.promptString(scanner, "Description", true);
                    permissions.add(new Permission(pName, pResource, pDesc));
                    System.out.println(ConsoleUtils.ANSI_GREEN + "Permission added." + ConsoleUtils.ANSI_RESET);
                } catch (IllegalArgumentException e) {
                    System.out.println(ConsoleUtils.ANSI_RED + "Error: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
                }
            }

            try {
                Role role = new Role(name, description, permissions);
                system.getRoleManager().add(role);
                system.getAuditLog().log("ROLE_CREATE", system.getCurrentUser(), role.getName(), "Created role");
                System.out.println(ConsoleUtils.ANSI_GREEN + "Role created successfully." + ConsoleUtils.ANSI_RESET);
            } catch (IllegalArgumentException e) {
                System.out.println(ConsoleUtils.ANSI_RED + "Error: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
            }
        });

        parser.registerCommand("role-view", "View role info", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            system.getRoleManager().findByName(name).ifPresentOrElse(
                    role -> System.out.println(FormatUtils.formatBox(role.format())),
                    () -> System.out.println(ConsoleUtils.ANSI_RED + "Role not found." + ConsoleUtils.ANSI_RESET)
            );
        });

        parser.registerCommand("role-update", "Update a role (name/description)", (scanner, system) -> {
            String oldName = ConsoleUtils.promptString(scanner, "Enter role name to update", true);
            system.getRoleManager().findByName(oldName).ifPresentOrElse(role -> {
                String newName = ConsoleUtils.promptString(scanner, "Enter new role name", true);
                String newDesc = ConsoleUtils.promptString(scanner, "Enter new description", true);

                if (!oldName.equalsIgnoreCase(newName) && system.getRoleManager().exists(newName)) {
                    System.out.println(ConsoleUtils.ANSI_RED + "Error: Role with this name already exists." + ConsoleUtils.ANSI_RESET);
                    return;
                }

                try {
                    Role.clearUsedNames();
                    Role newRole = new Role(newName, newDesc, new HashSet<>(role.getPermissions()));
                    system.getRoleManager().add(newRole);

                    for (User u : system.getUserManager().findAll()) {
                        for (RoleAssignment a : new ArrayList<>(system.getAssignmentManager().findByUser(u))) {
                            if (a.role().equals(role)) {
                                system.getAssignmentManager().remove(a);
                                system.getAssignmentManager().add(new PermanentAssignment(u, newRole, a.metadata()));
                            }
                        }
                    }
                    system.getRoleManager().remove(role);
                    System.out.println(ConsoleUtils.ANSI_GREEN + "Role updated successfully." + ConsoleUtils.ANSI_RESET);
                } catch (Exception e) {
                    System.out.println(ConsoleUtils.ANSI_RED + "Error updating role: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
                }
            }, () -> System.out.println(ConsoleUtils.ANSI_RED + "Role not found." + ConsoleUtils.ANSI_RESET));
        });

        parser.registerCommand("role-delete", "Delete a role", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            system.getRoleManager().findByName(name).ifPresentOrElse(role -> {
                List<User> assignedUsers = system.getUserManager().findAll().stream()
                        .filter(u -> system.getAssignmentManager().userHasRole(u, role))
                        .toList();

                if (!assignedUsers.isEmpty()) {
                    System.out.println(ConsoleUtils.ANSI_YELLOW + "WARNING: This role is assigned to " + assignedUsers.size() + " users." + ConsoleUtils.ANSI_RESET);
                }

                if (ConsoleUtils.promptYesNo(scanner, "Are you sure you want to delete role " + name + "?")) {
                    assignedUsers.forEach(u -> new ArrayList<>(system.getAssignmentManager().findByUser(u))
                            .stream().filter(a -> a.role().equals(role))
                            .forEach(system.getAssignmentManager()::remove));
                    system.getRoleManager().remove(role);
                    system.getAuditLog().log("ROLE_DELETE", system.getCurrentUser(), role.getName(), "Deleted role");
                    System.out.println(ConsoleUtils.ANSI_GREEN + "Role deleted successfully." + ConsoleUtils.ANSI_RESET);
                } else {
                    System.out.println("Deletion cancelled.");
                }
            }, () -> System.out.println(ConsoleUtils.ANSI_RED + "Role not found." + ConsoleUtils.ANSI_RESET));
        });

        parser.registerCommand("role-add-permission", "Add a permission to a role", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            if (!system.getRoleManager().exists(name)) {
                System.out.println(ConsoleUtils.ANSI_RED + "Role not found." + ConsoleUtils.ANSI_RESET);
                return;
            }
            try {
                String pName = ConsoleUtils.promptString(scanner, "Enter permission name", true);
                String pResource = ConsoleUtils.promptString(scanner, "Enter resource", true);
                String pDesc = ConsoleUtils.promptString(scanner, "Enter description", true);
                system.getRoleManager().addPermissionToRole(name, new Permission(pName, pResource, pDesc));
                System.out.println(ConsoleUtils.ANSI_GREEN + "Permission added successfully." + ConsoleUtils.ANSI_RESET);
            } catch (IllegalArgumentException e) {
                System.out.println(ConsoleUtils.ANSI_RED + "Error: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
            }
        });

        parser.registerCommand("role-remove-permission", "Remove a permission from a role", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            system.getRoleManager().findByName(name).ifPresentOrElse(role -> {
                List<Permission> perms = new ArrayList<>(role.getPermissions());
                if (perms.isEmpty()) {
                    System.out.println("Role has no permissions.");
                    return;
                }
                List<NamedItem<Permission>> options = perms.stream()
                        .map(p -> new NamedItem<>(p.format(), p)).toList();

                Permission toRemove = ConsoleUtils.promptChoice(scanner, "Select permission to remove", options).item();
                system.getRoleManager().removePermissionFromRole(name, toRemove);
                System.out.println(ConsoleUtils.ANSI_GREEN + "Permission removed successfully." + ConsoleUtils.ANSI_RESET);
            }, () -> System.out.println(ConsoleUtils.ANSI_RED + "Role not found." + ConsoleUtils.ANSI_RESET));
        });

        parser.registerCommand("role-search", "Search roles by filters", (scanner, system) -> {
            List<String> options = List.of("By name (contains)", "By permission (name)", "By minimum permissions");
            String choice = ConsoleUtils.promptChoice(scanner, "Select filter", options);
            List<Role> roles = system.getRoleManager().findAll();

            List<Role> result = switch (options.indexOf(choice)) {
                case 0 -> {
                    String q = ConsoleUtils.promptString(scanner, "Enter name query", true);
                    yield roles.stream().filter(r -> r.getName().contains(q)).toList();
                }
                case 1 -> {
                    String q = ConsoleUtils.promptString(scanner, "Enter permission name", true).toUpperCase();
                    yield roles.stream().filter(r -> r.getPermissions().stream().anyMatch(p -> p.name().contains(q))).toList();
                }
                case 2 -> {
                    int min = ConsoleUtils.promptInt(scanner, "Enter minimum permissions", 0, 100);
                    yield roles.stream().filter(r -> r.getPermissions().size() >= min).toList();
                }
                default -> new ArrayList<>();
            };
            printRolesTable(result);
        });
    }

    public static void registerAssignmentCommands(CommandParser parser) {

        parser.registerCommand("assign-role", "Assign a role to a user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            var userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println(ConsoleUtils.ANSI_RED + "User not found." + ConsoleUtils.ANSI_RESET);
                return;
            }

            List<Role> roles = system.getRoleManager().findAll();
            if (roles.isEmpty()) {
                System.out.println(ConsoleUtils.ANSI_RED + "No roles available." + ConsoleUtils.ANSI_RESET);
                return;
            }

            List<NamedItem<Role>> roleOptions = roles.stream().map(r -> new NamedItem<>(r.getName(), r)).toList();
            Role role = ConsoleUtils.promptChoice(scanner, "Select role", roleOptions).item();

            String typeChoice = ConsoleUtils.promptChoice(scanner, "Select assignment type", List.of("Permanent", "Temporary"));
            String reason = ConsoleUtils.promptString(scanner, "Enter reason", true);
            AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);

            try {
                if (typeChoice.equals("Temporary")) {
                    String dateStr = ConsoleUtils.promptString(scanner, "Enter expiration date (YYYY-MM-DD)", true);
                    if (!ValidationUtils.isValidDate(dateStr)) {
                        System.out.println(ConsoleUtils.ANSI_RED + "Invalid date format." + ConsoleUtils.ANSI_RESET);
                        return;
                    }
                    boolean autoRenew = ConsoleUtils.promptYesNo(scanner, "Enable auto-renew?");
                    String expiration = LocalDate.parse(dateStr).atTime(23, 59, 59).toString();
                    system.getAssignmentManager().add(new TemporaryAssignment(userOpt.get(), role, metadata, expiration, autoRenew));
                } else {
                    system.getAssignmentManager().add(new PermanentAssignment(userOpt.get(), role, metadata));
                }
                system.getAuditLog().log("ASSIGN_ROLE", system.getCurrentUser(), username, "Assigned " + typeChoice + " role " + role.getName());
                System.out.println(ConsoleUtils.ANSI_GREEN + "Role assigned successfully." + ConsoleUtils.ANSI_RESET);
            } catch (IllegalArgumentException e) {
                System.out.println(ConsoleUtils.ANSI_RED + "Error: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
            }
        });

        parser.registerCommand("revoke-role", "Revoke a role from a user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            var userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println(ConsoleUtils.ANSI_RED + "User not found." + ConsoleUtils.ANSI_RESET);
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(userOpt.get());
            if (assignments.isEmpty()) {
                System.out.println("User has no assignments.");
                return;
            }

            List<NamedItem<RoleAssignment>> options = assignments.stream()
                    .map(a -> new NamedItem<>(String.format("[%s] %s (%s)", a.assignmentType(), a.role().getName(), a.isActive() ? "ACTIVE" : "EXPIRED"), a))
                    .toList();

            RoleAssignment toRevoke = ConsoleUtils.promptChoice(scanner, "Select assignment to revoke", options).item();
            system.getAssignmentManager().remove(toRevoke);
            system.getAuditLog().log("REVOKE_ROLE", system.getCurrentUser(), username, "Revoked role " + toRevoke.role().getName());
            System.out.println(ConsoleUtils.ANSI_GREEN + "Assignment revoked successfully." + ConsoleUtils.ANSI_RESET);
        });

        parser.registerCommand("assignment-list", "List all assignments in the system", (scanner, system) -> {
            List<String[]> rows = new ArrayList<>();
            for (User u : system.getUserManager().findAll()) {
                for (RoleAssignment a : system.getAssignmentManager().findByUser(u)) {
                    rows.add(new String[]{u.username(), a.role().getName(), (a instanceof PermanentAssignment) ? "PERM" : "TEMP",
                            a.isActive() ? "ACTIVE" : "EXPIRED", a.metadata().assignedAt().substring(0, 16).replace("T", " ")});
                }
            }
            if (rows.isEmpty()) System.out.println("No assignments found.");
            else System.out.println(ConsoleUtils.ANSI_CYAN + FormatUtils.formatTable(new String[]{"Username", "Role", "Type", "Status", "Assigned At"}, rows) + ConsoleUtils.ANSI_RESET);
        });

        parser.registerCommand("assignment-list-user", "List assignments for a user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            system.getUserManager().findByUsername(username).ifPresentOrElse(u -> {
                List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(u);
                if (assignments.isEmpty()) System.out.println("No assignments found.");
                else {
                    List<String[]> rows = assignments.stream().map(a -> new String[]{a.role().getName(), (a instanceof PermanentAssignment) ? "PERM" : "TEMP",
                            a.isActive() ? "ACTIVE" : "EXPIRED", a.metadata().assignedAt().substring(0, 16).replace("T", " ")}).toList();
                    System.out.println(ConsoleUtils.ANSI_CYAN + FormatUtils.formatTable(new String[]{"Role", "Type", "Status", "Assigned At"}, rows) + ConsoleUtils.ANSI_RESET);
                }
            }, () -> System.out.println(ConsoleUtils.ANSI_RED + "User not found." + ConsoleUtils.ANSI_RESET));
        });

        parser.registerCommand("assignment-list-role", "List users with a specific role", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Enter role name", true);
            system.getRoleManager().findByName(roleName).ifPresentOrElse(r -> {
                List<User> users = system.getUserManager().findAll().stream()
                        .filter(u -> system.getAssignmentManager().userHasRole(u, r))
                        .toList();
                if (users.isEmpty()) {
                    System.out.println("No users found with this role.");
                } else {
                    printUsersTable(users);
                }
            }, () -> System.out.println(ConsoleUtils.ANSI_RED + "Role not found." + ConsoleUtils.ANSI_RESET));
        });

        parser.registerCommand("assignment-active", "List all active assignments", (scanner, system) -> {
            List<String[]> rows = new ArrayList<>();
            for (User u : system.getUserManager().findAll()) {
                for (RoleAssignment a : system.getAssignmentManager().findByUser(u)) {
                    if (a.isActive()) rows.add(new String[]{u.username(), a.role().getName(), (a instanceof PermanentAssignment) ? "PERM" : "TEMP", a.metadata().assignedAt().substring(0, 16).replace("T", " ")});
                }
            }
            if (rows.isEmpty()) System.out.println("No active assignments found.");
            else System.out.println(ConsoleUtils.ANSI_CYAN + FormatUtils.formatTable(new String[]{"Username", "Role", "Type", "Assigned At"}, rows) + ConsoleUtils.ANSI_RESET);
        });

        parser.registerCommand("assignment-expired", "List all expired temporary assignments", (scanner, system) -> {
            List<String[]> rows = new ArrayList<>();
            for (User u : system.getUserManager().findAll()) {
                for (RoleAssignment a : system.getAssignmentManager().findByUser(u)) {
                    if (a instanceof TemporaryAssignment ta && !a.isActive()) {
                        rows.add(new String[]{u.username(), a.role().getName(), ta.getExpiresAt().replace("T", " ")});
                    }
                }
            }
            if (rows.isEmpty()) System.out.println("No expired assignments found.");
            else System.out.println(ConsoleUtils.ANSI_CYAN + FormatUtils.formatTable(new String[]{"Username", "Role", "Expired At"}, rows) + ConsoleUtils.ANSI_RESET);
        });

        parser.registerCommand("assignment-extend", "Extend a temporary assignment", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            var userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println(ConsoleUtils.ANSI_RED + "User not found." + ConsoleUtils.ANSI_RESET);
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(userOpt.get()).stream()
                    .filter(a -> a instanceof TemporaryAssignment).toList();

            if (assignments.isEmpty()) {
                System.out.println(ConsoleUtils.ANSI_RED + "No temporary assignments found for this user." + ConsoleUtils.ANSI_RESET);
                return;
            }

            List<NamedItem<RoleAssignment>> options = assignments.stream()
                    .map(a -> new NamedItem<>(a.role().getName() + " (Expires: " + ((TemporaryAssignment) a).getExpiresAt() + ")", a))
                    .toList();

            TemporaryAssignment target = (TemporaryAssignment) ConsoleUtils.promptChoice(scanner, "Select assignment to extend", options).item();
            String dateStr = ConsoleUtils.promptString(scanner, "Enter new expiration date (YYYY-MM-DD)", true);

            if (!ValidationUtils.isValidDate(dateStr)) {
                System.out.println(ConsoleUtils.ANSI_RED + "Invalid date format." + ConsoleUtils.ANSI_RESET);
                return;
            }
            try {
                target.extend(LocalDate.parse(dateStr).atTime(23, 59, 59).toString());
                System.out.println(ConsoleUtils.ANSI_GREEN + "Assignment extended successfully." + ConsoleUtils.ANSI_RESET);
            } catch (IllegalArgumentException e) {
                System.out.println(ConsoleUtils.ANSI_RED + "Error: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
            }
        });

        parser.registerCommand("assignment-search", "Search assignments by filters", (scanner, system) -> {
            List<String> options = List.of("By user", "By role", "By type (PERM/TEMP)", "By status (ACTIVE/EXPIRED)", "Assigned after date", "Expires before date");
            String choice = ConsoleUtils.promptChoice(scanner, "Select filter", options);

            List<RoleAssignment> allAssignments = new ArrayList<>();
            system.getUserManager().findAll().forEach(u -> allAssignments.addAll(system.getAssignmentManager().findByUser(u)));

            List<RoleAssignment> result = switch (options.indexOf(choice)) {
                case 0 -> {
                    String un = ConsoleUtils.promptString(scanner, "Enter username", true);
                    yield allAssignments.stream().filter(a -> a.user().username().equals(un)).toList();
                }
                case 1 -> {
                    String rn = ConsoleUtils.promptString(scanner, "Enter role name", true);
                    yield allAssignments.stream().filter(a -> a.role().getName().equals(rn)).toList();
                }
                case 2 -> {
                    String t = ConsoleUtils.promptChoice(scanner, "Select type", List.of("PERM", "TEMP"));
                    yield allAssignments.stream().filter(a -> ((a instanceof PermanentAssignment) ? "PERM" : "TEMP").equals(t)).toList();
                }
                case 3 -> {
                    String s = ConsoleUtils.promptChoice(scanner, "Select status", List.of("ACTIVE", "EXPIRED"));
                    yield allAssignments.stream().filter(a -> (a.isActive() ? "ACTIVE" : "EXPIRED").equals(s)).toList();
                }
                case 4 -> {
                    String d = ConsoleUtils.promptString(scanner, "Enter assigned after date (YYYY-MM-DD)", true);
                    if (!ValidationUtils.isValidDate(d)) { System.out.println(ConsoleUtils.ANSI_RED + "Invalid date." + ConsoleUtils.ANSI_RESET); yield new ArrayList<RoleAssignment>(); }
                    LocalDateTime dt = LocalDate.parse(d).atStartOfDay();
                    yield allAssignments.stream().filter(a -> LocalDateTime.parse(a.metadata().assignedAt().toString()).isAfter(dt)).toList();
                }
                case 5 -> {
                    String d = ConsoleUtils.promptString(scanner, "Enter expires before date (YYYY-MM-DD)", true);
                    if (!ValidationUtils.isValidDate(d)) { System.out.println(ConsoleUtils.ANSI_RED + "Invalid date." + ConsoleUtils.ANSI_RESET); yield new ArrayList<RoleAssignment>(); }
                    LocalDateTime dt = LocalDate.parse(d).atTime(23, 59, 59);
                    yield allAssignments.stream().filter(a -> a instanceof TemporaryAssignment ta && LocalDateTime.parse(ta.getExpiresAt()).isBefore(dt)).toList();
                }
                default -> new ArrayList<>();
            };

            if (result.isEmpty()) System.out.println("No assignments found matching criteria.");
            else {
                List<String[]> rows = result.stream().map(a -> new String[]{a.user().username(), a.role().getName(), (a instanceof PermanentAssignment) ? "PERM" : "TEMP",
                        a.isActive() ? "ACTIVE" : "EXPIRED", a.metadata().assignedAt().substring(0, 16).replace("T", " ")}).toList();
                System.out.println(ConsoleUtils.ANSI_CYAN + FormatUtils.formatTable(new String[]{"Username", "Role", "Type", "Status", "Assigned At"}, rows) + ConsoleUtils.ANSI_RESET);
            }
        });
    }

    public static void registerPermissionCommands(CommandParser parser) {

        parser.registerCommand("permissions-user", "List all permissions of a user grouped by resource", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            system.getUserManager().findByUsername(username).ifPresentOrElse(u -> {
                Set<Permission> perms = system.getAssignmentManager().getUserPermissions(u);
                if (perms.isEmpty()) {
                    System.out.println("User has no permissions.");
                    return;
                }
                Map<String, List<Permission>> grouped = new HashMap<>();
                perms.forEach(p -> grouped.computeIfAbsent(p.resource(), k -> new ArrayList<>()).add(p));
                grouped.forEach((res, pList) -> {
                    System.out.println(ConsoleUtils.ANSI_YELLOW + "Resource: " + res + ConsoleUtils.ANSI_RESET);
                    pList.forEach(p -> System.out.println("  - " + p.name() + " (" + p.description() + ")"));
                });
            }, () -> System.out.println(ConsoleUtils.ANSI_RED + "User not found." + ConsoleUtils.ANSI_RESET));
        });

        parser.registerCommand("permissions-check", "Check if user has specific permission", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            system.getUserManager().findByUsername(username).ifPresentOrElse(u -> {
                String pName = ConsoleUtils.promptString(scanner, "Enter permission name", true);
                String pRes = ConsoleUtils.promptString(scanner, "Enter resource", true);
                if (system.getAssignmentManager().userHasPermission(u, pName, pRes)) {
                    System.out.println(ConsoleUtils.ANSI_GREEN + "Access GRANTED." + ConsoleUtils.ANSI_RESET);
                    List<String> providingRoles = system.getAssignmentManager().findByUser(u).stream()
                            .filter(RoleAssignment::isActive)
                            .filter(a -> a.role().hasPermission(pName, pRes))
                            .map(a -> a.role().getName())
                            .toList();
                    System.out.println("Provided by roles: " + String.join(", ", providingRoles));
                } else {
                    System.out.println(ConsoleUtils.ANSI_RED + "Access DENIED." + ConsoleUtils.ANSI_RESET);
                }
            }, () -> System.out.println(ConsoleUtils.ANSI_RED + "User not found." + ConsoleUtils.ANSI_RESET));
        });
    }

    public static void registerUtilityCommands(CommandParser parser) {

        parser.registerCommand("audit-log", "View or save the audit log", (scanner, system) -> {
            String choice = ConsoleUtils.promptChoice(scanner, "Audit Log Options",
                    List.of("Print all", "Filter by performer", "Filter by action", "Save to file"));

            switch (choice) {
                case "Print all" -> system.getAuditLog().printLog();
                case "Filter by performer" -> {
                    String p = ConsoleUtils.promptString(scanner, "Enter performer", true);
                    system.getAuditLog().printLog(system.getAuditLog().getByPerformer(p));
                }
                case "Filter by action" -> {
                    String a = ConsoleUtils.promptString(scanner, "Enter action", true);
                    system.getAuditLog().printLog(system.getAuditLog().getByAction(a));
                }
                case "Save to file" -> {
                    String fn = ConsoleUtils.promptString(scanner, "Enter filename", false);
                    system.getAuditLog().saveToFile(fn.isEmpty() ? "audit.txt" : fn);
                }
            }
        });

        parser.registerCommand("report-users", "Generate and save user report", (scanner, system) -> {
            ReportGenerator generator = new ReportGenerator();
            String report = generator.generateUserReport(system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);
            String fn = ConsoleUtils.promptString(scanner, "Enter filename to save (or leave empty to skip)", false);
            if (!fn.isEmpty()) generator.exportToFile(report, fn);
        });

        parser.registerCommand("report-roles", "Generate and save role report", (scanner, system) -> {
            ReportGenerator generator = new ReportGenerator();
            String report = generator.generateRoleReport(system.getRoleManager(), system.getAssignmentManager());
            System.out.println(report);
            String fn = ConsoleUtils.promptString(scanner, "Enter filename to save (or leave empty to skip)", false);
            if (!fn.isEmpty()) generator.exportToFile(report, fn);
        });

        parser.registerCommand("report-matrix", "Generate and save permission matrix", (scanner, system) -> {
            ReportGenerator generator = new ReportGenerator();
            String report = generator.generatePermissionMatrix(system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);
            String fn = ConsoleUtils.promptString(scanner, "Enter filename to save (or leave empty to skip)", false);
            if (!fn.isEmpty()) generator.exportToFile(report, fn);
        });

        parser.registerCommand("help", "Show all commands with descriptions", (scanner, system) -> parser.printHelp());

        parser.registerCommand("stats", "System statistics", (scanner, system) -> {
            int userCount = system.getUserManager().findAll().size();
            int roleCount = system.getRoleManager().findAll().size();
            int totalAssignments = 0;
            int activeAssignments = 0;
            int expiredAssignments = 0;
            Map<Role, Integer> rolePopularity = new HashMap<>();

            for (User u : system.getUserManager().findAll()) {
                List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(u);
                totalAssignments += assignments.size();
                for (RoleAssignment a : assignments) {
                    if (a.isActive()) activeAssignments++;
                    else expiredAssignments++;
                    rolePopularity.put(a.role(), rolePopularity.getOrDefault(a.role(), 0) + 1);
                }
            }

            double avgRoles = userCount == 0 ? 0.0 : (double) totalAssignments / userCount;

            System.out.println(ConsoleUtils.ANSI_YELLOW + FormatUtils.formatHeader("RBAC System Statistics") + ConsoleUtils.ANSI_RESET);
            String statsText = String.format("Users: %d\nRoles: %d\nAssignments (Total/Active/Expired): %d / %d / %d\nAvg roles per user: %.2f",
                    userCount, roleCount, totalAssignments, activeAssignments, expiredAssignments, avgRoles);

            System.out.println(FormatUtils.formatBox(statsText));
            System.out.println(ConsoleUtils.ANSI_YELLOW + "Top-3 popular roles:" + ConsoleUtils.ANSI_RESET);

            rolePopularity.entrySet().stream()
                    .sorted(Map.Entry.<Role, Integer>comparingByValue().reversed())
                    .limit(3)
                    .forEach(e -> System.out.println("  - " + e.getKey().getName() + " (" + e.getValue() + " assignments)"));
        });

        parser.registerCommand("clear", "Clear the screen", (scanner, system) -> {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        });

        parser.registerCommand("save", "Save data to file", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Enter filename", false);
            if (filename.isEmpty()) filename = "rbac.txt";
            try (PrintWriter writer = new PrintWriter(filename)) {
                writer.println("[USERS]");
                for (User u : system.getUserManager().findAll()) {
                    writer.printf("%s|%s|%s%n", u.username(), u.fullName(), u.email());
                }
                writer.println("[ROLES]");
                for (Role r : system.getRoleManager().findAll()) {
                    writer.printf("%s|%s|", r.getName(), r.getDescription());
                    List<String> perms = new ArrayList<>();
                    for (Permission p : r.getPermissions()) perms.add(p.name() + "^" + p.resource() + "^" + p.description());
                    writer.println(String.join("~", perms));
                }
                writer.println("[ASSIGNMENTS]");
                for (User u : system.getUserManager().findAll()) {
                    for (RoleAssignment a : system.getAssignmentManager().findByUser(u)) {
                        AssignmentMetadata m = a.metadata();
                        if (a instanceof PermanentAssignment) {
                            writer.printf("PERM|%s|%s|%s|%s|%s%n", u.username(), a.role().getName(), m.assignedAt(), m.assignedBy(), m.reason());
                        } else if (a instanceof TemporaryAssignment ta) {
                            writer.printf("TEMP|%s|%s|%s|%s|%s|%s|%s%n", u.username(), a.role().getName(), m.assignedAt(), m.assignedBy(), m.reason(), ta.getExpiresAt(), ta.isAutoRenew());
                        }
                    }
                }
                System.out.println(ConsoleUtils.ANSI_GREEN + "Data saved successfully to " + filename + ConsoleUtils.ANSI_RESET);
            } catch (Exception e) {
                System.out.println(ConsoleUtils.ANSI_RED + "Error saving data: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
            }
        });

        parser.registerCommand("load", "Load data from file", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Enter filename", false);
            if (filename.isEmpty()) filename = "rbac.txt";
            try (Scanner fileScanner = new Scanner(new File(filename))) {
                String section = "";
                while (fileScanner.hasNextLine()) {
                    String line = fileScanner.nextLine().trim();
                    if (line.isEmpty()) continue;
                    if (line.startsWith("[")) {
                        section = line;
                        continue;
                    }
                    if (section.equals("[USERS]")) {
                        String[] parts = line.split("\\|", -1);
                        if (!system.getUserManager().exists(parts[0])) {
                            system.getUserManager().add(User.create(parts[0], parts[1], parts[2]));
                        }
                    } else if (section.equals("[ROLES]")) {
                        String[] parts = line.split("\\|", -1);
                        if (!system.getRoleManager().exists(parts[0])) {
                            Set<Permission> perms = new HashSet<>();
                            if (parts.length > 2 && !parts[2].isEmpty()) {
                                String[] pStrings = parts[2].split("~");
                                for (String ps : pStrings) {
                                    String[] pData = ps.split("\\^", -1);
                                    perms.add(new Permission(pData[0], pData[1], pData[2]));
                                }
                            }
                            system.getRoleManager().add(new Role(parts[0], parts[1], perms));
                        }
                    } else if (section.equals("[ASSIGNMENTS]")) {
                        String[] parts = line.split("\\|", -1);
                        User u = system.getUserManager().findByUsername(parts[1]).orElse(null);
                        Role r = system.getRoleManager().findByName(parts[2]).orElse(null);
                        if (u != null && r != null) {
                            if (!system.getAssignmentManager().userHasRole(u, r)) {
                                AssignmentMetadata m = new AssignmentMetadata(parts[3], parts[4], parts[5]);
                                if (parts[0].equals("PERM")) {
                                    system.getAssignmentManager().add(new PermanentAssignment(u, r, m));
                                } else if (parts[0].equals("TEMP")) {
                                    system.getAssignmentManager().add(new TemporaryAssignment(u, r, m, parts[6], Boolean.parseBoolean(parts[7])));
                                }
                            }
                        }
                    }
                }
                System.out.println(ConsoleUtils.ANSI_GREEN + "Data loaded successfully from " + filename + ConsoleUtils.ANSI_RESET);
            } catch (Exception e) {
                System.out.println(ConsoleUtils.ANSI_RED + "Error loading data: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
            }
        });

        parser.registerCommand("exit", "Exit the program", (scanner, system) -> {
            if (ConsoleUtils.promptYesNo(scanner, "Are you sure you want to exit?")) {
                if (ConsoleUtils.promptYesNo(scanner, "Save data before exiting?")) {
                    try { parser.executeCommand("save", scanner, system); } catch (Exception ignored) {}
                }
                System.out.println("Exiting...");
                throw new RuntimeException("EXIT_SIGNAL");
            }
        });
    }

    private static void printUsersTable(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        String[] headers = {"Username", "Full Name", "Email"};
        List<String[]> rows = users.stream()
                .map(u -> new String[]{u.username(), FormatUtils.truncate(u.fullName(), 25), u.email()})
                .toList();
        System.out.println(ConsoleUtils.ANSI_CYAN + FormatUtils.formatTable(headers, rows) + ConsoleUtils.ANSI_RESET);
    }

    private static void printRolesTable(List<Role> roles) {
        if (roles.isEmpty()) {
            System.out.println("No roles found.");
            return;
        }
        String[] headers = {"Name", "Permissions", "ID"};
        List<String[]> rows = roles.stream()
                .map(r -> new String[]{r.getName(), String.valueOf(r.getPermissions().size()), r.getId()})
                .toList();
        System.out.println(ConsoleUtils.ANSI_CYAN + FormatUtils.formatTable(headers, rows) + ConsoleUtils.ANSI_RESET);
    }
}