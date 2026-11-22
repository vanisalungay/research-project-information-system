package com.rpis.backend.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the role column on notifications and its safe back-fill: every start-up applies
 * it, a single-role account's rows are attributed to its role, a multi-role account's rows
 * are attributed only from their type/context, and an unattributable legacy row is left
 * NULL instead of being guessed.
 */
class NotificationRoleMigrationTest {

    private static final Path BACKEND = Path.of("");

    private String read(String file) throws Exception {
        return Files.readString(BACKEND.resolve(file));
    }

    @Test
    void theMigrationAddsAndBackfillsTheRoleColumn() throws Exception {
        String sql = read("migration-add-notification-recipient-role.sql");
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS recipient_role VARCHAR(50)"));
        assertTrue(sql.contains("recipient_role IS NULL"),
                "only rows without a role are back-filled, so re-running is safe");
        assertTrue(sql.contains("WHEN r.role_count = 1 THEN r.only_role"),
                "a single-role account's rows belong to that role");
        assertTrue(sql.contains("WHEN n.type = 'DEAN_ASSIGNMENT' THEN 'DEAN'"));
        assertTrue(sql.contains("ELSE NULL"),
                "a row that cannot be attributed safely is not guessed");
        assertTrue(sql.contains("multi-role accounts with an unscoped notification"),
                "the verification reports multi-role rows that remain unscoped");
    }

    @Test
    void theEntityAndEveryStartUpUseIt() throws Exception {
        assertTrue(read("src/main/java/com/rpis/backend/model/Notification.java")
                        .contains("@Column(name = \"recipient_role\""),
                "the entity must store the role");
        assertTrue(read("../run-backend.bat")
                        .contains("migration-add-notification-recipient-role.sql"));
        assertTrue(read("DEPLOYMENT.md")
                        .contains("migration-add-notification-recipient-role.sql"));
    }
}
