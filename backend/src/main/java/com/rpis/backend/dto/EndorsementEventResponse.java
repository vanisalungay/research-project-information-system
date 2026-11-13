package com.rpis.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** One entry of an endorsement request's audit trail, as shown to authorized users. */
@Data
public class EndorsementEventResponse {

    private String eventType;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private String detail;
    private LocalDateTime createdAt;
}
