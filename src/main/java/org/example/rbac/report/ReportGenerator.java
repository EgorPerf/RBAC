package org.example.rbac.report;

import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.RoleAssignment;
import org.example.rbac.model.User;
import org.example.rbac.util.ValidationUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s | %-25s | %s%n", "Username", "Email", "Active Roles"));
        sb.append("-".repeat(80)).append("\n");

        for (User u : userManager.findAll()) {
            List<String> roles = assignmentManager.findByUser(u).stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.role().getName())
                    .toList();
            String rolesStr = roles.isEmpty() ? "None" : String.join(", ", roles);
            sb.append(String.format("%-20s | %-25s | %s%n", u.username(), u.email(), rolesStr));
        }
        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s | %-30s | %s%n", "Role Name", "Description", "Active Users Count"));
        sb.append("-".repeat(75)).append("\n");

        for (Role r : roleManager.findAll()) {
            long count = assignmentManager.findByRole(r).stream()
                    .filter(RoleAssignment::isActive)
                    .count();
            sb.append(String.format("%-20s | %-30s | %d%n", r.getName(), r.getDescription(), count));
        }
        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        Set<String> resources = new TreeSet<>();

        for (User u : userManager.findAll()) {
            assignmentManager.getUserPermissions(u).forEach(p -> resources.add(p.resource()));
        }

        if (resources.isEmpty()) {
            return "No permissions assigned to any user.\n";
        }

        List<String> resList = new ArrayList<>(resources);

        sb.append(String.format("%-20s", "User \\ Resource"));
        for (String res : resList) {
            sb.append(String.format(" | %-25s", res));
        }
        sb.append("\n").append("-".repeat(20 + resList.size() * 28)).append("\n");

        for (User u : userManager.findAll()) {
            sb.append(String.format("%-20s", u.username()));
            Set<Permission> perms = assignmentManager.getUserPermissions(u);

            for (String res : resList) {
                String actions = perms.stream()
                        .filter(p -> p.resource().equals(res))
                        .map(Permission::name)
                        .sorted()
                        .collect(Collectors.joining(","));
                if (actions.isEmpty()) {
                    actions = "-";
                }
                sb.append(String.format(" | %-25s", actions));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        ValidationUtils.requireNonEmpty(filename, "Filename");
        String normFilename = ValidationUtils.normalizeString(filename);
        try (PrintWriter writer = new PrintWriter(normFilename)) {
            writer.print(report);
        } catch (Exception e) {
            throw new RuntimeException("Error saving report: " + e.getMessage(), e);
        }
    }
}