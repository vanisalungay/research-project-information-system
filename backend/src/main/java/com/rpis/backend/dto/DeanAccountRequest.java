package com.rpis.backend.dto;

import lombok.Data;

/**
 * Administrative payload for creating a Dean account or for assigning / updating
 * the Dean role on an existing account.
 *
 * <p>Only an RPS_ADMIN may submit it (enforced by SecurityConfig and re-checked in
 * the service), and the service refuses to create or promote anything but the Dean
 * role through this endpoint, so it can never be used to hand out staff or
 * administrator privileges. When {@code userId} is supplied the Dean role is
 * assigned to that existing account instead of creating a duplicate one; the
 * password is then ignored and never overwritten.
 */
@Data
public class DeanAccountRequest {

    /** Existing account to promote to Dean. Null = create a new Dean account. */
    private Long userId;

    /** Full name of the Dean. Required when creating a new account. */
    private String name;

    /** Institutional email. Required when creating a new account. */
    private String email;

    /** Initial password, chosen by the administrator. Required when creating. */
    private String password;

    /** Designation / title, e.g. "Dean" or "Department Chair". */
    private String designation;

    /** College the Dean is authorized to endorse for. */
    private String assignedCollege;

    /** Account status: APPROVED (active) or INACTIVE (deactivated). */
    private String status;
}
