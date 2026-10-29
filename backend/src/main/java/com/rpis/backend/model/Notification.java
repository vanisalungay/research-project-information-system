package com.rpis.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "title")
    private String title;

    @Column(name = "type")
    private String type;

    /**
     * The record this notification is about, interpreted by {@link #type}: the proposal
     * for a proposal notification, and the endorsement request for an endorsement
     * notification ({@code ENDORSEMENT}), which is what clicking the notification opens.
     * Null when the notification is about an announcement instead - see
     * {@link #proposalAnnouncementId}.
     */
    @Column(name = "proposal_id")
    private Long proposalId;

    /**
     * The Proposal Announcement (Call for Proposals) this notification is about.
     * Null for every notification that is not raised for an announcement (for
     * example proposal status notifications). Lets the Proponent notification list
     * open the exact announcement the notification refers to.
     */
    @Column(name = "proposal_announcement_id")
    private Long proposalAnnouncementId;

    /**
     * Computed (not persisted) name of the Call for Proposals this notification is
     * about, filled by the notification service from the referenced announcement so
     * the Proponent notification list can show the call title.
     */
    @Transient
    private String proposalAnnouncementName;

    /**
     * Computed (not persisted) current status of the Call for Proposals this
     * notification is about: DRAFT, SCHEDULED, OPEN or CLOSED. Derived from the
     * saved status plus the saved dates at read time (so a Scheduled call that has
     * since opened reads as OPEN), which lets the Proponent notification list show
     * the colored status indicator. Null when the notification has no announcement.
     */
    @Transient
    private String proposalAnnouncementStatus;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
