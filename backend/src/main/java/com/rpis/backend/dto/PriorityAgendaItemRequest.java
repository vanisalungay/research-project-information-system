package com.rpis.backend.dto;

import lombok.Data;

/**
 * Create / update payload of one Priority Agenda item.
 * <p>
 * {@code agendaKey} is only honoured when the item is created: it is the stable
 * identifier persisted with every proposal, so it is never editable afterwards and
 * a rename of the display {@code name} can never break a historical record.
 */
@Data
public class PriorityAgendaItemRequest {

    private String agendaKey;

    private String name;

    private String subLabel;

    private String description;

    private Integer displayOrder;

    private Boolean active;
}
