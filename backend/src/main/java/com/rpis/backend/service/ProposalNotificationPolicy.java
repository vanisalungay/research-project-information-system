package com.rpis.backend.service;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationTypes;
import com.rpis.backend.util.ProposalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Decides who is told about a proposal status transition and what the message says.
 *
 * <p>Keeping this in one place makes the recipient rules testable without constructing
 * the whole proposal service, and stops the messages from drifting apart between the
 * status-driven path and the review/budget services.
 *
 * <p>Every recipient is addressed by role through {@link NotificationService#notifyRoles},
 * so a role that is not involved in a transition is never notified, and an account that
 * appears in two roles receives one row. "RPS" always means both {@code RPS_ADMIN} and
 * {@code RPS_STAFF}: the administrator manages the office and must see the work that is
 * waiting there.
 */
@Service
@RequiredArgsConstructor
public class ProposalNotificationPolicy {

    private static final List<String> RPS = List.of(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF);
    private static final List<String> OVCRIGE = List.of(UserRoles.OVCRIGE);
    private static final List<String> OVCAF = List.of(UserRoles.OVCAF);
    private static final List<String> REC = List.of(UserRoles.REC);
    private static final List<String> OC = List.of(UserRoles.OC);

    private final NotificationService notificationService;

    public void notifyForStatus(Proposal proposal) {
        notifyForStatus(proposal, false);
    }

    /**
     * Raises the notification that belongs to the proposal's current status.
     *
     * @param resubmission true when this submission was a revision resubmission, so the
     *                     RPS receives a "resubmitted" message instead of "new proposal"
     */
    public void notifyForStatus(Proposal proposal, boolean resubmission) {
        if (proposal == null || proposal.getStatus() == null) {
            return;
        }
        String status = ProposalStatus.normalize(proposal.getStatus());
        if (status == null) {
            return;
        }
        String title = proposal.getProjectTitle();
        Long proposalId = proposal.getId();

        switch (status) {
            case ProposalStatus.FOR_REVIEW, ProposalStatus.LEGACY_SUBMITTED -> {
                String message = resubmission
                        ? "The revised proposal \"" + title + "\" has been resubmitted for review."
                        : "New proposal submitted for review: \"" + title + "\"";
                notificationService.notifyRoles(RPS, message,
                        resubmission ? "Proposal Resubmitted" : "New Proposal Submitted",
                        NotificationTypes.PROPOSAL_UPDATE, proposalId,
                        resubmission ? NotificationEvent.REVISION_SUBMITTED
                                : NotificationEvent.NEW_PROPOSAL);
                // The Proponent's own confirmation. It is the same submission event, so
                // the two recipients share the event's preference/dedupe key.
                notifyProponent(proposal,
                        resubmission
                                ? "Your revised proposal \"" + title
                                        + "\" has been resubmitted successfully and is now with "
                                        + "RPS for review."
                                : "Your proposal \"" + title
                                        + "\" has been submitted successfully and received by "
                                        + "RPS for review.",
                        resubmission ? "Proposal Resubmitted" : "Proposal Submitted",
                        NotificationTypes.PROPOSAL_SUBMISSION,
                        resubmission ? NotificationEvent.REVISION_SUBMITTED
                                : NotificationEvent.NEW_PROPOSAL);
            }
            case "ENDORSED" -> {
                notificationService.notifyRoles(OVCRIGE,
                        "A proposal has been endorsed to OVCRIGE: \"" + title + "\"",
                        "Proposal Endorsed", NotificationTypes.PROPOSAL_ENDORSED, proposalId,
                        NotificationEvent.STATUS_UPDATED);
                notifyProponent(proposal,
                        "Your proposal \"" + title
                                + "\" has been reviewed and endorsed by RPS. It has been "
                                + "forwarded to OVCRIGE for review.",
                        "Proposal Endorsed by RPS", NotificationTypes.PROPOSAL_ENDORSED,
                        NotificationEvent.STATUS_UPDATED);
            }
            case "UNDER_REVIEW" -> {
                notificationService.notifyRoles(REC,
                        "Proposal awaiting REC review and evaluation (office-level request): \""
                                + title + "\"",
                        "REC Office Review Request", NotificationTypes.REVIEW_REQUEST, proposalId,
                        NotificationEvent.NEW_PROPOSAL);
                notifyProponent(proposal,
                        "Your proposal \"" + title
                                + "\" has been forwarded to the Research Ethics Committee (REC) "
                                + "for review.",
                        "Proposal Forwarded to REC", NotificationTypes.REVIEW_REQUEST,
                        NotificationEvent.STATUS_UPDATED);
            }
            case "REC_APPROVED" -> {
                notificationService.notifyRoles(OVCRIGE,
                        "REC has evaluated and recommended proposal for approval: \"" + title + "\"",
                        "Proposal Approved by REC", NotificationTypes.APPROVAL, proposalId,
                        NotificationEvent.STATUS_UPDATED);
                notifyProponent(proposal,
                        "Your proposal \"" + title
                                + "\" has been approved by the Research Ethics Committee (REC) "
                                + "and forwarded to OVCRIGE for coordination.",
                        "Proposal Approved by REC", NotificationTypes.APPROVAL,
                        NotificationEvent.STATUS_UPDATED);
            }
            case "OVC_APPROVED" -> {
                // The OVC review accepted the proposal. The Chancellor's Office is told when
                // OVCRIGE actually forwards it (FOR_OC_APPROVAL), not here, so one action
                // cannot produce two "Final Approval Required" rows.
                notifyProponent(proposal,
                        "Your proposal \"" + title
                                + "\" has been approved by OVCRIGE and is ready to be forwarded "
                                + "to the Office of the Chancellor.",
                        "Proposal Approved by OVCRIGE", NotificationTypes.APPROVAL,
                        NotificationEvent.STATUS_UPDATED);
            }
            case "FOR_OC_APPROVAL" -> {
                notificationService.notifyRoles(OC,
                        "A proposal is ready for your final approval: \"" + title + "\"",
                        "Final Approval Required", NotificationTypes.APPROVAL, proposalId,
                        NotificationEvent.STATUS_UPDATED);
                notifyProponent(proposal,
                        "Your proposal \"" + title
                                + "\" has been forwarded to the Office of the Chancellor for "
                                + "final approval.",
                        "Proposal Forwarded to the Office of the Chancellor",
                        NotificationTypes.APPROVAL, NotificationEvent.STATUS_UPDATED);
            }
            case "FOR_OVCAF_APPROVAL" -> {
                notificationService.notifyRoles(OVCAF,
                        "A proposal is ready for your review and endorsement: \"" + title + "\"",
                        "Proposal for OVCAF Review", NotificationTypes.FUNDING, proposalId,
                        NotificationEvent.STATUS_UPDATED);
                notifyProponent(proposal,
                        "Your proposal \"" + title
                                + "\" has been approved by the Office of the Chancellor."
                                + specialOrderSentence(proposal)
                                + " It has been forwarded to OVCAF for financial validation.",
                        "Proposal Approved by the Chancellor", NotificationTypes.APPROVAL,
                        NotificationEvent.STATUS_UPDATED);
            }
            case "RETURNED_TO_RPS" -> notificationService.notifyRoles(RPS,
                    "A proposal was returned for revision and awaits your review and forwarding: \""
                            + title + "\"."
                            + remarksSentence(proposal),
                    "Revision Request Received", NotificationTypes.REVISION, proposalId,
                    NotificationEvent.STATUS_UPDATED);
            case "REC_REVISION", "REVISION", "RPS_RETURNED" -> notifyProponent(proposal,
                    "Your proposal \"" + title
                            + "\" has been returned for revision. Please review the remarks "
                            + "and make the necessary corrections."
                            + remarksSentence(proposal),
                    "Proposal Returned for Revision", NotificationTypes.REVISION,
                    NotificationEvent.STATUS_UPDATED);
            case "REC_REJECTED", "REJECTED" -> notifyProponent(proposal,
                    "Your proposal \"" + title + "\" has been rejected."
                            + remarksSentence(proposal),
                    "Proposal Rejected", NotificationTypes.REJECTION,
                    NotificationEvent.STATUS_UPDATED);
            case "APPROVED" -> {
                notifyProponent(proposal,
                        "Your proposal \"" + title + "\" has been approved for implementation."
                                + specialOrderSentence(proposal),
                        "Proposal Approved for Implementation", NotificationTypes.APPROVAL,
                        NotificationEvent.STATUS_UPDATED);
                if (!Boolean.FALSE.equals(proposal.getNeedsBudget())) {
                    notificationService.notifyRoles(OVCAF,
                            "A proposal is approved and ready for budget endorsement: \"" + title + "\"",
                            "Budget Endorsement Required", NotificationTypes.FUNDING, proposalId,
                            NotificationEvent.STATUS_UPDATED);
                }
            }
            case "READY_FOR_RELEASE" -> {
                notificationService.notifyRoles(OVCAF,
                        "Budget processing requested for proposal: \"" + title + "\"",
                        "Budget Processing Request", NotificationTypes.FUNDING, proposalId,
                        NotificationEvent.STATUS_UPDATED);
                notifyProponent(proposal,
                        "The budget for your proposal \"" + title
                                + "\" has been approved. It is awaiting the release of funds "
                                + "before implementation begins.",
                        "Budget Approved", NotificationTypes.FUNDING,
                        NotificationEvent.STATUS_UPDATED);
            }
            case "RELEASED" -> {
                notifyProponent(proposal,
                        "Implementation funds have been successfully released for your project: \""
                                + title + "\"",
                        "Funds Released", NotificationTypes.FUNDING,
                        NotificationEvent.STATUS_UPDATED);
                notificationService.notifyRoles(RPS,
                        "Funds released. Ready for implementation monitoring: \"" + title + "\"",
                        "Implementation Monitoring", NotificationTypes.PROPOSAL_UPDATE, proposalId,
                        NotificationEvent.STATUS_UPDATED);
            }
            case "RETURNED" -> {
                notifyProponent(proposal,
                        "The budget request for your proposal \"" + title
                                + "\" was returned. Please review the remarks and resubmit."
                                + remarksSentence(proposal),
                        "Budget Request Returned", NotificationTypes.FUNDING,
                        NotificationEvent.STATUS_UPDATED);
                notificationService.notifyRoles(RPS,
                        "Budget release was returned by Finance Office for proposal: \"" + title + "\"",
                        "Budget Request Returned", NotificationTypes.FUNDING, proposalId,
                        NotificationEvent.STATUS_UPDATED);
            }
            default -> {
                // A status with no recipient rule (for example DRAFT or COMPLETED) raises
                // nothing. Terminal project completion is raised by the reporting service.
            }
        }
    }

    /**
     * The Chancellor's Office returning a proposal to OVCRIGE is not an REC approval,
     * even though the workflow reuses the {@code REC_APPROVED} status. This dedicated
     * message keeps the recipient from being told something that did not happen, and it
     * deliberately does not notify the proponent.
     */
    public void notifyOvcrigeReturned(Proposal proposal, String remarks) {
        if (proposal == null) {
            return;
        }
        String message = "The Chancellor's Office returned proposal \""
                + proposal.getProjectTitle() + "\" to OVCRIGE for revision.";
        if (remarks != null && !remarks.isBlank()) {
            message += " Remarks: " + remarks;
        }
        notificationService.notifyRoles(OVCRIGE, message,
                "Proposal Returned by the Chancellor's Office",
                NotificationTypes.REVISION, proposal.getId(),
                NotificationEvent.STATUS_UPDATED);
    }

    /**
     * OVCAF approving the budget is one event with four audiences: the proponent is told
     * the release is awaited, ORPS is told the release is next, and the Chancellor's
     * Office is told the budget it issued the SO for has cleared. The shared dedupe key
     * makes a retried endorsement produce no second row for any of them.
     */
    public void notifyOvcafBudgetApproved(Proposal proposal) {
        if (proposal == null) {
            return;
        }
        Long proposalId = proposal.getId();
        String title = proposal.getProjectTitle();
        NotificationEvent event =
                NotificationEvent.STATUS_UPDATED.withDedupeKey("ovcaf:budget-approved:" + proposalId);

        notifyProponent(proposal,
                "The budget for your proposal \"" + title
                        + "\" has been approved by OVCAF and is awaiting fund release. "
                        + "The project becomes approved for implementation once the release is recorded.",
                "Budget Approved", NotificationTypes.APPROVAL, event);
        notificationService.notifyRoles(RPS,
                "Budget approved by OVCAF; awaiting fund release: \"" + title + "\"",
                "Budget Approved", NotificationTypes.PROPOSAL_UPDATE, proposalId, event);
        notificationService.notifyRoles(OC,
                "OVCAF approved the budget for proposal \"" + title
                        + "\". It is now ready for fund release.",
                "Budget Approved by OVCAF", NotificationTypes.APPROVAL, proposalId, event);
    }

    /**
     * Tells the Proponent that their proposal was returned for revision.
     *
     * <p>This is the Proponent-facing half of a return: the responsible office is still
     * named (so the message does not read as an anonymous decision) and the remarks the
     * office recorded travel with it. ORPS is told separately through the
     * {@code RETURNED_TO_RPS} recipient rule, so neither side misses the event. The
     * returned-at timestamp is part of the dedupe key: a retried request within the same
     * return writes one row, while a later return of the same proposal is a new event.
     */
    public void notifyProponentRevisionReturned(Proposal proposal, String office) {
        if (proposal == null) {
            return;
        }
        String source = office == null || office.isBlank() ? "ORPS" : office.trim();
        String dedupeKey = "proposal:" + proposal.getId()
                + ":revision-returned:" + proposal.getReturnedAt();
        notifyProponent(proposal,
                "Your proposal \"" + proposal.getProjectTitle()
                        + "\" has been returned for revision by " + source
                        + ". ORPS is reviewing it and will forward the request with the"
                        + " revision deadline." + remarksSentence(proposal),
                "Proposal Returned for Revision", NotificationTypes.REVISION,
                NotificationEvent.STATUS_UPDATED.withDedupeKey(dedupeKey));
    }

    /**
     * OVCAF returning a proposal is a hand-off to ORPS, not a proponent decision. The
     * remarks the reviewer wrote travel with the notification so ORPS can act on them.
     */
    public void notifyOvcafReturned(Proposal proposal, String remarks) {
        if (proposal == null) {
            return;
        }
        String message = "OVCAF returned proposal \"" + proposal.getProjectTitle()
                + "\" for budget revision. It awaits your review and forwarding to the proponent.";
        if (remarks != null && !remarks.isBlank()) {
            message += " Remarks: " + truncateRemarks(remarks);
        }
        notificationService.notifyRoles(RPS, message,
                "Proposal Returned by OVCAF",
                NotificationTypes.REVISION, proposal.getId(),
                NotificationEvent.STATUS_UPDATED.withDedupeKey(
                        "ovcaf:returned:" + proposal.getId()));
    }

    /**
     * OVCRIGE rejecting a proposal is a hand-off to ORPS, not a final decision. The
     * Proponent is deliberately not notified here: ORPS reviews the rejection and
     * forwards it through the existing rejection workflow.
     */
    public void notifyRpsRejectionReturned(Proposal proposal, String remarks) {
        notifyRpsRejectionReturned(proposal, remarks, "OVCRIGE");
    }

    /**
     * The same hand-off for any reviewing office. OVCAF uses this too, so ORPS is told
     * which office rejected the proposal and the reason it recorded.
     */
    public void notifyRpsRejectionReturned(Proposal proposal, String remarks, String office) {
        if (proposal == null) {
            return;
        }
        String source = office == null || office.isBlank() ? "a reviewing office" : office;
        String message = source + " rejected proposal \""
                + proposal.getProjectTitle() + "\" and returned it to ORPS for processing.";
        if (remarks != null && !remarks.isBlank()) {
            message += " Reason: " + truncateRemarks(remarks);
        }
        NotificationEvent event = "OVCAF".equalsIgnoreCase(source)
                ? NotificationEvent.STATUS_UPDATED.withDedupeKey(
                        "ovcaf:rejected:" + proposal.getId())
                : NotificationEvent.STATUS_UPDATED;
        notificationService.notifyRoles(RPS, message,
                "Proposal Rejected by " + source,
                NotificationTypes.REJECTION, proposal.getId(),
                event);
    }

    /** The reason text kept in a notification, capped so one row stays readable. */
    private static String truncateRemarks(String remarks) {
        String trimmed = remarks.trim();
        return trimmed.length() > 2000 ? trimmed.substring(0, 2000) + "..." : trimmed;
    }

    /** " Special Order <number> has been issued." or '' when no SO is recorded. */
    private static String specialOrderSentence(Proposal proposal) {
        String soNumber = proposal == null ? null : proposal.getSoNumber();
        if (soNumber == null || soNumber.isBlank()) {
            return "";
        }
        return " Special Order " + soNumber.trim() + " has been issued.";
    }

    /** " Remarks: ..." or '' when the proposal carries no remarks. */
    private static String remarksSentence(Proposal proposal) {
        String remarks = proposal == null ? null : proposal.getRemarks();
        if (remarks == null || remarks.isBlank()) {
            return "";
        }
        String trimmed = remarks.trim();
        return " Remarks: " + (trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed);
    }

    private void notifyProponent(Proposal proposal, String message, String title, String type,
            NotificationEvent event) {
        User proponent = proposal.getProponent();
        if (proponent == null) {
            return;
        }
        notificationService.notifyUsers(List.of(proponent), message, title, type,
                proposal.getId(), event, UserRoles.PROPONENT);
    }
}
