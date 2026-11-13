package com.rpis.backend.model;

/**
 * The canonical role vocabulary of RPIS.
 *
 * <p>Every role comparison, security rule, token claim and database value is spelled
 * with one of these constants. That matters more than it looks: the Spring Security
 * rules and the services that enforce the same rule in code are two statements of one
 * policy, and they had already drifted apart once - the endorsement decision routes
 * required {@code DEAN} while every account was stored as {@code RPS_DEAN}, so every
 * Dean decision answered 403 even though the Dean had authenticated successfully and
 * held the right role. Binding both sides to one constant removes that whole class of
 * defect.
 *
 * <h2>The Dean is an independent role, not part of RPS</h2>
 * {@link #DEAN} is deliberately <strong>not</strong> named {@code RPS_DEAN}. The Dean /
 * Department Chair is an academic office inside a college: an RPS administrator grants
 * the role (see {@code DeanAccountService}) and the holder signs in through the ordinary
 * login, but the role carries no RPS privilege whatsoever.
 * <ul>
 *   <li>it is absent from {@code CurrentUser.PRIVILEGED_ROLES}, so a Dean never sees the
 *       university-wide proposal set;</li>
 *   <li>it is absent from {@code FileController.UNRESTRICTED_ROLES}, so a Dean can read
 *       only the endorsement letters assigned to it;</li>
 *   <li>its entire authority is the endorsement workflow
 *       ({@code PUT /api/endorsements/{id}/approve|reject}), re-scoped to the college it
 *       is assigned to in {@code EndorsementRequestService}.</li>
 * </ul>
 *
 * <p>These values are stored verbatim in {@code users.role} and embedded in the bearer
 * token, so a value may never change without a matching data migration. The one that
 * renamed the Dean role is {@code backend/migration-rename-dean-role.sql}.
 */
public final class UserRoles {

    private UserRoles() {
        // Constants only.
    }

    /** Faculty Proponent: submits proposals and reaches only their own records. */
    public static final String PROPONENT = "PROPONENT";

    /** RPS administrator: account management, master data, endorsement letter settings. */
    public static final String RPS_ADMIN = "RPS_ADMIN";

    /** RPS staff: completeness review and monitoring. */
    public static final String RPS_STAFF = "RPS_STAFF";

    /**
     * Dean / Department Chair. An independent role - see the class comment - whose only
     * authority is deciding the endorsement requests of the college it is assigned to.
     */
    public static final String DEAN = "DEAN";

    /** Office of the Vice Chancellor for Research, Innovation and Extension. */
    public static final String OVCRIGE = "OVCRIGE";

    /** Office of the Vice Chancellor for Administration and Finance. */
    public static final String OVCAF = "OVCAF";

    /** Research Evaluation Committee. */
    public static final String REC = "REC";

    /** Office of the Chancellor. */
    public static final String OC = "OC";
}
