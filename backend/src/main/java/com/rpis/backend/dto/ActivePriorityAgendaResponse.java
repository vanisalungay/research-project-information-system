package com.rpis.backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * The Priority Agenda configuration a new proposal selects from: the active
 * framework (agenda set) and its active agenda items.
 * <p>
 * Every field is {@code null} / empty when no set is active, which the Proponent
 * form renders as its empty state. That is deliberately a successful response
 * (HTTP 200) rather than an error, so "nothing is configured yet" can never be
 * confused with "the request failed".
 * <p>
 * {@code label} is the ready-to-display heading, for example
 * {@code (based on MSUN RIIDE 2025-2028)}, composed from the stored framework name
 * and coverage period - never hard-coded.
 */
@Data
public class ActivePriorityAgendaResponse {

    private Long agendaSetId;

    private String frameworkName;

    private Integer coverageStartYear;

    private Integer coverageEndYear;

    private String label;

    private List<Item> agendas = new ArrayList<>();

    /** One active agenda item offered to new proposals. */
    @Data
    public static class Item {
        private Long id;
        private String agendaKey;
        private String name;
        private String subLabel;
        private String description;
        private Integer displayOrder;
    }
}
