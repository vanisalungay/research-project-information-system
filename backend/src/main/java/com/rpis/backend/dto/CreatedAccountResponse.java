package com.rpis.backend.dto;

import com.rpis.backend.model.User;
import lombok.Data;

/**
 * The one response that ever carries a temporary password.
 *
 * <p>An administrator-created account is issued an institutional address and a randomly
 * generated temporary password. The administrator has to hand those to the person, so they
 * are returned once, here, by the create-account endpoint - and by nothing else. Ordinary
 * account reads, the account list and the login response never carry the password: the
 * account itself is always serialized with its stored hash removed.
 */
@Data
public class CreatedAccountResponse {

    /** The created account, with the stored password hash removed. */
    private User user;

    /** The institutional address the account was created with. */
    private String email;

    /** The generated temporary password: returned once, stored only as a BCrypt hash. */
    private String temporaryPassword;

    /** True when the plain address was taken and a numbered variant had to be issued. */
    private boolean emailAdjusted;

    /** True when the account must replace this password at first login. */
    private boolean mustChangePassword;

    /**
     * True when an existing account was reused (the person already had an account with
     * this institutional address) instead of a new account being created. A reused
     * account keeps its password, so {@link #temporaryPassword} is null and the client
     * must not present credentials that were never issued.
     */
    private boolean accountReused;

    /** Builds the response, never serializing the stored hash. */
    public static CreatedAccountResponse of(
            User user, String email, String temporaryPassword, boolean emailAdjusted) {
        return of(user, email, temporaryPassword, emailAdjusted, false);
    }

    public static CreatedAccountResponse of(User user, String email, String temporaryPassword,
            boolean emailAdjusted, boolean accountReused) {
        CreatedAccountResponse response = new CreatedAccountResponse();
        // The password field is WRITE_ONLY on the entity, so the stored hash is never
        // serialized; do NOT null it here. Mutating the account would mark a managed row
        // dirty and, inside a surrounding transaction, fail the not-null column.
        response.setUser(user);
        response.setEmail(email);
        response.setTemporaryPassword(temporaryPassword);
        response.setEmailAdjusted(emailAdjusted);
        response.setAccountReused(accountReused);
        response.setMustChangePassword(user != null && Boolean.TRUE.equals(user.getMustChangePassword()));
        return response;
    }
}