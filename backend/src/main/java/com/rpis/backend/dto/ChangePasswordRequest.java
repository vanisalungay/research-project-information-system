package com.rpis.backend.dto;

import lombok.Data;

/**
 * The body of a password change.
 *
 * <p>Carries no account identifier on purpose: the account is taken from the authenticated
 * principal, so one account can never change another account's password. No password value is
 * ever returned by the endpoint that accepts this.
 */
@Data
public class ChangePasswordRequest {

    /** The password the account is currently signed in with. */
    private String currentPassword;

    /** The password the account wants to use from now on. */
    private String newPassword;

    /** The new password again, so a typo cannot lock the account out. */
    private String confirmPassword;
}