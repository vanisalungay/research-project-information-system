package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    /**
     * The account's primary role. It is the role the account was created with and the
     * default shown in account management; {@link #roleAssignments} holds every role the
     * account may sign in as. A person who is a Proponent and is later appointed Dean keeps
     * this primary role and gains {@code DEAN} in the assignments - the account and its
     * address are never duplicated.
     */
    @Column(nullable = false)
    private String role;

    /**
     * Every role assigned to this account, one row per role in {@code user_roles}. The
     * bearer token carries the role the user selected at sign-in, and that role is checked
     * against this set on every request, so an account may be a Proponent and a Dean at the
     * same time without either role granting the other's authority.
     *
     * <p>Kept out of the JSON body under its storage name; {@link #getRoles()} exposes the
     * effective set (the primary role included) as {@code roles}.
     */
    @JsonIgnore
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    private Set<String> roleAssignments = new LinkedHashSet<>();

    private String status = "APPROVED";
    
    @Transient
    private String token;

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
     * Whether the account has explicitly saved the Notifications tab.
     *
     * <p>Without this flag a never-configured account and an account that deliberately
     * cleared every box are indistinguishable: both store an empty list. False means
     * "use the defaults" (optional proposal alerts on, deadline reminders off); true
     * makes an empty list a real "all optional alerts off" choice. Core workflow alerts
     * are delivered regardless.
     */
    @Column(name = "notify_preferences_configured")
    private Boolean notifyPreferencesConfigured = false;

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

    /**
     * The status of the account's Dean appointment ({@code ACTIVE} or {@code REVOKED}),
     * for a read model of the administrator's account list. It is not stored here - the
     * appointment row is the source of truth. It is set for an account with an active Dean
     * appointment and for an account whose Dean appointment is only history (including an
     * account that no longer holds the active Dean role).
     */
    @Transient
    private String deanStatus;

    /**
     * Roles the account once held but no longer holds, filled by the read model from the
     * historical appointment records. A revoked Dean assignment is shown as
     * <em>Dean (Revoked)</em> in account management instead of being replaced by an
     * invented Proponent role. These values never authorize access.
     */
    @Transient
    private List<String> revokedRoles = new ArrayList<>();

    /**
     * The roles the account currently holds. This is the list the API returns (the storage
     * collection itself is never serialized), and it is the only list role checks use. The
     * primary {@link #role} column is historical: an account whose Dean role was revoked
     * keeps {@code DEAN} as its primary value for the record, but it is absent here and
     * therefore grants nothing.
     */
    @Transient
    @JsonProperty("roles")
    public List<String> getRoles() {
        Set<String> effective = new LinkedHashSet<>();
        if (roleAssignments != null) {
            for (String assigned : roleAssignments) {
                String normalized = canonical(assigned);
                if (normalized != null) {
                    effective.add(normalized);
                }
            }
        }
        List<String> active = new ArrayList<>(effective);
        // Stable order for the UI and the API: the primary role first when it is active,
        // then the remaining roles alphabetically. A persisted collection has no
        // guaranteed iteration order, so this cannot rely on insertion order.
        active.sort(String::compareTo);
        String primary = canonical(role);
        if (primary != null) {
            int index = active.indexOf(primary);
            if (index > 0) {
                active.remove(index);
                active.add(0, primary);
            }
        }
        return active;
    }

    /**
     * Keeps the primary role and the active assignment set in step when a role is set
     * programmatically. Hydration by JPA uses the field directly, so loaded rows keep
     * exactly the assignments stored in {@code user_roles}.
     */
    public void setRole(String role) {
        this.role = role;
        String normalized = canonical(role);
        if (normalized != null) {
            if (roleAssignments == null) {
                roleAssignments = new LinkedHashSet<>();
            }
            roleAssignments.add(normalized);
        }
    }

    /** True when the account currently holds the given role. Revoked roles never match. */
    public boolean hasRole(String wanted) {
        String target = canonical(wanted);
        if (target == null || roleAssignments == null) {
            return false;
        }
        for (String assigned : roleAssignments) {
            if (target.equals(canonical(assigned))) {
                return true;
            }
        }
        return false;
    }

    /** Adds a role without changing the primary role or removing any other assignment. */
    public void assignRole(String assignedRole) {
        String normalized = canonical(assignedRole);
        if (normalized == null) {
            return;
        }
        if (roleAssignments == null) {
            roleAssignments = new LinkedHashSet<>();
        }
        roleAssignments.add(normalized);
        if (canonical(role) == null) {
            role = normalized;
        }
    }

    /** Removes one assigned role. The primary role is handled by the caller explicitly. */
    public void removeRole(String assignedRole) {
        String normalized = canonical(assignedRole);
        if (normalized == null || roleAssignments == null) {
            return;
        }
        roleAssignments.removeIf(assigned -> normalized.equals(canonical(assigned)));
    }

    private static String canonical(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }
}