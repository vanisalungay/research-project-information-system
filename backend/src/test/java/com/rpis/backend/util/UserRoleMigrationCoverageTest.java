package com.rpis.backend.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the multi-role schema: the entity reads {@code user_roles}, and every existing
 * account keeps its role through the migration. A deployment that adds the code but not
 * the migration would fail every role query, so the run/deploy lists are checked too.
 */
class UserRoleMigrationCoverageTest {

    private static final Path BACKEND = Path.of("");

    private String read(String file) throws Exception {
        return Files.readString(BACKEND.resolve(file));
    }

    @Test
    void theMigrationCreatesAndBackfillsTheAssignmentTable() throws Exception {
        String sql = read("migration-add-user-roles.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS user_roles"),
                "the migration must create the assignment table idempotently");
        assertTrue(sql.contains("INSERT INTO user_roles (user_id, role)"),
                "every existing account's role must be back-filled");
        assertTrue(sql.contains("SELECT id, UPPER(TRIM(role))"),
                "the back-fill must normalize the stored role the way the code does");
        assertTrue(sql.contains("ON CONFLICT (user_id, role) DO NOTHING"),
                "re-running the migration must not duplicate a role");
        assertTrue(sql.contains("REFERENCES users(id)"),
                "an assignment must point at a real account");
    }

    @Test
    void everyStartUpAndDeploymentAppliesTheMigration() throws Exception {
        assertTrue(read("../run-backend.bat").contains("migration-add-user-roles.sql"),
                "the local launcher must apply the migration");
        assertTrue(read("DEPLOYMENT.md").contains("migration-add-user-roles.sql"),
                "the deployment runbook must list it in order");
    }

    @Test
    void theAccountEntityReadsTheSameTable() throws Exception {
        String entity = read("src/main/java/com/rpis/backend/model/User.java");
        assertTrue(entity.contains("@CollectionTable(name = \"user_roles\""),
                "the entity must read the table the migration creates");
        assertTrue(entity.contains("public boolean hasRole(String wanted)"),
                "role checks must consult the assignment set");
        assertTrue(entity.contains("@JsonProperty(\"roles\")"),
                "the API must expose every role the account holds");
    }
}
