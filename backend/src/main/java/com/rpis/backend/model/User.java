package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    private String status = "APPROVED";
    
    @Transient
    private String token;

    /**
     * The generation of bearer tokens that are currently valid for this account.
     *
     * <p>A token carries the version it was issued with. Signing out increments this
     * column, so every token issued before the sign-out no longer matches and is
     * rejected by the authentication filter. Storing the counter in the shared
     * database (rather than in memory) is what makes the invalidation visible to every
     * instance behind the load balancer, and it keeps the token itself stateless.
     *
     * <p>Tokens issued before this column existed carry no version and are treated as
     * version 0, which is the value existing rows are migrated to - so upgrading does
     * not sign anybody out.
     */
    @Column(nullable = false)
    private int tokenVersion = 0;

    // NEW FIELD
    private String departmentOffice;

    // NEW FIELD
    private LocalDate dateRegistered;

    // NEW FIELD
    private Boolean emailVerified = false;

    /**
     * True while the account still holds an administrator-issued temporary password. The
     * account must replace it before it can use the rest of the application: the login
     * response carries this flag so the client can send the user to the change-password
     * page, and the API refuses the account until the password is changed. Existing accounts
     * default to false, so nobody is asked to change a password they chose themselves.
     */
    private Boolean mustChangePassword = false;

    private String contactNumber;
    private String specialization;

    /**
     * Notification Routing preferences (Manage Profile -> Notifications).
     *
     * <p>Stored on the account so the choice follows the user to any browser and any
     * instance behind the load balancer. The two lists hold the selected option
     * labels shown on that page; an empty list means the user has not opted into any
     * alert of that group, which is the same meaning as "all boxes clear".
     *
     * <p>Before this existed the page rendered two hardcoded option lists whose
     * "Save Notification Settings" button only showed a confirmation dialog - the
     * selection was never stored anywhere and was lost on reload.
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "notify_proposals", columnDefinition = "TEXT")
    private List<String> notifyProposals = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "notify_deadlines", columnDefinition = "TEXT")
    private List<String> notifyDeadlines = new ArrayList<>();

    /**
     * Designation / title of the account, printed on documents the account issues
     * (for example "Dean" or "Department Chair" on the official endorsement letter).
     * Optional for every other role.
     */
    private String designation;

    /**
     * The college the account is authorized to act for. Only meaningful for the Dean
     * role: an endorsement request is routed to the Dean assigned to its college.
     * Null means no college is configured yet, which the endorsement service treats
     * as the institution-wide fallback and records in the audit trail.
     */
    private String assignedCollege;

    /**
     * Electronic signature of the account (Manage Profile -> Electronic
     * Signature). The image is stored as a file under uploads/signatures/ - the
     * existing upload convention - so only its original file name and stored path
     * live on the account. The path is never serialized back to the client.
     */
    private String signatureFileName;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String signatureFilePath;
}