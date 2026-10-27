package com.rpis.backend.service;

import com.rpis.backend.dto.ActivePriorityAgendaResponse;
import com.rpis.backend.dto.PriorityAgendaItemRequest;
import com.rpis.backend.dto.PriorityAgendaSetRequest;
import com.rpis.backend.model.PriorityAgenda;
import com.rpis.backend.model.PriorityAgendaSet;
import com.rpis.backend.repository.PriorityAgendaRepository;
import com.rpis.backend.repository.PriorityAgendaSetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL-backed test of the Priority Agenda master data (section 05 of the Create
 * Proposal form): the framework the form used to hard-code is seeded and active, an
 * administrator can configure a new framework and coverage period without a code
 * change, exactly one framework is ever active, an agenda item can be deactivated
 * without deleting it, and a historical key still resolves after that.
 * <p>
 * Runs against the isolated {@code rpis_test} database only. Every test is
 * transactional, so the created rows are rolled back.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PriorityAgendaServicePostgresTest {

    @Autowired
    private PriorityAgendaService priorityAgendaService;
    @Autowired
    private PriorityAgendaSetRepository agendaSetRepository;
    @Autowired
    private PriorityAgendaRepository agendaRepository;

    private PriorityAgendaSetRequest setRequest(String frameworkName, int start, int end) {
        PriorityAgendaSetRequest request = new PriorityAgendaSetRequest();
        request.setFrameworkName(frameworkName);
        request.setCoverageStartYear(start);
        request.setCoverageEndYear(end);
        return request;
    }

    private PriorityAgendaItemRequest itemRequest(String name, String subLabel) {
        PriorityAgendaItemRequest request = new PriorityAgendaItemRequest();
        request.setName(name);
        request.setSubLabel(subLabel);
        return request;
    }

    private PriorityAgendaSet activeSet() {
        return agendaSetRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new AssertionError("no active framework"));
    }

    // ===== SEED =====

    @Test
    void theFormersFrameworkIsSeededAndActiveWithItsOriginalAgendas() {
        ActivePriorityAgendaResponse active = priorityAgendaService.getActiveConfiguration();

        assertNotNull(active.getAgendaSetId());
        assertEquals("MSUN RIIDE", active.getFrameworkName());
        assertEquals(2025, active.getCoverageStartYear());
        assertEquals(2028, active.getCoverageEndYear());
        assertEquals("(based on MSUN RIIDE 2025\u20132028)", active.getLabel());
        assertEquals(List.of("dagat", "punla", "kalikasan", "negosyo", "tanglaw"),
                active.getAgendas().stream().map(ActivePriorityAgendaResponse.Item::getAgendaKey).toList());
    }

    @Test
    void theSeededAgendaKeysAreTheOnesHistoricalProposalsStore() {
        // Every proposal already stores these keys, so the migration must not rename them.
        Set<String> keys = priorityAgendaService.activeAgendaKeys();
        assertEquals(Set.of("dagat", "punla", "kalikasan", "negosyo", "tanglaw"), keys);
    }

    // ===== VALIDATION =====

    @Test
    void anInvalidCoveragePeriodIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> priorityAgendaService.createSet(setRequest("MSUN RIDE", 2032, 2029)));
    }

    @Test
    void aMissingFrameworkNameOrYearIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> priorityAgendaService.createSet(setRequest("   ", 2030, 2033)));

        PriorityAgendaSetRequest noYear = new PriorityAgendaSetRequest();
        noYear.setFrameworkName("MSUN RIDE");
        assertThrows(IllegalArgumentException.class, () -> priorityAgendaService.createSet(noYear));
    }

    @Test
    void aDuplicateFrameworkAndPeriodIsRejected() {
        PriorityAgendaSetRequest duplicate = setRequest("msun riide", 2025, 2028);
        assertThrows(IllegalStateException.class, () -> priorityAgendaService.createSet(duplicate));
    }

    // ===== CONFIGURE A NEW FRAMEWORK =====

    @Test
    void aNewFrameworkAndCoveragePeriodCanBeConfigured() {
        PriorityAgendaSet created = priorityAgendaService.createSet(setRequest("MSUN RIDE", 2029, 2032));

        assertFalse(created.getActive(), "a new framework starts inactive");
        assertEquals("(based on MSUN RIDE 2029\u20132032)",
                PriorityAgendaService.formatLabel(created));

        priorityAgendaService.addItem(created.getId(), itemRequest("BLUE ECONOMY Program", "Sector"));
        priorityAgendaService.activateSet(created.getId());

        ActivePriorityAgendaResponse active = priorityAgendaService.getActiveConfiguration();
        assertEquals("MSUN RIDE", active.getFrameworkName());
        assertEquals(2029, active.getCoverageStartYear());
        assertEquals(2032, active.getCoverageEndYear());
        assertEquals("(based on MSUN RIDE 2029\u20132032)", active.getLabel());
        assertEquals(List.of("blue-economy-program"),
                active.getAgendas().stream().map(ActivePriorityAgendaResponse.Item::getAgendaKey).toList());
    }

    @Test
    void activatingANewFrameworkDeactivatesThePreviousOneAndKeepsIt() {
        PriorityAgendaSet previous = activeSet();
        PriorityAgendaSet next = priorityAgendaService.createSet(setRequest("MSUN RIDE", 2029, 2032));

        priorityAgendaService.activateSet(next.getId());

        List<PriorityAgendaSet> all = priorityAgendaService.getAllSets();
        long activeCount = all.stream().filter(set -> Boolean.TRUE.equals(set.getActive())).count();
        assertEquals(1, activeCount, "only one framework may be active");

        // The previous framework and its items are still there (nothing is deleted).
        PriorityAgendaSet reloaded = priorityAgendaService.getSet(previous.getId());
        assertFalse(reloaded.getActive());
        assertEquals(5, reloaded.getAgendas().size());
    }

    // ===== AGENDA ITEMS =====

    @Test
    void aNewAgendaBecomesAvailableOnceItIsAdded() {
        PriorityAgendaSet set = activeSet();
        PriorityAgenda item = priorityAgendaService.addItem(set.getId(),
                itemRequest("DAGAT Program (Coastal)", "Commodity"));

        assertEquals("dagat-program-coastal", item.getAgendaKey());
        assertTrue(item.getActive());
        assertTrue(priorityAgendaService.activeAgendaKeys().contains("dagat-program-coastal"));
    }

    @Test
    void aDuplicateAgendaNameInsideAFrameworkIsRejected() {
        PriorityAgendaSet set = activeSet();
        assertThrows(IllegalStateException.class,
                () -> priorityAgendaService.addItem(set.getId(), itemRequest("DAGAT Program", "Commodity")));
    }

    @Test
    void deactivatingAnAgendaRemovesItFromTheFormButKeepsItForHistory() {
        PriorityAgendaSet set = activeSet();
        PriorityAgenda dagat = agendaRepository.findByAgendaSetIdOrderByDisplayOrderAscIdAsc(set.getId())
                .stream()
                .filter(item -> "dagat".equals(item.getAgendaKey()))
                .findFirst()
                .orElseThrow();

        PriorityAgendaItemRequest update = itemRequest("DAGAT Program", "Commodity");
        update.setActive(false);
        priorityAgendaService.updateItem(dagat.getId(), update);

        // It is no longer offered to a new proposal...
        assertFalse(priorityAgendaService.activeAgendaKeys().contains("dagat"));
        assertFalse(priorityAgendaService.getActiveConfiguration().getAgendas().stream()
                .anyMatch(item -> "dagat".equals(item.getAgendaKey())));

        // ...but it is still in the admin list, still resolvable, and never deleted.
        assertTrue(priorityAgendaService.getAllSets().stream()
                .flatMap(s -> s.getAgendas().stream())
                .anyMatch(item -> "dagat".equals(item.getAgendaKey())));
        assertEquals("DAGAT Program", priorityAgendaService.lookup(List.of("dagat")).get(0).getName());
    }

    @Test
    void lookupResolvesAKeyOfADeactivatedFramework() {
        PriorityAgendaSet next = priorityAgendaService.createSet(setRequest("MSUN RIDE", 2029, 2032));
        priorityAgendaService.addItem(next.getId(), itemRequest("BLUE ECONOMY Program", "Sector"));
        priorityAgendaService.activateSet(next.getId());

        // A proposal submitted under the old framework still resolves its names.
        List<ActivePriorityAgendaResponse.Item> resolved =
                priorityAgendaService.lookup(List.of("dagat", "kalikasan"));
        assertEquals(List.of("DAGAT Program", "KALIKASAN Program"),
                resolved.stream().map(ActivePriorityAgendaResponse.Item::getName).toList());

        // An unknown key is simply absent (never invented).
        assertTrue(priorityAgendaService.lookup(List.of("not-a-real-agenda")).isEmpty());
    }

    // ===== SUBMISSION VALIDATION SUPPORT =====

    @Test
    void theAllowedProposalKeysAreTheActiveOnesPlusWhatTheProposalStores() {
        Set<String> allowed = priorityAgendaService.allowedProposalKeys(List.of("legacy-agenda"));

        assertTrue(allowed.contains("dagat"));
        assertTrue(allowed.contains("legacy-agenda"), "a stored key must stay allowed for a revision");
        assertFalse(allowed.contains("brand-new"));
    }
}
