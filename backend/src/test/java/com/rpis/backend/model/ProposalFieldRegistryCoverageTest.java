package com.rpis.backend.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the field-level remark feature against drift: every reviewable field of
 * {@link Proposal} (and of each repeated child entity) must appear in the frontend
 * field registry {@code src/utils/proposalFields.js}.
 * <p>
 * Without this test a new proposal field could silently have no remark control, which
 * is exactly the gap the field-level remark work was meant to close. Fields that are
 * intentionally not reviewable are listed in {@link #NOT_REVIEWABLE} with the reason.
 */
class ProposalFieldRegistryCoverageTest {

    /**
     * Proposal fields that are deliberately not reviewable: workflow state, identifiers,
     * relations or timestamps rather than proposal content.
     */
    private static final Set<String> NOT_REVIEWABLE = Set.of(
            // identifiers
            "id", "proposalCode", "revisionNumber", "documentId",
            // relations
            "proponent", "proposalAnnouncementId",
            // workflow state
            "status", "reviewedBy", "reviewedByPosition", "remarks",
            // Special Order / budget decision metadata
            "soNumber", "soFileName", "soFilePath", "soUploadedAt", "needsBudget",
            // return-for-revision routing metadata
            "returnedByOffice", "returnedByName", "returnedByUserId", "returnedAt", "returnRemarks",
            "revisionDeadline", "revisionForwardedAt", "revisionForwardedByName",
            "revisionForwardedByUserId", "revisionNotes",
            // timestamps
            "createdAt", "updatedAt",
            // repeated collections: covered through their columns below
            "sites", "logFrames", "personnel", "budget", "otherProjects", "priorityAgendas", "limitations");

    /** Child-entity fields that are structural rather than reviewable. */
    private static final Set<String> CHILD_STRUCTURAL = Set.of("id", "proposal");

    /**
     * Collections that are addressed by a natural key instead of by row position, and
     * are therefore modelled in the registry as keyed fields
     * ({@code priorityAgenda.<key>.value}) rather than as a table with columns.
     * {@link #keyedPriorityAgendaFieldsAreCovered()} asserts their coverage.
     */
    private static final Set<String> KEYED_COLLECTIONS = Set.of("priorityAgendas");

    private Path registryFile() {
        // Surefire runs with the backend module as the working directory.
        Path path = Paths.get("..", "src", "utils", "proposalFields.js");
        assertTrue(Files.exists(path), "field registry not found at " + path.toAbsolutePath());
        return path;
    }

    private String registry() throws Exception {
        return Files.readString(registryFile());
    }

    @Test
    void everyReviewableProposalFieldIsPresentInTheFieldRegistry() throws Exception {
        String source = registry();
        List<String> missing = new ArrayList<>();

        for (Field field : Proposal.class.getDeclaredFields()) {
            String name = field.getName();
            if (NOT_REVIEWABLE.contains(name) || field.isSynthetic()) {
                continue;
            }
            if (!source.contains("key: '" + name + "'")) {
                missing.add(name);
            }
        }

        assertTrue(missing.isEmpty(),
                "These proposal fields have no entry in src/utils/proposalFields.js: " + missing);
    }

    @Test
    void everyRepeatedEntryFieldIsPresentInTheFieldRegistry() throws Exception {
        String source = registry();
        List<String> missing = new ArrayList<>();

        for (Field field : Proposal.class.getDeclaredFields()) {
            Class<?> elementType = elementTypeOf(field);
            if (elementType == null || elementType.equals(String.class)) {
                continue;
            }
            if (KEYED_COLLECTIONS.contains(field.getName())) {
                continue;
            }
            // The collection itself must be declared so the UI can render its rows.
            if (!source.contains("prop: '" + field.getName() + "'")) {
                missing.add(field.getName() + " (collection)");
            }
            for (Field childField : elementType.getDeclaredFields()) {
                String childName = childField.getName();
                if (CHILD_STRUCTURAL.contains(childName) || childField.isSynthetic()) {
                    continue;
                }
                if (!source.contains("key: '" + childName + "'")) {
                    missing.add(field.getName() + "." + childName);
                }
            }
        }

        assertTrue(missing.isEmpty(),
                "These repeated-entry fields have no entry in src/utils/proposalFields.js: " + missing);
    }

    @Test
    void keyedPriorityAgendaFieldsAreCovered() throws Exception {
        String source = registry();
        assertTrue(source.contains("priorityAgenda."),
                "keyed priority-agenda fields must be present in the field registry");
        for (String agendaKey : List.of("dagat", "punla", "kalikasan", "negosyo", "tanglaw")) {
            assertTrue(source.contains("priorityAgenda." + agendaKey + ".value"),
                    "priority agenda key not covered: " + agendaKey);
        }
    }

    @Test
    void registryCoversAllTwentySevenNumberedSections() throws Exception {
        String source = registry();
        for (int i = 1; i <= 27; i++) {
            String key = String.format("%02d", i);
            assertTrue(source.contains("key: '" + key + "'"), "section not covered: " + key);
        }
    }

    @Test
    void thePreviouslyDroppedFieldsAreNowCoveredAndWrongKeysAreGone() throws Exception {
        String source = registry();
        assertTrue(source.contains("key: 'projectLeaderSex'"), "Sex must be reviewable");
        assertTrue(source.contains("key: 'limitationsText'"), "Limitations text must be reviewable");
        assertTrue(source.contains("key: 'review'"), "Review of Literature text must be reviewable");
        assertTrue(source.contains("key: 'timeDevoted'"), "Personnel time must use the entity key");
        assertFalse(source.contains("key: 'literatureCited'"),
                "the registry must not use the non-existent literatureCited key");
        assertFalse(source.contains("key: 'budgetFileName'"),
                "the registry must not use the non-existent budgetFileName key");
        assertFalse(source.contains("key: 'logFrame'"),
                "the registry must not use the singular logFrame key");
    }

    /** Element type of a {@code List<T>} field, or {@code null} for non-collections. */
    private Class<?> elementTypeOf(Field field) {
        if (!List.class.isAssignableFrom(field.getType())) {
            return null;
        }
        String signature = field.getGenericType().getTypeName();
        int start = signature.indexOf('<');
        int end = signature.lastIndexOf('>');
        if (start < 0 || end < 0) {
            return null;
        }
        try {
            return Class.forName(signature.substring(start + 1, end));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
