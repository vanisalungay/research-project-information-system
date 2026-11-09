package com.rpis.backend.config;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the start-up contract of the four schema-bootstrap steps.
 *
 * These steps used to create schema objects on every application start. They are now gated
 * behind an explicitly-enabled property, and this test pins both halves of that promise:
 * the default start-up issues SELECT statements only (so starting the application against a
 * real database cannot alter it), and the enabled path still never issues an UPDATE, DELETE,
 * TRUNCATE, DROP or ALTER and never moves a sequence backwards.
 *
 * No database is involved: the JdbcTemplate is a Mockito mock and the DataSource is a mock
 * whose connection reports "lock acquired". The behaviour under test is which SQL is issued,
 * and proving it must not require touching the live rpis_db.
 */
class BootstrapInitializerSafetyTest {

    @Test
    void disabledCooperatingAgencyBootstrapIssuesSelectOnly() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, "public.uk_cooperating_agencies_name_lower", null, null, 21);

        new CooperatingAgencyInitializer(jdbc, mock(DataSource.class), false).initializeDirectory();

        assertSelectOnly(issuedSql(jdbc), "cooperating agency verification");
    }

    @Test
    void disabledPriorityAgendaBootstrapIssuesSelectOnly() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, "public.uk_priority_agenda_set_single_active", null, null, 2);

        new PriorityAgendaInitializer(jdbc, mock(DataSource.class), false).initializeMasterData();

        assertSelectOnly(issuedSql(jdbc), "priority agenda verification");
    }

    @Test
    void disabledProposalCodeSequenceBootstrapIssuesSelectOnly() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, "public.proposal_code_seq", null, 26L, null);

        new ProposalCodeSequenceInitializer(jdbc, false).initializeSequence();

        List<String> issued = issuedSql(jdbc);
        assertSelectOnly(issued, "proposal code sequence verification");
        assertFalse(containsAny(issued, "setval", "nextval", "create", "alter"),
                "a disabled bootstrap must not create or advance the sequence: " + issued);
    }

    @Test
    void disabledEndorsementCodeSequenceBootstrapIssuesSelectOnly() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, "public.endorsement_code_seq", null, 10L, null);

        new EndorsementCodeSequenceInitializer(jdbc, false).initializeSequence();

        List<String> issued = issuedSql(jdbc);
        assertSelectOnly(issued, "endorsement code sequence verification");
        assertFalse(containsAny(issued, "setval", "nextval", "create", "alter"),
                "a disabled bootstrap must not create or advance the sequence: " + issued);
    }

    @Test
    void aMissingSequenceIsReportedButNotCreated() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, null, null, null, null);

        new ProposalCodeSequenceInitializer(jdbc, false).initializeSequence();

        List<String> issued = issuedSql(jdbc);
        assertSelectOnly(issued, "missing-sequence verification");
        assertFalse(containsAny(issued, "create sequence"),
                "reporting a missing sequence must not create it: " + issued);
    }

    @Test
    void theSharedConfigurationLeavesEveryBootstrapStepDisabled() throws IOException {
        String shared = readResource("application.properties");

        assertTrue(shared.contains("rpis.bootstrap.sequences.enabled=${RPIS_BOOTSTRAP_SEQUENCES_ENABLED:false}"),
                "the sequence bootstrap must default to false");
        assertTrue(shared.contains(
                "rpis.bootstrap.cooperating-agencies.enabled=${RPIS_BOOTSTRAP_COOPERATING_AGENCIES_ENABLED:false}"),
                "the cooperating agency bootstrap must default to false");
        assertTrue(shared.contains(
                "rpis.bootstrap.priority-agendas.enabled=${RPIS_BOOTSTRAP_PRIORITY_AGENDAS_ENABLED:false}"),
                "the priority agenda bootstrap must default to false");
    }

    @Test
    void onlyTheDisposableTestProfileEnablesTheBootstrap() throws IOException {
        String testProfile = readResource("application-test.properties");

        assertTrue(testProfile.contains("rpis.bootstrap.sequences.enabled=true"));
        assertTrue(testProfile.contains("rpis.bootstrap.cooperating-agencies.enabled=true"));
        assertTrue(testProfile.contains("rpis.bootstrap.priority-agendas.enabled=true"));
    }    @Test
    void enabledAgencyBootstrapOnlyInsertsMissingRowsAndNeverRewritesThem() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, "public.uk_cooperating_agencies_name_lower", null, null, 21);

        new CooperatingAgencyInitializer(jdbc, lockableDataSource(true), true).initializeDirectory();

        List<String> issued = issuedSql(jdbc);

        assertTrue(issued.stream().anyMatch(sql -> sql.toUpperCase(Locale.ROOT)
                        .startsWith("CREATE UNIQUE INDEX IF NOT EXISTS")),
                "the enabled path must create the unique index idempotently: " + issued);

        List<String> inserts = issued.stream()
                .filter(sql -> sql.toUpperCase(Locale.ROOT).startsWith("INSERT"))
                .toList();
        assertEquals(CooperatingAgencyInitializer.DEFAULT_AGENCIES.size(), inserts.size(),
                "one guarded insert per default agency");
        assertTrue(inserts.stream()
                        .allMatch(sql -> sql.toUpperCase(Locale.ROOT).contains("ON CONFLICT DO NOTHING")),
                "every insert must be skipped when the row already exists: " + inserts);

        assertFalse(containsAny(issued, "update ", "delete ", "truncate", "drop ", "alter "),
                "the enabled path must never rewrite existing data: " + issued);
        assertFalse(containsAny(issued, "set name", "active = false", "rename"),
                "an agency an administrator renamed must be preserved: " + issued);
    }

    @Test
    void enabledAgencyBootstrapTakesTheClusterLock() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, "public.uk_cooperating_agencies_name_lower", null, null, 21);
        DataSource dataSource = lockableDataSource(true);

        new CooperatingAgencyInitializer(jdbc, dataSource, true).initializeDirectory();

        verify(dataSource, atLeastOnce()).getConnection();
    }

    @Test
    void proposalSequenceAlignmentIsExpressedAsGreatestSoItCannotGoBackwards() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, "public.proposal_code_seq", Boolean.TRUE, 100L, null);

        new ProposalCodeSequenceInitializer(jdbc, true).initializeSequence();

        String setval = singleSetval(issuedSql(jdbc), "proposal_code_seq");
        assertTrue(setval.toUpperCase(Locale.ROOT).contains("GREATEST("),
                "the alignment must be a GREATEST so it can never move the sequence backwards: " + setval);
        assertTrue(setval.contains("last_value"),
                "the current last_value must be one operand of the GREATEST: " + setval);
        assertFalse(plainBackwardCapableSetval(setval, "proposal_code_seq"),
                "an unguarded setval to a fixed value could move the sequence backwards: " + setval);
    }

    @Test
    void endorsementSequenceAlignmentIsExpressedAsGreatestSoItCannotGoBackwards() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, "public.endorsement_code_seq", Boolean.TRUE, 10L, null);

        new EndorsementCodeSequenceInitializer(jdbc, true).initializeSequence();

        String setval = singleSetval(issuedSql(jdbc), "endorsement_code_seq");
        assertTrue(setval.toUpperCase(Locale.ROOT).contains("GREATEST("),
                "the alignment must be a GREATEST so it can never move the sequence backwards: " + setval);
        assertTrue(setval.contains("last_value"),
                "the current last_value must be one operand of the GREATEST: " + setval);
        assertFalse(plainBackwardCapableSetval(setval, "endorsement_code_seq"),
                "an unguarded setval to a fixed value could move the sequence backwards: " + setval);
    }

    @Test
    void aNeverUsedSequenceWithNoStoredRowsStartsAtOneWithoutMovingBackwards() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        stubReads(jdbc, "public.proposal_code_seq", Boolean.FALSE, null, null);

        new ProposalCodeSequenceInitializer(jdbc, true).initializeSequence();

        String setval = singleSetval(issuedSql(jdbc), "proposal_code_seq");
        assertTrue(setval.toLowerCase(Locale.ROOT).contains("1, false"),
                "a fresh sequence must start at 001 (is_called = false): " + setval);
    }    private static List<String> issuedSql(JdbcTemplate jdbc) {
        List<String> issued = new ArrayList<>();
        mockingDetails(jdbc).getInvocations().stream()
                .map(Invocation::getArguments)
                .filter(arguments -> arguments.length > 0 && arguments[0] instanceof String)
                .map(arguments -> (String) arguments[0])
                .forEach(issued::add);
        return issued;
    }

    private static void assertSelectOnly(List<String> issued, String what) {
        assertFalse(issued.isEmpty(), what + " should at least check the catalog");
        List<String> notSelect = issued.stream()
                .filter(sql -> !sql.trim().toUpperCase(Locale.ROOT).startsWith("SELECT"))
                .toList();
        assertTrue(notSelect.isEmpty(), what + " must issue SELECT statements only, but issued: " + notSelect);
    }

    private static boolean containsAny(List<String> sql, String... fragments) {
        for (String statement : sql) {
            String lower = statement.toLowerCase(Locale.ROOT);
            for (String fragment : fragments) {
                if (lower.contains(fragment)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String singleSetval(List<String> issued, String sequence) {
        List<String> setvals = issued.stream()
                .filter(sql -> sql.contains("setval('" + sequence + "'"))
                .toList();
        assertEquals(1, setvals.size(), "expected exactly one alignment statement: " + issued);
        return setvals.get(0);
    }

    private static boolean plainBackwardCapableSetval(String setval, String sequence) {
        return setval.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT)
                .matches(".*setval\\('" + sequence + "',\\s*\\d+.*");
    }

    private static void stubReads(JdbcTemplate jdbc, String relation, Boolean isCalled, Long maxCode,
            Integer count) {
        Answer<Object> answer = invocation -> {
            Class<?> type = invocation.getArgument(1);
            if (type == String.class) {
                return relation;
            }
            if (type == Boolean.class) {
                return isCalled;
            }
            if (type == Long.class) {
                return maxCode;
            }
            if (type == Integer.class) {
                return count;
            }
            return null;
        };
        when(jdbc.queryForObject(anyString(), any(Class.class))).thenAnswer(answer);
        when(jdbc.queryForObject(anyString(), any(Class.class), any(Object[].class))).thenAnswer(answer);
    }

    private static DataSource lockableDataSource(boolean grantLock) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(grantLock);

        PreparedStatement statement = mock(PreparedStatement.class);
        when(statement.executeQuery()).thenReturn(resultSet);

        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }

    private static String readResource(String name) throws IOException {
        try (InputStream in = BootstrapInitializerSafetyTest.class.getClassLoader()
                .getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("classpath resource not found: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}