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

    @Column(name = "is_read")
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
