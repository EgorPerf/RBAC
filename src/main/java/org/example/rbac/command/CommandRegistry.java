package org.example.rbac.command;

import org.example.rbac.filter.UserFilter;
import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.PermanentAssignment;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.RoleAssignment;
import org.example.rbac.model.TemporaryAssignment;
import org.example.rbac.model.User;
import org.example.rbac.util.ValidationUtils;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class CommandRegistry {

    private CommandRegistry() {}

    public static void registerUserCommands(CommandParser parser) {

        parser.registerCommand("user-list", "List all users or filter by keyword", (scanner, system) -> {
            System.out.print("Enter filter (leave empty for all): ");
            String filterStr = ValidationUtils.normalizeString(scanner.nextLine());

            UserFilter filter = filterStr.isEmpty() ? null : user ->
                    user.username().contains(filterStr) ||
                            user.fullName().contains(filterStr) ||
                            user.email().contains(filterStr);

            List<User> users = system.getUserManager().findByFilter(filter);
            printUsersTable(users);
        });

        parser.registerCommand("user-create", "Create a new user", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());
            System.out.print("Enter full name: ");
            String fullName = ValidationUtils.normalizeString(scanner.nextLine());
            System.out.print("Enter email: ");
            String email = ValidationUtils.normalizeString(scanner.nextLine());

            try {
                User user = User.create(username, fullName, email);
                system.getUserManager().add(user);
                System.out.println("User created successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error creating user: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user info", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());

            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                System.out.println("--- User Info ---");
                System.out.println("Username: " + user.username());
                System.out.println("Full Name: " + user.fullName());
                System.out.println("Email: " + user.email());

                List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
                System.out.println("Roles:");
                if (assignments.isEmpty()) {
                    System.out.println("  None");
                } else {
                    assignments.stream()
                            .filter(RoleAssignment::isActive)
                            .forEach(a -> System.out.println("  - " + a.role().getName()));
                }

                Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user);
                System.out.println("Permissions:");
                if (permissions.isEmpty()) {
                    System.out.println("  None");
                } else {
                    permissions.forEach(p -> System.out.println("  - " + p.name() + ":" + p.resource()));
                }
            }, () -> System.out.println("User not found."));
        });

        parser.registerCommand("user-update", "Update user data", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());

            if (!system.getUserManager().exists(username)) {
                System.out.println("User not found.");
                return;
            }

            System.out.print("Enter new full name: ");
            String fullName = ValidationUtils.normalizeString(scanner.nextLine());
            System.out.print("Enter new email: ");
            String email = ValidationUtils.normalizeString(scanner.nextLine());

            try {
                system.getUserManager().update(username, fullName, email);
                System.out.println("User updated successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error updating user: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete a user", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());

            system.getUserManager().findByUsername(username).ifPresentOrElse(user -> {
                System.out.print("Are you sure? Type 'да' to confirm: ");
                String confirmation = ValidationUtils.normalizeString(scanner.nextLine());

                if ("да".equalsIgnoreCase(confirmation)) {
                    List<RoleAssignment> assignments = new ArrayList<>(system.getAssignmentManager().findByUser(user));
                    for (RoleAssignment assignment : assignments) {
                        system.getAssignmentManager().remove(assignment);
                    }
                    system.getUserManager().remove(user);
                    System.out.println("User and their assignments deleted successfully.");
                } else {
                    System.out.println("Deletion cancelled.");
                }
            }, () -> System.out.println("User not found."));
        });

        parser.registerCommand("user-search", "Search users by specific filters", (scanner, system) -> {
            System.out.println("Select filter:");
            System.out.println("1 - By username (contains)");
            System.out.println("2 - By email (contains)");
            System.out.println("3 - By email domain");
            System.out.println("4 - By full name (contains)");
            System.out.print("Choice: ");
            String choice = ValidationUtils.normalizeString(scanner.nextLine());

            System.out.print("Enter search string: ");
            String query = ValidationUtils.normalizeString(scanner.nextLine());

            UserFilter filter = switch (choice) {
                case "1" -> user -> user.username().contains(query);
                case "2" -> user -> user.email().contains(query);
                case "3" -> user -> user.email().endsWith(query);
                case "4" -> user -> user.fullName().contains(query);
                default -> null;
            };

            if (filter == null) {
                System.out.println("Invalid choice.");
                return;
            }

            List<User> users = system.getUserManager().findByFilter(filter);
            printUsersTable(users);
        });
    }

    public static void registerRoleCommands(CommandParser parser) {

        parser.registerCommand("role-list", "List all roles", (scanner, system) -> {
            List<Role> roles = system.getRoleManager().findAll();
            printRolesTable(roles);
        });

        parser.registerCommand("role-create", "Create a new role", (scanner, system) -> {
            System.out.print("Enter role name: ");
            String name = ValidationUtils.normalizeString(scanner.nextLine());
            System.out.print("Enter role description: ");
            String description = ValidationUtils.normalizeString(scanner.nextLine());

            Set<Permission> permissions = new HashSet<>();
            while (true) {
                System.out.print("Add a permission? (да/нет): ");
                String answer = ValidationUtils.normalizeString(scanner.nextLine()).toLowerCase();
                if (!answer.equals("да") && !answer.equals("yes") && !answer.equals("y") && !answer.equals("1")) {
                    break;
                }

                try {
                    System.out.print("  Permission name (e.g. READ): ");
                    String pName = ValidationUtils.normalizeString(scanner.nextLine());
                    System.out.print("  Resource (e.g. DATA): ");
                    String pResource = ValidationUtils.normalizeString(scanner.nextLine());
                    System.out.print("  Description: ");
                    String pDesc = ValidationUtils.normalizeString(scanner.nextLine());

                    permissions.add(new Permission(pName, pResource, pDesc));
                    System.out.println("  Permission added.");
                } catch (IllegalArgumentException e) {
                    System.out.println("  Error adding permission: " + e.getMessage());
                }
            }

            try {
                Role role = new Role(name, description, permissions);
                system.getRoleManager().add(role);
                System.out.println("Role created successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error creating role: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role info", (scanner, system) -> {
            System.out.print("Enter role name: ");
            String name = ValidationUtils.normalizeString(scanner.nextLine());

            system.getRoleManager().findByName(name).ifPresentOrElse(
                    role -> System.out.println(role.format()),
                    () -> System.out.println("Role not found.")
            );
        });

        parser.registerCommand("role-update", "Update a role (name/description)", (scanner, system) -> {
            System.out.print("Enter role name to update: ");
            String oldName = ValidationUtils.normalizeString(scanner.nextLine());

            system.getRoleManager().findByName(oldName).ifPresentOrElse(role -> {
                System.out.print("Enter new role name: ");
                String newName = ValidationUtils.normalizeString(scanner.nextLine());
                System.out.print("Enter new description: ");
                String newDesc = ValidationUtils.normalizeString(scanner.nextLine());

                if (!oldName.equalsIgnoreCase(newName) &&
                        system.getRoleManager().exists(newName)) {
                    System.out.println("Error: Role with this name already exists.");
                    return;
                }

                try {
                    Set<Permission> perms = new HashSet<>(role.getPermissions());

                    Role.clearUsedNames();
                    Role newRole = new Role(newName, newDesc, perms);

                    system.getRoleManager().add(newRole);

                    List<User> allUsers = system.getUserManager().findAll();
                    if (allUsers != null) {
                        for (User u : allUsers) {
                            List<RoleAssignment> assignments = new ArrayList<>(system.getAssignmentManager().findByUser(u));
                            for (RoleAssignment a : assignments) {
                                if (a.role().equals(role)) {
                                    system.getAssignmentManager().remove(a);
                                    system.getAssignmentManager().add(new PermanentAssignment(u, newRole, a.metadata()));
                                }
                            }
                        }
                    }

                    system.getRoleManager().remove(role);

                    System.out.println("Role updated successfully (migrated to new instance).");
                } catch (Exception e) {
                    System.out.println("Error updating role: " + e.getMessage());
                }
            }, () -> System.out.println("Role not found."));
        });

        parser.registerCommand("role-delete", "Delete a role", (scanner, system) -> {
            System.out.print("Enter role name: ");
            String name = ValidationUtils.normalizeString(scanner.nextLine());

            system.getRoleManager().findByName(name).ifPresentOrElse(role -> {
                List<User> assignedUsers = new ArrayList<>();
                List<User> allUsers = system.getUserManager().findAll();
                if (allUsers != null) {
                    for (User u : allUsers) {
                        if (system.getAssignmentManager().userHasRole(u, role)) {
                            assignedUsers.add(u);
                        }
                    }
                }

                if (!assignedUsers.isEmpty()) {
                    System.out.println("WARNING: This role is assigned to the following users:");
                    for (User u : assignedUsers) {
                        System.out.println("  - " + u.username());
                    }
                }

                System.out.print("Are you sure? Type 'да' to confirm: ");
                String confirmation = ValidationUtils.normalizeString(scanner.nextLine());

                if ("да".equalsIgnoreCase(confirmation)) {
                    for (User u : assignedUsers) {
                        List<RoleAssignment> assignments = new ArrayList<>(system.getAssignmentManager().findByUser(u));
                        for (RoleAssignment a : assignments) {
                            if (a.role().equals(role)) {
                                system.getAssignmentManager().remove(a);
                            }
                        }
                    }
                    try {
                        system.getRoleManager().remove(role);
                        System.out.println("Role deleted successfully.");
                    } catch (Exception e) {
                        System.out.println("Error deleting role: " + e.getMessage());
                    }
                } else {
                    System.out.println("Deletion cancelled.");
                }
            }, () -> System.out.println("Role not found."));
        });

        parser.registerCommand("role-add-permission", "Add a permission to a role", (scanner, system) -> {
            System.out.print("Enter role name: ");
            String name = ValidationUtils.normalizeString(scanner.nextLine());

            if (!system.getRoleManager().exists(name)) {
                System.out.println("Role not found.");
                return;
            }

            System.out.print("Enter permission name (e.g. READ): ");
            String pName = ValidationUtils.normalizeString(scanner.nextLine());
            System.out.print("Enter resource (e.g. DATA): ");
            String pResource = ValidationUtils.normalizeString(scanner.nextLine());
            System.out.print("Enter description: ");
            String pDesc = ValidationUtils.normalizeString(scanner.nextLine());

            try {
                Permission permission = new Permission(pName, pResource, pDesc);
                system.getRoleManager().addPermissionToRole(name, permission);
                System.out.println("Permission added successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error adding permission: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove a permission from a role", (scanner, system) -> {
            System.out.print("Enter role name: ");
            String name = ValidationUtils.normalizeString(scanner.nextLine());

            system.getRoleManager().findByName(name).ifPresentOrElse(role -> {
                List<Permission> perms = new ArrayList<>(role.getPermissions());
                if (perms.isEmpty()) {
                    System.out.println("Role has no permissions.");
                    return;
                }

                System.out.println("Permissions:");
                for (int i = 0; i < perms.size(); i++) {
                    System.out.println((i + 1) + ". " + perms.get(i).format());
                }

                System.out.print("Enter permission number to remove: ");
                try {
                    int index = Integer.parseInt(ValidationUtils.normalizeString(scanner.nextLine())) - 1;
                    if (index >= 0 && index < perms.size()) {
                        Permission toRemove = perms.get(index);
                        system.getRoleManager().removePermissionFromRole(name, toRemove);
                        System.out.println("Permission removed successfully.");
                    } else {
                        System.out.println("Invalid permission number.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number.");
                }
            }, () -> System.out.println("Role not found."));
        });

        parser.registerCommand("role-search", "Search roles by specific filters", (scanner, system) -> {
            System.out.println("Select filter:");
            System.out.println("1 - By name (contains)");
            System.out.println("2 - By specific permission (name)");
            System.out.println("3 - By minimum number of permissions");
            System.out.print("Choice: ");
            String choice = ValidationUtils.normalizeString(scanner.nextLine());

            List<Role> roles = system.getRoleManager().findAll();
            List<Role> result;

            switch (choice) {
                case "1" -> {
                    System.out.print("Enter name query: ");
                    String query = ValidationUtils.normalizeString(scanner.nextLine());
                    result = roles.stream().filter(r -> r.getName().contains(query)).toList();
                }
                case "2" -> {
                    System.out.print("Enter permission name (e.g. READ): ");
                    String query = ValidationUtils.normalizeString(scanner.nextLine()).toUpperCase();
                    result = roles.stream().filter(r -> r.getPermissions().stream()
                            .anyMatch(p -> p.name().contains(query))).toList();
                }
                case "3" -> {
                    System.out.print("Enter minimum number of permissions: ");
                    try {
                        int min = Integer.parseInt(ValidationUtils.normalizeString(scanner.nextLine()));
                        result = roles.stream().filter(r -> r.getPermissions().size() >= min).toList();
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number.");
                        return;
                    }
                }
                default -> {
                    System.out.println("Invalid choice.");
                    return;
                }
            }
            printRolesTable(result);
        });
    }

    public static void registerAssignmentCommands(CommandParser parser) {

        parser.registerCommand("assign-role", "Assign a role to a user", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());

            var userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();

            List<Role> roles = system.getRoleManager().findAll();
            if (roles.isEmpty()) {
                System.out.println("No roles available in the system.");
                return;
            }

            System.out.println("Available roles:");
            for (int i = 0; i < roles.size(); i++) {
                System.out.println((i + 1) + ". " + roles.get(i).getName());
            }

            System.out.print("Select role number: ");
            int roleIdx;
            try {
                roleIdx = Integer.parseInt(ValidationUtils.normalizeString(scanner.nextLine())) - 1;
                if (roleIdx < 0 || roleIdx >= roles.size()) throw new Exception();
            } catch (Exception e) {
                System.out.println("Invalid role selection.");
                return;
            }
            Role role = roles.get(roleIdx);

            System.out.print("Assignment type (1 - Permanent, 2 - Temporary): ");
            String typeChoice = ValidationUtils.normalizeString(scanner.nextLine());

            System.out.print("Enter reason for assignment: ");
            String reason = ValidationUtils.normalizeString(scanner.nextLine());
            AssignmentMetadata metadata = AssignmentMetadata.now("admin", reason);

            try {
                if ("2".equals(typeChoice)) {
                    System.out.print("Enter expiration date (YYYY-MM-DD): ");
                    String dateStr = ValidationUtils.normalizeString(scanner.nextLine());

                    if (!ValidationUtils.isValidDate(dateStr)) {
                        System.out.println("Invalid date format. Please use YYYY-MM-DD.");
                        return;
                    }

                    System.out.print("Auto-renew? (да/нет): ");
                    boolean autoRenew = ValidationUtils.normalizeString(scanner.nextLine()).equalsIgnoreCase("да");

                    String expiration = LocalDate.parse(dateStr).atTime(23, 59, 59).toString();
                    system.getAssignmentManager().add(new TemporaryAssignment(user, role, metadata, expiration, autoRenew));
                } else {
                    system.getAssignmentManager().add(new PermanentAssignment(user, role, metadata));
                }
                System.out.println("Role assigned successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke a role from a user", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());

            var userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(userOpt.get());
            if (assignments.isEmpty()) {
                System.out.println("User has no assignments.");
                return;
            }

            System.out.println("Active assignments:");
            for (int i = 0; i < assignments.size(); i++) {
                RoleAssignment a = assignments.get(i);
                String type = (a instanceof PermanentAssignment) ? "PERM" : "TEMP";
                System.out.printf("%d. [%s] %s (Status: %s)%n",
                        (i + 1), type, a.role().getName(), a.isActive() ? "ACTIVE" : "EXPIRED");
            }

            System.out.print("Select assignment number to revoke: ");
            try {
                int idx = Integer.parseInt(ValidationUtils.normalizeString(scanner.nextLine())) - 1;
                RoleAssignment toRevoke = assignments.get(idx);
                system.getAssignmentManager().remove(toRevoke);
                System.out.println("Assignment revoked successfully.");
            } catch (Exception e) {
                System.out.println("Invalid selection.");
            }
        });

        parser.registerCommand("assignment-list", "List all assignments in the system", (scanner, system) -> {
            List<User> allUsers = system.getUserManager().findAll();

            System.out.printf("%-15s | %-15s | %-6s | %-8s | %-20s%n",
                    "Username", "Role", "Type", "Status", "Assigned At");
            System.out.println("-".repeat(75));

            int count = 0;
            for (User u : allUsers) {
                List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(u);
                for (RoleAssignment a : assignments) {
                    String type = (a instanceof PermanentAssignment) ? "PERM" : "TEMP";
                    System.out.printf("%-15s | %-15s | %-6s | %-8s | %s%n",
                            u.username(),
                            a.role().getName(),
                            type,
                            a.isActive() ? "ACTIVE" : "EXPIRED",
                            a.metadata().assignedAt().toString().substring(0, 16).replace("T", " ")
                    );
                    count++;
                }
            }
            if (count == 0) {
                System.out.println("No assignments found.");
            }
        });

        parser.registerCommand("assignment-list-user", "List assignments for a specific user", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());
            system.getUserManager().findByUsername(username).ifPresentOrElse(u -> {
                List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(u);
                if (assignments.isEmpty()) {
                    System.out.println("No assignments found for user.");
                } else {
                    System.out.printf("%-15s | %-6s | %-8s | %-20s%n", "Role", "Type", "Status", "Assigned At");
                    System.out.println("-".repeat(58));
                    for (RoleAssignment a : assignments) {
                        String type = (a instanceof PermanentAssignment) ? "PERM" : "TEMP";
                        System.out.printf("%-15s | %-6s | %-8s | %s%n",
                                a.role().getName(),
                                type,
                                a.isActive() ? "ACTIVE" : "EXPIRED",
                                a.metadata().assignedAt().toString().substring(0, 16).replace("T", " ")
                        );
                    }
                }
            }, () -> System.out.println("User not found."));
        });

        parser.registerCommand("assignment-list-role", "List users with a specific role", (scanner, system) -> {
            System.out.print("Enter role name: ");
            String roleName = ValidationUtils.normalizeString(scanner.nextLine());
            system.getRoleManager().findByName(roleName).ifPresentOrElse(r -> {
                List<User> users = system.getUserManager().findAll();
                boolean found = false;
                for (User u : users) {
                    if (system.getAssignmentManager().userHasRole(u, r)) {
                        if (!found) {
                            System.out.printf("%-15s | %-25s%n", "Username", "Full Name");
                            System.out.println("-".repeat(43));
                            found = true;
                        }
                        System.out.printf("%-15s | %-25s%n", u.username(), u.fullName());
                    }
                }
                if (!found) {
                    System.out.println("No users found with this role.");
                }
            }, () -> System.out.println("Role not found."));
        });

        parser.registerCommand("assignment-active", "List all active assignments", (scanner, system) -> {
            List<User> allUsers = system.getUserManager().findAll();

            System.out.printf("%-15s | %-15s | %-6s | %-20s%n", "Username", "Role", "Type", "Assigned At");
            System.out.println("-".repeat(65));

            int count = 0;
            for (User u : allUsers) {
                for (RoleAssignment a : system.getAssignmentManager().findByUser(u)) {
                    if (a.isActive()) {
                        String type = (a instanceof PermanentAssignment) ? "PERM" : "TEMP";
                        System.out.printf("%-15s | %-15s | %-6s | %s%n",
                                u.username(),
                                a.role().getName(),
                                type,
                                a.metadata().assignedAt().toString().substring(0, 16).replace("T", " ")
                        );
                        count++;
                    }
                }
            }
            if (count == 0) {
                System.out.println("No active assignments found.");
            }
        });

        parser.registerCommand("assignment-expired", "List all expired temporary assignments", (scanner, system) -> {
            List<User> allUsers = system.getUserManager().findAll();
            System.out.printf("%-15s | %-15s | %-20s%n", "Username", "Role", "Expired At");
            System.out.println("-".repeat(56));

            int count = 0;
            for (User u : allUsers) {
                for (RoleAssignment a : system.getAssignmentManager().findByUser(u)) {
                    if (a instanceof TemporaryAssignment && !a.isActive()) {
                        System.out.printf("%-15s | %-15s | %s%n",
                                u.username(),
                                a.role().getName(),
                                ((TemporaryAssignment) a).getExpiresAt().replace("T", " ")
                        );
                        count++;
                    }
                }
            }
            if (count == 0) {
                System.out.println("No expired assignments found.");
            }
        });

        parser.registerCommand("assignment-extend", "Extend a temporary assignment", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());
            System.out.print("Enter role name: ");
            String roleName = ValidationUtils.normalizeString(scanner.nextLine());

            var userOpt = system.getUserManager().findByUsername(username);
            var roleOpt = system.getRoleManager().findByName(roleName);

            if (userOpt.isEmpty() || roleOpt.isEmpty()) {
                System.out.println("User or role not found.");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(userOpt.get());
            RoleAssignment target = null;
            for (RoleAssignment a : assignments) {
                if (a.role().equals(roleOpt.get())) {
                    target = a;
                    break;
                }
            }

            if (target == null) {
                System.out.println("Assignment not found.");
                return;
            }

            if (!(target instanceof TemporaryAssignment)) {
                System.out.println("Only temporary assignments can be extended.");
                return;
            }

            System.out.print("Enter new expiration date (YYYY-MM-DD): ");
            String dateStr = ValidationUtils.normalizeString(scanner.nextLine());

            if (!ValidationUtils.isValidDate(dateStr)) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
                return;
            }

            try {
                String newExp = LocalDate.parse(dateStr).atTime(23, 59, 59).toString();
                ((TemporaryAssignment) target).extend(newExp);
                System.out.println("Assignment extended successfully.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("assignment-search", "Search assignments by filters", (scanner, system) -> {
            System.out.println("Select filter:");
            System.out.println("1 - By user");
            System.out.println("2 - By role");
            System.out.println("3 - By type (PERM/TEMP)");
            System.out.println("4 - By status (ACTIVE/EXPIRED)");
            System.out.println("5 - Assigned after date");
            System.out.println("6 - Expires before date");
            System.out.print("Choice: ");
            String choice = ValidationUtils.normalizeString(scanner.nextLine());

            List<RoleAssignment> allAssignments = new ArrayList<>();
            List<User> allUsers = system.getUserManager().findAll();
            for (User u : allUsers) {
                allAssignments.addAll(system.getAssignmentManager().findByUser(u));
            }

            List<RoleAssignment> result = new ArrayList<>();
            switch (choice) {
                case "1" -> {
                    System.out.print("Enter username: ");
                    String un = ValidationUtils.normalizeString(scanner.nextLine());
                    result = allAssignments.stream().filter(a -> a.user().username().equals(un)).toList();
                }
                case "2" -> {
                    System.out.print("Enter role name: ");
                    String rn = ValidationUtils.normalizeString(scanner.nextLine());
                    result = allAssignments.stream().filter(a -> a.role().getName().equals(rn)).toList();
                }
                case "3" -> {
                    System.out.print("Enter type (PERM/TEMP): ");
                    String t = ValidationUtils.normalizeString(scanner.nextLine()).toUpperCase();
                    result = allAssignments.stream().filter(a -> {
                        String at = (a instanceof PermanentAssignment) ? "PERM" : "TEMP";
                        return at.equals(t);
                    }).toList();
                }
                case "4" -> {
                    System.out.print("Enter status (ACTIVE/EXPIRED): ");
                    String s = ValidationUtils.normalizeString(scanner.nextLine()).toUpperCase();
                    result = allAssignments.stream().filter(a -> {
                        String as = a.isActive() ? "ACTIVE" : "EXPIRED";
                        return as.equals(s);
                    }).toList();
                }
                case "5" -> {
                    System.out.print("Enter assigned after date (YYYY-MM-DD): ");
                    String inputDate = ValidationUtils.normalizeString(scanner.nextLine());
                    if (!ValidationUtils.isValidDate(inputDate)) {
                        System.out.println("Invalid date.");
                        return;
                    }
                    LocalDateTime dt = LocalDate.parse(inputDate).atStartOfDay();
                    result = allAssignments.stream().filter(a -> {
                        LocalDateTime assignedDt = LocalDateTime.parse(a.metadata().assignedAt().toString());
                        return assignedDt.isAfter(dt);
                    }).toList();
                }
                case "6" -> {
                    System.out.print("Enter expires before date (YYYY-MM-DD): ");
                    String inputDate = ValidationUtils.normalizeString(scanner.nextLine());
                    if (!ValidationUtils.isValidDate(inputDate)) {
                        System.out.println("Invalid date.");
                        return;
                    }
                    LocalDateTime dt = LocalDate.parse(inputDate).atTime(23, 59, 59);
                    result = allAssignments.stream().filter(a -> {
                        if (a instanceof TemporaryAssignment) {
                            LocalDateTime exp = LocalDateTime.parse(((TemporaryAssignment) a).getExpiresAt());
                            return exp.isBefore(dt);
                        }
                        return false;
                    }).toList();
                }
                default -> {
                    System.out.println("Invalid choice.");
                    return;
                }
            }

            System.out.printf("%-15s | %-15s | %-6s | %-8s | %-20s%n",
                    "Username", "Role", "Type", "Status", "Assigned At");
            System.out.println("-".repeat(75));
            if (result.isEmpty()) {
                System.out.println("No assignments found matching criteria.");
            } else {
                for (RoleAssignment a : result) {
                    String type = (a instanceof PermanentAssignment) ? "PERM" : "TEMP";
                    System.out.printf("%-15s | %-15s | %-6s | %-8s | %s%n",
                            a.user().username(),
                            a.role().getName(),
                            type,
                            a.isActive() ? "ACTIVE" : "EXPIRED",
                            a.metadata().assignedAt().toString().substring(0, 16).replace("T", " ")
                    );
                }
            }
        });
    }

    public static void registerPermissionCommands(CommandParser parser) {

        parser.registerCommand("permissions-user", "List all permissions of a user grouped by resource", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());

            system.getUserManager().findByUsername(username).ifPresentOrElse(u -> {
                Set<Permission> perms = system.getAssignmentManager().getUserPermissions(u);
                if (perms.isEmpty()) {
                    System.out.println("User has no permissions.");
                    return;
                }

                Map<String, List<Permission>> grouped = new HashMap<>();
                for (Permission p : perms) {
                    grouped.computeIfAbsent(p.resource(), k -> new ArrayList<>()).add(p);
                }

                for (Map.Entry<String, List<Permission>> entry : grouped.entrySet()) {
                    System.out.println("Resource: " + entry.getKey());
                    for (Permission p : entry.getValue()) {
                        System.out.println("  - " + p.name() + " (" + p.description() + ")");
                    }
                }
            }, () -> System.out.println("User not found."));
        });

        parser.registerCommand("permissions-check", "Check if user has specific permission", (scanner, system) -> {
            System.out.print("Enter username: ");
            String username = ValidationUtils.normalizeString(scanner.nextLine());

            system.getUserManager().findByUsername(username).ifPresentOrElse(u -> {
                System.out.print("Enter permission name: ");
                String pName = ValidationUtils.normalizeString(scanner.nextLine());
                System.out.print("Enter resource: ");
                String pRes = ValidationUtils.normalizeString(scanner.nextLine());

                boolean hasPerm = system.getAssignmentManager().userHasPermission(u, pName, pRes);

                if (hasPerm) {
                    List<String> providingRoles = new ArrayList<>();
                    for (RoleAssignment a : system.getAssignmentManager().findByUser(u)) {
                        if (a.isActive() && a.role().hasPermission(pName, pRes)) {
                            providingRoles.add(a.role().getName());
                        }
                    }
                    System.out.println("Access GRANTED.");
                    System.out.println("Provided by roles: " + String.join(", ", providingRoles));
                } else {
                    System.out.println("Access DENIED.");
                }
            }, () -> System.out.println("User not found."));
        });
    }

    public static void registerUtilityCommands(CommandParser parser) {
        parser.registerCommand("help", "Show all commands with descriptions", (scanner, system) -> {
            parser.printHelp();
        });

        parser.registerCommand("stats", "System statistics", (scanner, system) -> {
            try {
                System.out.println(system.getClass().getMethod("generateStatistics").invoke(system));
            } catch (Exception ex) {
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
                        if (a.isActive()) {
                            activeAssignments++;
                        } else {
                            expiredAssignments++;
                        }
                        rolePopularity.put(a.role(), rolePopularity.getOrDefault(a.role(), 0) + 1);
                    }
                }

                double avgRoles = userCount == 0 ? 0.0 : (double) totalAssignments / userCount;

                System.out.println("Users: " + userCount);
                System.out.println("Roles: " + roleCount);
                System.out.println("Assignments (Total/Active/Expired): " + totalAssignments + " / " + activeAssignments + " / " + expiredAssignments);
                System.out.printf("Avg roles per user: %.2f%n", avgRoles);
                System.out.println("Top-3 popular roles:");

                rolePopularity.entrySet().stream()
                        .sorted(Map.Entry.<Role, Integer>comparingByValue().reversed())
                        .limit(3)
                        .forEach(e -> System.out.println("  - " + e.getKey().getName() + " (" + e.getValue() + " assignments)"));
            }
        });

        parser.registerCommand("clear", "Clear the screen", (scanner, system) -> {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        });

        parser.registerCommand("save", "Save data to file", (scanner, system) -> {
            System.out.print("Enter filename (default: rbac.txt): ");
            String filename = ValidationUtils.normalizeString(scanner.nextLine());
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
                    for (Permission p : r.getPermissions()) {
                        perms.add(p.name() + "^" + p.resource() + "^" + p.description());
                    }
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
                System.out.println("Data saved successfully.");
            } catch (Exception e) {
                System.out.println("Error saving data: " + e.getMessage());
            }
        });

        parser.registerCommand("load", "Load data from file", (scanner, system) -> {
            System.out.print("Enter filename (default: rbac.txt): ");
            String filename = ValidationUtils.normalizeString(scanner.nextLine());
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
                            boolean exists = system.getAssignmentManager().userHasRole(u, r);
                            if (!exists) {
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
                System.out.println("Data loaded successfully.");
            } catch (Exception e) {
                System.out.println("Error loading data: " + e.getMessage());
            }
        });

        parser.registerCommand("exit", "Exit the program", (scanner, system) -> {
            System.out.print("Are you sure you want to exit? (да/нет): ");
            String confirmation = ValidationUtils.normalizeString(scanner.nextLine());
            if ("да".equalsIgnoreCase(confirmation)) {
                System.out.print("Save data before exiting? (да/нет): ");
                String saveConfirm = ValidationUtils.normalizeString(scanner.nextLine());
                if ("да".equalsIgnoreCase(saveConfirm)) {
                    try {
                        parser.executeCommand("save", scanner, system);
                    } catch (Exception e) {
                    }
                }
                System.out.println("Exiting...");
                throw new RuntimeException("EXIT_SIGNAL");
            } else {
                System.out.println("Exit cancelled.");
            }
        });
    }

    private static void printUsersTable(List<User> users) {
        System.out.printf("%-15s | %-25s | %-25s%n", "Username", "Full Name", "Email");
        System.out.println("-".repeat(71));

        if (users.isEmpty()) {
            System.out.println("No users found.");
        } else {
            for (User user : users) {
                System.out.printf("%-15s | %-25s | %-25s%n", user.username(), user.fullName(), user.email());
            }
        }
    }

    private static void printRolesTable(List<Role> roles) {
        System.out.printf("%-20s | %-15s | %s%n", "Name", "Permissions", "ID");
        System.out.println("-".repeat(73));

        if (roles.isEmpty()) {
            System.out.println("No roles found.");
        } else {
            for (Role role : roles) {
                System.out.printf("%-20s | %-15d | %s%n",
                        role.getName(),
                        role.getPermissions().size(),
                        role.getId());
            }
        }
    }
}