package com.rpis.backend.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the domain correction: new accounts are issued @msunaawan.edu.ph, the retired
 * misspelling is no longer generated, and the migration corrects existing rows without
 * merging an address that is already taken.
 */
class InstitutionalEmailDomainMigrationTest {

    private static final Path BACKEND = Path.of("");

    private String read(String file) throws Exception {
        return Files.readString(BACKEND.resolve(file));
    }

    @Test
    void theGeneratorUsesTheCorrectDomainAndRejectsTheRetiredOne() throws Exception {
        String service = read("src/main/java/com/rpis/backend/service/InstitutionalCredentialsService.java");
        assertTrue(service.contains("INSTITUTIONAL_DOMAIN = \"@msunaawan.edu.ph\""),
                "generated addresses must use the correct university domain");
        assertFalse(service.contains("@msunawaan.edu.ph"),
                "the retired misspelling must not remain in the generator or validation");
        assertFalse(service.contains("LEGACY_DOMAIN"),
                "there is no second accepted domain once the accounts are corrected");
    }

    @Test
    void theMigrationCorrectsAccountsAndAppointmentSnapshotsSafely() throws Exception {
        String sql = read("migration-fix-institutional-email-domain.sql");
        assertTrue(sql.contains("UPDATE users u"));
        assertTrue(sql.contains("split_part(u.email, '@', 1) || '@msunaawan.edu.ph'"),
                "only the domain is replaced; the local part is preserved");
        assertTrue(sql.contains("NOT EXISTS"),
                "an address that is already taken must not be reassigned");
        assertTrue(sql.contains("UPDATE dean_assignments a"),
                "the appointment snapshots follow the corrected account");
        assertTrue(sql.contains("split_part(a.dean_email, '@', 1)"),
                "a snapshot is only repointed when its local part agrees");
        assertTrue(sql.contains("unresolved conflict"),
                "the verification lists conflicts for manual resolution");
    }

    @Test
    void everyStartUpAndDeploymentAppliesTheCorrection() throws Exception {
        assertTrue(read("../run-backend.bat").contains("migration-fix-institutional-email-domain.sql"),
                "the local launcher must apply the correction");
        assertTrue(read("DEPLOYMENT.md").contains("migration-fix-institutional-email-domain.sql"),
                "the deployment runbook must list it");
    }
}
