package org.example.rbac;

import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;
import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.PermanentAssignment;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.User;

import java.util.HashSet;
import java.util.Set;

public class RBACSystem {

    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(this.userManager, this.roleManager);
        this.currentUser = "system";
    }

    public RBACSystem(UserManager userManager, RoleManager roleManager, AssignmentManager assignmentManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
        this.assignmentManager = assignmentManager;
        this.currentUser = "system";
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void initialize() {
        Permission readPerm = new Permission("READ", "DATA", "Read data");
        Permission writePerm = new Permission("WRITE", "DATA", "Write data");
        Permission deletePerm = new Permission("DELETE", "DATA", "Delete data");

        Set<Permission> adminPerms = new HashSet<>(Set.of(readPerm, writePerm, deletePerm));
        Set<Permission> managerPerms = new HashSet<>(Set.of(readPerm, writePerm));
        Set<Permission> viewerPerms = new HashSet<>(Set.of(readPerm));

        Role adminRole = new Role("Admin", "Administrator", adminPerms);
        Role managerRole = new Role("Manager", "Manager", managerPerms);
        Role viewerRole = new Role("Viewer", "Viewer", viewerPerms);

        if (!roleManager.exists(adminRole.getName())) roleManager.add(adminRole);
        if (!roleManager.exists(managerRole.getName())) roleManager.add(managerRole);
        if (!roleManager.exists(viewerRole.getName())) roleManager.add(viewerRole);

        User adminUser;
        if (!userManager.exists("admin")) {
            adminUser = User.create("admin", "System Administrator", "admin@system.local");
            userManager.add(adminUser);
        } else {
            adminUser = userManager.findByUsername("admin").get();
        }

        if (!assignmentManager.userHasRole(adminUser, adminRole)) {
            AssignmentMetadata meta = AssignmentMetadata.now(currentUser, "System Initialization");
            PermanentAssignment assignment = new PermanentAssignment(adminUser, adminRole, meta);
            assignmentManager.add(assignment);
        }
    }

    public String generateStatistics() {
        return String.format(
                "--- RBAC System Statistics ---\nUsers: %d\nRoles: %d\nAssignments: %d\n------------------------------",
                userManager.count(),
                roleManager.count(),
                assignmentManager.count()
        );
    }
}