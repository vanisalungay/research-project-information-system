package com.rpis.backend.dto;

import lombok.Data;

/**
 * Create / update payload of a Priority Agenda framework (agenda set).
 * <p>
 * {@code active} is optional: when it is {@code true} the set becomes the active one
 * and every other set is deactivated in the same transaction, so the
 * single-active-set rule can never be broken by an update.
 */
@Data
public class PriorityAgendaSetRequest {

    private String frameworkName;

    private Integer coverageStartYear;

    private Integer coverageEndYear;

    private Boolean active;
}
