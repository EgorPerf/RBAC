package org.example.rbac.report;

import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.RoleAssignment;
import org.example.rbac.model.User;
import org.example.rbac.util.FormatUtils;
import org.example.rbac.util.ValidationUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        String[] headers = {"Username", "Email", "Active Roles"};
        List<String[]> rows = userManager.findAll().parallelStream()
                .map(u -> {
                    List<String> roles = assignmentManager.findByUser(u).stream()
                            .filter(RoleAssignment::isActive)
                            .map(a -> a.role().getName())
                            .toList();
                    String rolesStr = roles.isEmpty() ? "None" : String.join(", ", roles);
                    return new String[]{u.username(), u.email(), rolesStr};
                })
                .collect(Collectors.toList());
        return FormatUtils.formatTable(headers, rows) + "\n";
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        String[] headers = {"Role Name", "Description", "Active Users Count"};
        List<String[]> rows = roleManager.findAll().stream()
                .map(r -> {
                    long count = assignmentManager.findByRole(r).stream().filter(RoleAssignment::isActive).count();
                    return new String[]{r.getName(), FormatUtils.truncate(r.getDescription(), 30), String.valueOf(count)};
                })
                .collect(Collectors.toList());
        return FormatUtils.formatTable(headers, rows) + "\n";
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        Set<String> resources = userManager.findAll().parallelStream()
                .flatMap(u -> assignmentManager.getUserPermissions(u).stream())
                .map(Permission::resource)
                .collect(Collectors.toCollection(TreeSet::new));

        if (resources.isEmpty()) {
            return "No permissions assigned to any user.\n";
        }

        List<String> resList = new ArrayList<>(resources);
        String[] headers = new String[resList.size() + 1];
        headers[0] = "User \\ Resource";
        for (int i = 0; i < resList.size(); i++) headers[i + 1] = resList.get(i);

        List<String[]> rows = userManager.findAll().parallelStream()
                .map(u -> {
                    String[] row = new String[headers.length];
                    row[0] = u.username();
                    Set<Permission> perms = assignmentManager.getUserPermissions(u);

                    for (int i = 0; i < resList.size(); i++) {
                        String res = resList.get(i);
                        String actions = perms.stream()
                                .filter(p -> p.resource().equals(res))
                                .map(Permission::name)
                                .sorted()
                                .collect(Collectors.joining(","));
                        row[i + 1] = actions.isEmpty() ? "-" : actions;
                    }
                    return row;
                })
                .collect(Collectors.toList());

        return FormatUtils.formatTable(headers, rows) + "\n";
    }

    public void exportToFile(String report, String filename) {
        ValidationUtils.requireNonEmpty(filename, "Filename");
        try (PrintWriter writer = new PrintWriter(ValidationUtils.normalizeString(filename))) {
            writer.print(report);
        } catch (Exception e) {
            throw new RuntimeException("Error saving report: " + e.getMessage(), e);
        }
    }
}