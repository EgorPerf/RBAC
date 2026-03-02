package org.example.rbac.command;

import org.example.rbac.RBACSystem;

import java.util.Scanner;

@FunctionalInterface
public interface Command {
    void execute(Scanner scanner, RBACSystem system);
}