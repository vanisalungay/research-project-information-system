package com.rpis.backend.dto;

import com.rpis.backend.model.DeanAssignment;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One Dean appointment as the administrator's Dean Role Management table shows it -
 * current and former alike, so a succession is readable in one list.
 */
@Data
public class DeanAssignmentResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String assignedCollege;
    private String designation;
    private String status;
    private LocalDateTime dateAssigned;
    private LocalDateTime dateRevoked;
    private String createdByName;
    private String revokedByName;

    /** Maps a stored appointment to its read model. */
    public static DeanAssignmentResponse of(DeanAssignment assignment) {
        DeanAssignmentResponse response = new DeanAssignmentResponse();
        response.id = assignment.getId();
        response.userId = assignment.getUserId();
        response.name = assignment.getDeanName();
        response.email = assignment.getDeanEmail();
        response.assignedCollege = assignment.getAssignedCollege();
        response.designation = assignment.getDesignation();
        response.status = assignment.getStatus();
        response.dateAssigned = assignment.getDateAssigned();
        response.dateRevoked = assignment.getDateRevoked();
        response.createdByName = assignment.getCreatedByName();
        response.revokedByName = assignment.getRevokedByName();
        return response;
    }
}
