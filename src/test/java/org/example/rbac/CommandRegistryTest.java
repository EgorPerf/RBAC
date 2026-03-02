package org.example.rbac;

import org.example.rbac.command.CommandParser;
import org.example.rbac.command.CommandRegistry;
import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.PermanentAssignment;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.RoleAssignment;
import org.example.rbac.model.TemporaryAssignment;
import org.example.rbac.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    private CommandParser parser;
    private RBACSystem system;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
        parser = new CommandParser();
        system = new RBACSystem();
        CommandRegistry.registerUserCommands(parser);
        CommandRegistry.registerRoleCommands(parser);
        CommandRegistry.registerAssignmentCommands(parser);
        CommandRegistry.registerPermissionCommands(parser);
        CommandRegistry.registerUtilityCommands(parser);
    }

    @Test
    @DisplayName("user-list: вывод всех пользователей")
    void testUserListAll() {
        system.getUserManager().add(User.create("test1", "Test One", "t1@test.com"));
        system.getUserManager().add(User.create("test2", "Test Two", "t2@test.com"));

        Scanner scanner1 = new Scanner("\n");
        String out1 = captureOutput(() -> parser.executeCommand("user-list", scanner1, system));
        assertTrue(out1.contains("test1"));
        assertTrue(out1.contains("test2"));
    }

    @Test
    @DisplayName("user-list: с фильтром")
    void testUserListFilter() {
        system.getUserManager().add(User.create("test1", "Test One", "t1@test.com"));
        system.getUserManager().add(User.create("test2", "Test Two", "t2@test.com"));

        Scanner scanner2 = new Scanner("One\n");
        String out2 = captureOutput(() -> parser.executeCommand("user-list", scanner2, system));
        assertTrue(out2.contains("test1"));
        assertFalse(out2.contains("test2"));
    }

    @Test
    @DisplayName("user-list: пустой результат")
    void testUserListEmpty() {
        Scanner scanner3 = new Scanner("xyz\n");
        String out3 = captureOutput(() -> parser.executeCommand("user-list", scanner3, system));
        assertTrue(out3.contains("No users found."));
    }

    @Test
    @DisplayName("user-create: успешное создание")
    void testUserCreateSuccess() {
        Scanner scanner1 = new Scanner("newuser\nNew User\nnew@user.com\n");
        String out1 = captureOutput(() -> parser.executeCommand("user-create", scanner1, system));
        assertTrue(out1.contains("User created successfully."));
        assertTrue(system.getUserManager().exists("newuser"));
    }

    @Test
    @DisplayName("user-create: ошибка валидации")
    void testUserCreateFail() {
        Scanner scanner2 = new Scanner("newuser\nAnother\nbad\n");
        String out2 = captureOutput(() -> parser.executeCommand("user-create", scanner2, system));
        assertTrue(out2.contains("Error creating user"));
    }

    @Test
    @DisplayName("user-view: успешный просмотр")
    void testUserViewSuccess() {
        User user = User.create("viewuser", "View User", "v@v.com");
        system.getUserManager().add(user);

        Role role = new Role("ViewRole", "Desc", new HashSet<>(Set.of(new Permission("READ", "VIEW", "Desc"))));
        system.getRoleManager().add(role);

        system.getAssignmentManager().add(new PermanentAssignment(user, role, AssignmentMetadata.now("sys", "test")));

        Scanner scanner1 = new Scanner("viewuser\n");
        String out1 = captureOutput(() -> parser.executeCommand("user-view", scanner1, system));
        assertTrue(out1.contains("viewuser"));
        assertTrue(out1.contains("View User"));
        assertTrue(out1.contains("ViewRole"));
        assertTrue(out1.contains("READ:view"));
    }

    @Test
    @DisplayName("user-view: неизвестный пользователь")
    void testUserViewUnknown() {
        Scanner scanner2 = new Scanner("unknown\n");
        String out2 = captureOutput(() -> parser.executeCommand("user-view", scanner2, system));
        assertTrue(out2.contains("User not found."));
    }

    @Test
    @DisplayName("user-view: пользователь без ролей")
    void testUserViewNoRoles() {
        User userNoRoles = User.create("noroles", "No Roles", "n@n.com");
        system.getUserManager().add(userNoRoles);
        Scanner scanner3 = new Scanner("noroles\n");
        String out3 = captureOutput(() -> parser.executeCommand("user-view", scanner3, system));
        assertTrue(out3.contains("None"));
    }

    @Test
    @DisplayName("user-update: успешное обновление")
    void testUserUpdateSuccess() {
        system.getUserManager().add(User.create("upduser", "Old Name", "old@test.com"));

        Scanner scanner1 = new Scanner("upduser\nNew Name\nnew@test.com\n");
        String out1 = captureOutput(() -> parser.executeCommand("user-update", scanner1, system));
        assertTrue(out1.contains("User updated successfully."));
        assertEquals("New Name", system.getUserManager().findByUsername("upduser").get().fullName());
    }

    @Test
    @DisplayName("user-update: неизвестный пользователь")
    void testUserUpdateUnknown() {
        Scanner scanner2 = new Scanner("unknown\nNew Name\nnew@test.com\n");
        String out2 = captureOutput(() -> parser.executeCommand("user-update", scanner2, system));
        assertTrue(out2.contains("User not found."));
    }

    @Test
    @DisplayName("user-update: ошибка валидации")
    void testUserUpdateFail() {
        system.getUserManager().add(User.create("upduser2", "Old Name", "old@test.com"));
        Scanner scanner3 = new Scanner("upduser2\nBad Name\nbad_email\n");
        String out3 = captureOutput(() -> parser.executeCommand("user-update", scanner3, system));
        assertTrue(out3.contains("Error updating user"));
    }

    @Test
    @DisplayName("user-delete: отмена удаления")
    void testUserDeleteCancel() {
        User user = User.create("deluser", "Del User", "del@test.com");
        system.getUserManager().add(user);

        Scanner scanner1 = new Scanner("deluser\nнет\n");
        String out1 = captureOutput(() -> parser.executeCommand("user-delete", scanner1, system));
        assertTrue(out1.contains("Deletion cancelled."));
        assertTrue(system.getUserManager().exists("deluser"));
    }

    @Test
    @DisplayName("user-delete: успешное удаление")
    void testUserDeleteSuccess() {
        User user = User.create("deluser2", "Del User", "del@test.com");
        system.getUserManager().add(user);

        Scanner scanner2 = new Scanner("deluser2\nда\n");
        String out2 = captureOutput(() -> parser.executeCommand("user-delete", scanner2, system));
        assertTrue(out2.contains("deleted successfully."));
        assertFalse(system.getUserManager().exists("deluser2"));
    }

    @Test
    @DisplayName("user-delete: неизвестный пользователь")
    void testUserDeleteUnknown() {
        Scanner scanner3 = new Scanner("unknown\nда\n");
        String out3 = captureOutput(() -> parser.executeCommand("user-delete", scanner3, system));
        assertTrue(out3.contains("User not found."));
    }

    @Test
    @DisplayName("user-search: все варианты фильтров")
    void testUserSearchAllOptions() {
        system.getUserManager().add(User.create("alpha", "Alpha User", "alpha@gmail.com"));

        Scanner sc1 = new Scanner("1\nalph\n");
        assertTrue(captureOutput(() -> parser.executeCommand("user-search", sc1, system)).contains("alpha"));

        Scanner sc2 = new Scanner("2\ngmail\n");
        assertTrue(captureOutput(() -> parser.executeCommand("user-search", sc2, system)).contains("alpha"));

        Scanner sc3 = new Scanner("3\ngmail.com\n");
        assertTrue(captureOutput(() -> parser.executeCommand("user-search", sc3, system)).contains("alpha"));

        Scanner sc4 = new Scanner("4\nAlpha\n");
        assertTrue(captureOutput(() -> parser.executeCommand("user-search", sc4, system)).contains("alpha"));

        Scanner scBad = new Scanner("99\nquery\n");
        assertTrue(captureOutput(() -> parser.executeCommand("user-search", scBad, system)).contains("Invalid choice."));
    }

    @Test
    @DisplayName("role-list: непустой список")
    void testRoleListNotEmpty() {
        system.getRoleManager().add(new Role("TestRoleA", "DescA", new HashSet<>()));
        system.getRoleManager().add(new Role("TestRoleB", "DescB", new HashSet<>(Set.of(new Permission("READ", "DATA", "Desc")))));

        Scanner scanner = new Scanner("\n");
        String out = captureOutput(() -> parser.executeCommand("role-list", scanner, system));

        assertTrue(out.contains("TestRoleA"));
        assertTrue(out.contains("TestRoleB"));
    }

    @Test
    @DisplayName("role-list: пустой список")
    void testRoleListEmpty() {
        Scanner scanner = new Scanner("\n");
        String out = captureOutput(() -> parser.executeCommand("role-list", scanner, system));
        assertTrue(out.contains("No roles found."));
    }

    @Test
    @DisplayName("role-create: успешное создание")
    void testRoleCreateSuccess() {
        Scanner scanner = new Scanner("NewRole\nNew Desc\nда\nWRITE\nDATA\nWrite data\nнет\n");
        String out = captureOutput(() -> parser.executeCommand("role-create", scanner, system));

        assertTrue(out.contains("Role created successfully."));
        assertTrue(system.getRoleManager().exists("NewRole"));
        assertEquals(1, system.getRoleManager().findByName("NewRole").get().getPermissions().size());
    }

    @Test
    @DisplayName("role-create: ошибка создания")
    void testRoleCreateFail() {
        Scanner scannerPermError = new Scanner("ErrRole\nDesc\nда\n\n\n\nнет\n");
        String outPermError = captureOutput(() -> parser.executeCommand("role-create", scannerPermError, system));
        assertTrue(outPermError.contains("Error adding permission:"));

        system.getRoleManager().add(new Role("DupRole", "Desc", new HashSet<>()));
        Scanner scannerRoleError = new Scanner("DupRole\nDesc\nнет\n");
        String outRoleError = captureOutput(() -> parser.executeCommand("role-create", scannerRoleError, system));
        assertTrue(outRoleError.contains("Error creating role:"));
    }

    @Test
    @DisplayName("role-view: успешный просмотр")
    void testRoleViewSuccess() {
        Role role = new Role("ViewRoleFormat", "Desc", new HashSet<>());
        system.getRoleManager().add(role);

        Scanner scanner1 = new Scanner("ViewRoleFormat\n");
        String out1 = captureOutput(() -> parser.executeCommand("role-view", scanner1, system));
        assertTrue(out1.contains(role.format()) || out1.contains("ViewRoleFormat"));
    }

    @Test
    @DisplayName("role-view: неизвестная роль")
    void testRoleViewUnknown() {
        Scanner scanner2 = new Scanner("UnknownRole\n");
        String out2 = captureOutput(() -> parser.executeCommand("role-view", scanner2, system));
        assertTrue(out2.contains("Role not found."));
    }

    @Test
    @DisplayName("role-update: успешное обновление")
    void testRoleUpdateSuccess() {
        Role role = new Role("UpdRole", "Old", new HashSet<>());
        system.getRoleManager().add(role);
        User u = User.create("user1", "User One", "user1@test.com");
        system.getUserManager().add(u);
        system.getAssignmentManager().add(new PermanentAssignment(u, role, AssignmentMetadata.now("sys", "test")));

        Scanner scanner = new Scanner("UpdRole\nNewRoleMigrated\nNewDesc\n");
        String out = captureOutput(() -> parser.executeCommand("role-update", scanner, system));

        assertTrue(out.contains("Role updated successfully"));
        assertTrue(system.getRoleManager().exists("NewRoleMigrated"));
        assertFalse(system.getRoleManager().exists("UpdRole"));

        Role newRoleObj = system.getRoleManager().findByName("NewRoleMigrated").get();
        assertTrue(system.getAssignmentManager().userHasRole(u, newRoleObj));
    }

    @Test
    @DisplayName("role-update: ошибки")
    void testRoleUpdateFails() {
        Scanner scannerUnknown = new Scanner("unknown\nnew\ndesc\n");
        String outUnknown = captureOutput(() -> parser.executeCommand("role-update", scannerUnknown, system));
        assertTrue(outUnknown.contains("Role not found."));

        system.getRoleManager().add(new Role("Upd1", "D1", new HashSet<>()));
        system.getRoleManager().add(new Role("Upd2", "D2", new HashSet<>()));

        Scanner scannerConflict = new Scanner("Upd1\nUpd2\nnewdesc\n");
        String outConflict = captureOutput(() -> parser.executeCommand("role-update", scannerConflict, system));
        assertTrue(outConflict.contains("already exists."));
    }

    @Test
    @DisplayName("role-delete: отмена")
    void testRoleDeleteCancel() {
        Role role = new Role("DelRole2", "Old", new HashSet<>());
        system.getRoleManager().add(role);
        User u = User.create("user2", "User Two", "user2@test.com");
        system.getUserManager().add(u);
        system.getAssignmentManager().add(new PermanentAssignment(u, role, AssignmentMetadata.now("sys", "test")));

        Scanner scanner1 = new Scanner("DelRole2\nнет\n");
        String out1 = captureOutput(() -> parser.executeCommand("role-delete", scanner1, system));
        assertTrue(out1.contains("WARNING: This role is assigned"));
        assertTrue(out1.contains("Deletion cancelled."));
        assertTrue(system.getRoleManager().exists("DelRole2"));
    }

    @Test
    @DisplayName("role-delete: успех")
    void testRoleDeleteSuccess() {
        Role role = new Role("DelRole3", "Old", new HashSet<>());
        system.getRoleManager().add(role);

        Scanner scanner2 = new Scanner("DelRole3\nда\n");
        String out2 = captureOutput(() -> parser.executeCommand("role-delete", scanner2, system));
        assertTrue(out2.contains("Role deleted successfully."));
        assertFalse(system.getRoleManager().exists("DelRole3"));
    }

    @Test
    @DisplayName("role-delete: неизвестная роль")
    void testRoleDeleteUnknown() {
        Scanner scannerUnknown = new Scanner("unknown\nда\n");
        String outUnknown = captureOutput(() -> parser.executeCommand("role-delete", scannerUnknown, system));
        assertTrue(outUnknown.contains("Role not found."));
    }

    @Test
    @DisplayName("role-add-permission: успешное добавление")
    void testRoleAddPermission() {
        Role role = new Role("PermRole", "Desc", new HashSet<>());
        system.getRoleManager().add(role);

        Scanner scanner = new Scanner("PermRole\nEXECUTE\nSCRIPT\nRun scripts\n");
        String out = captureOutput(() -> parser.executeCommand("role-add-permission", scanner, system));

        assertTrue(out.contains("Permission added successfully."));
        assertTrue(system.getRoleManager().findByName("PermRole").get().hasPermission("EXECUTE", "script"));
    }

    @Test
    @DisplayName("role-add-permission: ошибки")
    void testRoleAddPermissionFail() {
        Scanner scannerAddUnk = new Scanner("unknown\nREAD\nDATA\nDesc\n");
        assertTrue(captureOutput(() -> parser.executeCommand("role-add-permission", scannerAddUnk, system)).contains("Role not found."));

        system.getRoleManager().add(new Role("PermR", "D", new HashSet<>()));
        Scanner scannerAddBad = new Scanner("PermR\n\n\n\n");
        assertTrue(captureOutput(() -> parser.executeCommand("role-add-permission", scannerAddBad, system)).contains("Error adding permission:"));
    }

    @Test
    @DisplayName("role-remove-permission: успешное удаление")
    void testRoleRemovePermission() {
        Role role = new Role("RemPermRole", "Desc", new HashSet<>(Set.of(new Permission("READ", "DATA", "desc"))));
        system.getRoleManager().add(role);

        Scanner scanner = new Scanner("RemPermRole\n1\n");
        String out = captureOutput(() -> parser.executeCommand("role-remove-permission", scanner, system));
        assertTrue(out.contains("Permission removed successfully."));
        assertEquals(0, system.getRoleManager().findByName("RemPermRole").get().getPermissions().size());
    }

    @Test
    @DisplayName("role-remove-permission: ошибки")
    void testRoleRemovePermissionFail() {
        Scanner scanner2 = new Scanner("UnknownRole\n1\n");
        String out2 = captureOutput(() -> parser.executeCommand("role-remove-permission", scanner2, system));
        assertTrue(out2.contains("Role not found."));

        system.getRoleManager().add(new Role("PermR", "D", new HashSet<>()));
        Scanner scannerRemEmpty = new Scanner("PermR\n");
        assertTrue(captureOutput(() -> parser.executeCommand("role-remove-permission", scannerRemEmpty, system)).contains("Role has no permissions."));

        system.getRoleManager().add(new Role("PermR2", "D", new HashSet<>(Set.of(new Permission("R", "d", "d")))));
        Scanner scannerRemBadInput = new Scanner("PermR2\nabc\n");
        assertTrue(captureOutput(() -> parser.executeCommand("role-remove-permission", scannerRemBadInput, system)).contains("Invalid input."));

        Scanner scannerRemBadNum = new Scanner("PermR2\n99\n");
        assertTrue(captureOutput(() -> parser.executeCommand("role-remove-permission", scannerRemBadNum, system)).contains("Invalid permission number."));
    }

    @Test
    @DisplayName("role-search: поиск по имени, правам и количеству")
    void testRoleSearch() {
        system.getRoleManager().add(new Role("SearchRoleA", "Desc", new HashSet<>(Set.of(new Permission("READ", "DATA", "desc")))));
        system.getRoleManager().add(new Role("SearchRoleB", "Desc", new HashSet<>(Set.of(new Permission("WRITE", "DATA", "desc"), new Permission("EXECUTE", "DATA", "desc")))));

        Scanner scanner1 = new Scanner("1\nRoleA\n");
        String out1 = captureOutput(() -> parser.executeCommand("role-search", scanner1, system));
        assertTrue(out1.contains("SearchRoleA"));

        Scanner scanner2 = new Scanner("2\nWRITE\n");
        String out2 = captureOutput(() -> parser.executeCommand("role-search", scanner2, system));
        assertTrue(out2.contains("SearchRoleB"));

        Scanner scanner3 = new Scanner("3\n2\n");
        String out3 = captureOutput(() -> parser.executeCommand("role-search", scanner3, system));
        assertTrue(out3.contains("SearchRoleB"));

        Scanner scBadNum = new Scanner("3\nabc\n");
        assertTrue(captureOutput(() -> parser.executeCommand("role-search", scBadNum, system)).contains("Invalid number."));

        Scanner scBadChoice = new Scanner("99\n");
        assertTrue(captureOutput(() -> parser.executeCommand("role-search", scBadChoice, system)).contains("Invalid choice."));
    }

    @Test
    @DisplayName("assign-role: успешное создание постоянного назначения")
    void testAssignRolePermanent() {
        User u = User.create("assignee", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("Manager", "Desc", new HashSet<>());
        system.getRoleManager().add(r);

        Scanner scanner = new Scanner("assignee\n1\n1\nTesting permanent\n");
        String out = captureOutput(() -> parser.executeCommand("assign-role", scanner, system));

        assertTrue(out.contains("assigned successfully"));
        assertTrue(system.getAssignmentManager().userHasRole(u, r));
    }

    @Test
    @DisplayName("assign-role: ошибки")
    void testAssignRoleFails() {
        Scanner scUnkUser = new Scanner("unk\n");
        assertTrue(captureOutput(() -> parser.executeCommand("assign-role", scUnkUser, system)).contains("User not found."));

        system.getUserManager().add(User.create("usr1", "User One", "u@u.com"));
        Scanner scNoRoles = new Scanner("usr1\n");
        assertTrue(captureOutput(() -> parser.executeCommand("assign-role", scNoRoles, system)).contains("No roles available"));

        system.getRoleManager().add(new Role("RoleR", "D", new HashSet<>()));
        Scanner scBadIdx = new Scanner("usr1\n99\n");
        assertTrue(captureOutput(() -> parser.executeCommand("assign-role", scBadIdx, system)).contains("Invalid role selection."));

        Scanner scTempBadDate = new Scanner("usr1\n1\n2\nreason\nbad_date\nда\n");
        assertTrue(captureOutput(() -> parser.executeCommand("assign-role", scTempBadDate, system)).contains("Invalid date format."));
    }

    @Test
    @DisplayName("revoke-role: успешный отзыв назначения")
    void testRevokeRole() {
        User u = User.create("revoker", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("RevokeRole", "Desc", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("admin", "test")));

        Scanner scanner = new Scanner("revoker\n1\n");
        String out = captureOutput(() -> parser.executeCommand("revoke-role", scanner, system));

        assertTrue(out.contains("revoked successfully"));
        assertFalse(system.getAssignmentManager().userHasRole(u, r));
    }

    @Test
    @DisplayName("revoke-role: ошибки")
    void testRevokeRoleFails() {
        Scanner scUnk = new Scanner("unk\n");
        assertTrue(captureOutput(() -> parser.executeCommand("revoke-role", scUnk, system)).contains("User not found."));

        system.getUserManager().add(User.create("usr1", "User One", "u@u.com"));
        Scanner scNoAss = new Scanner("usr1\n");
        assertTrue(captureOutput(() -> parser.executeCommand("revoke-role", scNoAss, system)).contains("User has no assignments."));

        Role r = new Role("RoleR", "D", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(system.getUserManager().findByUsername("usr1").get(), r, AssignmentMetadata.now("s", "s")));
        Scanner scBadIdx = new Scanner("usr1\n99\n");
        assertTrue(captureOutput(() -> parser.executeCommand("revoke-role", scBadIdx, system)).contains("Invalid selection."));
    }

    @Test
    @DisplayName("assignment-list: проверка табличного вывода")
    void testAssignmentList() {
        User u = User.create("listuser", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("ListRole", "Desc", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("admin", "test")));

        String out = captureOutput(() -> parser.executeCommand("assignment-list", new Scanner("\n"), system));

        assertTrue(out.contains("listuser"));
        assertTrue(out.contains("ListRole"));
        assertTrue(out.contains("PERM"));
    }

    @Test
    @DisplayName("assignment-list-user: список назначений пользователя")
    void testAssignmentListUser() {
        User u = User.create("usr1", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("Role1", "Desc", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("admin", "test")));

        Scanner scanner1 = new Scanner("usr1\n");
        String out1 = captureOutput(() -> parser.executeCommand("assignment-list-user", scanner1, system));
        assertTrue(out1.contains("Role1"));
        assertTrue(out1.contains("PERM"));

        Scanner scanner2 = new Scanner("unknown\n");
        String out2 = captureOutput(() -> parser.executeCommand("assignment-list-user", scanner2, system));
        assertTrue(out2.contains("User not found."));
    }

    @Test
    @DisplayName("assignment-list-role: список пользователей с ролью")
    void testAssignmentListRole() {
        User u = User.create("usr2", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("Role2", "Desc", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("admin", "test")));

        Scanner scanner1 = new Scanner("Role2\n");
        String out1 = captureOutput(() -> parser.executeCommand("assignment-list-role", scanner1, system));
        assertTrue(out1.contains("usr2"));

        Scanner scanner2 = new Scanner("UnknownRole\n");
        String out2 = captureOutput(() -> parser.executeCommand("assignment-list-role", scanner2, system));
        assertTrue(out2.contains("Role not found."));
    }

    @Test
    @DisplayName("assignment-active: список активных назначений")
    void testAssignmentActive() {
        User u = User.create("usr3", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("Role3", "Desc", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("admin", "test")));

        Scanner scanner = new Scanner("\n");
        String out = captureOutput(() -> parser.executeCommand("assignment-active", scanner, system));
        assertTrue(out.contains("usr3"));
        assertTrue(out.contains("Role3"));
    }

    @Test
    @DisplayName("assignment-expired: вывод истекших назначений")
    void testAssignmentExpired() {
        User u = User.create("expuser", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("ExpRole", "Desc", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new TemporaryAssignment(u, r, AssignmentMetadata.now("admin", "test"), "2000-01-01T23:59:59", false));

        Scanner scanner = new Scanner("\n");
        String out = captureOutput(() -> parser.executeCommand("assignment-expired", scanner, system));
        assertTrue(out.contains("expuser"));
        assertTrue(out.contains("ExpRole"));
    }

    @Test
    @DisplayName("assignment-extend: успешное продление")
    void testAssignmentExtend() {
        User u = User.create("extuser", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("ExtRole", "Desc", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new TemporaryAssignment(u, r, AssignmentMetadata.now("admin", "test"), "2025-01-01T23:59:59", false));

        Scanner scanner = new Scanner("extuser\nExtRole\n2030-01-01\n");
        String out = captureOutput(() -> parser.executeCommand("assignment-extend", scanner, system));
        assertTrue(out.contains("Assignment extended successfully"));
    }

    @Test
    @DisplayName("assignment-extend: ошибки")
    void testAssignmentExtendFails() {
        Scanner scUnk = new Scanner("unk\nunkR\n");
        assertTrue(captureOutput(() -> parser.executeCommand("assignment-extend", scUnk, system)).contains("User or role not found."));

        User u = User.create("usr1", "User One", "u@u.com");
        system.getUserManager().add(u);
        Role r = new Role("RoleR", "D", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("s", "s")));

        Scanner scPerm = new Scanner("usr1\nRoleR\n2030-01-01\n");
        assertTrue(captureOutput(() -> parser.executeCommand("assignment-extend", scPerm, system)).contains("Only temporary assignments can be extended."));
    }

    @Test
    @DisplayName("assignment-search: поиск по фильтрам")
    void testAssignmentSearch() {
        User u = User.create("searchuser", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("SearchRole", "Desc", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("admin", "test")));

        Scanner scanner1 = new Scanner("1\nsearchuser\n");
        String out1 = captureOutput(() -> parser.executeCommand("assignment-search", scanner1, system));
        assertTrue(out1.contains("searchuser"));

        Scanner scanner2 = new Scanner("3\nPERM\n");
        String out2 = captureOutput(() -> parser.executeCommand("assignment-search", scanner2, system));
        assertTrue(out2.contains("PERM"));
    }

    @Test
    @DisplayName("assignment-search: ошибки фильтров")
    void testAssignmentSearchFails() {
        Scanner scBad = new Scanner("99\n");
        assertTrue(captureOutput(() -> parser.executeCommand("assignment-search", scBad, system)).contains("Invalid choice."));

        Scanner scBadDate5 = new Scanner("5\nbad\n");
        assertTrue(captureOutput(() -> parser.executeCommand("assignment-search", scBadDate5, system)).contains("Invalid date."));

        Scanner scBadDate6 = new Scanner("6\nbad\n");
        assertTrue(captureOutput(() -> parser.executeCommand("assignment-search", scBadDate6, system)).contains("Invalid date."));
    }

    @Test
    @DisplayName("permissions-user: вывод прав пользователя с группировкой")
    void testPermissionsUser() {
        User u = User.create("permuser", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("PermRole2", "Desc", new HashSet<>(Set.of(new Permission("READ", "data", "desc"))));
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("admin", "test")));

        Scanner scanner = new Scanner("permuser\n");
        String out = captureOutput(() -> parser.executeCommand("permissions-user", scanner, system));
        assertTrue(out.contains("Resource: data"));
        assertTrue(out.contains("READ"));
    }

    @Test
    @DisplayName("permissions-user: ошибки")
    void testPermissionsUserFails() {
        Scanner scUnk1 = new Scanner("unk\n");
        assertTrue(captureOutput(() -> parser.executeCommand("permissions-user", scUnk1, system)).contains("User not found."));

        User u = User.create("usr1", "User One", "u@u.com");
        system.getUserManager().add(u);
        Scanner scNoPerm = new Scanner("usr1\n");
        assertTrue(captureOutput(() -> parser.executeCommand("permissions-user", scNoPerm, system)).contains("User has no permissions."));
    }

    @Test
    @DisplayName("permissions-check: проверка наличия конкретного права")
    void testPermissionsCheck() {
        User u = User.create("checkuser", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("CheckRole", "Desc", new HashSet<>(Set.of(new Permission("WRITE", "file", "desc"))));
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("admin", "test")));

        Scanner scanner1 = new Scanner("checkuser\nWRITE\nfile\n");
        String out1 = captureOutput(() -> parser.executeCommand("permissions-check", scanner1, system));
        assertTrue(out1.contains("Access GRANTED."));
        assertTrue(out1.contains("CheckRole"));

        Scanner scanner2 = new Scanner("checkuser\nREAD\ndata\n");
        String out2 = captureOutput(() -> parser.executeCommand("permissions-check", scanner2, system));
        assertTrue(out2.contains("Access DENIED."));
    }

    @Test
    @DisplayName("permissions-check: ошибка")
    void testPermissionsCheckFails() {
        Scanner scUnk2 = new Scanner("unk\n\n\n");
        assertTrue(captureOutput(() -> parser.executeCommand("permissions-check", scUnk2, system)).contains("User not found."));
    }

    @Test
    @DisplayName("help: вывод списка команд")
    void testHelp() {
        Scanner scanner = new Scanner("\n");
        String out = captureOutput(() -> parser.executeCommand("help", scanner, system));
        assertNotNull(out);
    }

    @Test
    @DisplayName("stats: вывод статистики")
    void testStats() {
        User u = User.create("statuser", "Test", "t@t.com");
        system.getUserManager().add(u);
        Role r = new Role("StatRole", "Desc", new HashSet<>());
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("admin", "test")));

        Scanner scanner = new Scanner("\n");
        String out = captureOutput(() -> parser.executeCommand("stats", scanner, system));
        assertNotNull(out);
        assertFalse(out.trim().isEmpty());
    }

    @Test
    @DisplayName("clear: очистка экрана")
    void testClear() {
        Scanner scanner = new Scanner("\n");
        String out = captureOutput(() -> parser.executeCommand("clear", scanner, system));
        assertTrue(out.contains("\033[H\033[2J") || out.contains("\n"));
    }

    @Test
    @DisplayName("save and load: сериализация и десериализация")
    void testSaveLoad() {
        User u = User.create("suser", "Save User", "s@s.com");
        system.getUserManager().add(u);
        Role r = new Role("SRole", "Desc", new HashSet<>(Set.of(new Permission("READ", "data", "desc"))));
        system.getRoleManager().add(r);
        system.getAssignmentManager().add(new PermanentAssignment(u, r, AssignmentMetadata.now("sys", "test")));

        Scanner scannerSave = new Scanner("test_rbac.txt\n");
        String outSave = captureOutput(() -> parser.executeCommand("save", scannerSave, system));
        assertTrue(outSave.contains("Data saved successfully."));

        Role.clearUsedNames();

        RBACSystem newSystem = new RBACSystem();
        Scanner scannerLoad = new Scanner("test_rbac.txt\n");
        String outLoad = captureOutput(() -> parser.executeCommand("load", scannerLoad, newSystem));
        assertTrue(outLoad.contains("Data loaded successfully."));

        assertTrue(newSystem.getUserManager().exists("suser"));
        assertTrue(newSystem.getRoleManager().exists("SRole"));

        new java.io.File("test_rbac.txt").delete();
    }

    @Test
    @DisplayName("exit: успешный выход и отмена")
    void testExit() {
        Scanner scanner1 = new Scanner("нет\n");
        String out1 = captureOutput(() -> parser.executeCommand("exit", scanner1, system));
        assertTrue(out1.contains("Exit cancelled."));

        Scanner scanner2 = new Scanner("да\nнет\n");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> captureOutput(() -> parser.executeCommand("exit", scanner2, system)));
        assertEquals("EXIT_SIGNAL", ex.getMessage());
    }

    private String captureOutput(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outContent));
            action.run();
            return outContent.toString();
        } finally {
            System.setOut(originalOut);
        }
    }
}