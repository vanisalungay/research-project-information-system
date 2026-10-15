package com.rpis.backend.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Canonical catalog of the numbered proposal sections.
 * <p>
 * The numbering and titles are shared with the proposal form and the Proponent's
 * Revision Page, so a remark always refers to the same part of the proposal on both
 * sides of the workflow. The server catalog is the source of truth: remarks whose
 * section key is not listed here are rejected instead of being persisted against a
 * non-existent section.
 */
public final class ProposalSectionCatalog {

    private static final Map<String, String> SECTIONS = new LinkedHashMap<>();

    static {
        SECTIONS.put("01", "Project Profile");
        SECTIONS.put("02", "Cooperating Agencies");
        SECTIONS.put("03", "Sites of Implementation");
        SECTIONS.put("04", "Type of Research");
        SECTIONS.put("05", "Priority Agenda");
        SECTIONS.put("06", "Innovation Goals");
        SECTIONS.put("07", "Sector Relevance");
        SECTIONS.put("08", "Sustainable Development Goals (SDG)");
        SECTIONS.put("09", "Executive Summary");
        SECTIONS.put("10", "Introduction");
        SECTIONS.put("11", "Review of Literature");
        SECTIONS.put("12", "Methodology");
        SECTIONS.put("13", "Technology Roadmap");
        SECTIONS.put("14", "Expected Outputs (6Ps)");
        SECTIONS.put("15", "Potential Outcomes");
        SECTIONS.put("16", "Potential Impacts (2Is)");
        SECTIONS.put("17", "Target Beneficiaries");
        SECTIONS.put("18", "Sustainability Plan");
        SECTIONS.put("19", "Gender and Development (GAD) Score");
        SECTIONS.put("20", "Limitations of the Project");
        SECTIONS.put("21", "Risk Management Plan");
        SECTIONS.put("22", "Logical Framework");
        SECTIONS.put("23", "Literature Cited");
        SECTIONS.put("24", "Personnel Requirement");
        SECTIONS.put("25", "Line-Item Budget Requirement");
        SECTIONS.put("26", "Other Ongoing Projects");
        SECTIONS.put("27", "Other Supporting Documents");
    }

    private ProposalSectionCatalog() {
    }

    /** All valid section keys, e.g. {@code "01"} ... {@code "27"}. */
    public static Set<String> keys() {
        return SECTIONS.keySet();
    }

    public static boolean isValid(String sectionKey) {
        return sectionKey != null && SECTIONS.containsKey(normalize(sectionKey));
    }

    /** Canonical label for a section key, or the raw key when unknown. */
    public static String labelFor(String sectionKey) {
        String normalized = normalize(sectionKey);
        return SECTIONS.getOrDefault(normalized, normalized);
    }

    /**
     * Normalizes a client-supplied section key to the catalog form. Accepts
     * {@code "12"}, {@code "012"} or {@code "section_12"} so a slightly different
     * frontend representation still maps onto the canonical key.
     */
    public static String normalize(String sectionKey) {
        if (sectionKey == null) {
            return null;
        }
        String value = sectionKey.trim().toLowerCase();
        if (value.startsWith("section_")) {
            value = value.substring("section_".length());
        }
        if (value.matches("\\d{1,2}")) {
            return String.format("%02d", Integer.parseInt(value));
        }
        return value;
    }
}
