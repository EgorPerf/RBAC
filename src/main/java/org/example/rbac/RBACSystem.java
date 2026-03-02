package org.example.rbac;

import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;

    public RBACSystem(UserManager userManager, RoleManager roleManager, AssignmentManager assignmentManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
        this.assignmentManager = assignmentManager;
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
}