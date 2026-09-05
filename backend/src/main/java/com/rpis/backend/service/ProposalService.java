package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.*;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationCycleService applicationCycleService;

    // ========================
    // PROPOSAL ID AND REVISION FORMAT
    // ========================
    // Proposal ID: 3-digit continuous counter (001, 002, 003, ... 139)
    // Revision number: 2-digit format (00 = initial, 01 = first revision, 02 =
    // second, etc.)
    // Document format: MSUN-ORPS-DA-{proposalId}-{year}-REV{revisionNo}
    // Example: MSUN-ORPS-DA-001-2026-REV00

    /**
     * Generate the next proposal code (3-digit continuous counter).
     * This counter never resets across years.
     */
    private String generateNextProposalCode() {
        String maxCode = proposalRepository.findMaxProposalCode();
        if (maxCode == null || maxCode.isEmpty()) {
            return "001"; // First proposal
        }
        try {
            int nextNum = Integer.parseInt(maxCode) + 1;
            return String.format("%03d", nextNum);
        } catch (NumberFormatException e) {
            return "001"; // Fallback if parsing fails
        }
    }

    /**
     * Generate the document ID in format:
     * MSUN-ORPS-DA-{proposalCode}-{year}-REV{revisionNo}
     * 
     * @param proposalCode   3-digit proposal code (e.g., "001")
     * @param revisionNumber revision number (e.g., 0 for initial, 1 for first
     *                       revision)
     * @return formatted document ID
     */
    private String generateDocumentId(String proposalCode, int revisionNumber) {
        int year = Year.now().getValue();
        return String.format("MSUN-ORPS-DA-%s-%d-REV%02d", proposalCode, year, revisionNumber);
    }

    public List<Proposal> getAllProposals() {
        return proposalRepository.findAll();
    }

    public List<Proposal> getProposalsByStatus(String status) {
        return proposalRepository.findByStatus(status.toUpperCase());
    }

    public List<Proposal> getProposalsByProponent(Long proponentId) {
        User proponent = userRepository.findById(proponentId)
                .orElseThrow(() -> new IllegalArgumentException("Proponent not found: " + proponentId));
        return proposalRepository.findByProponent(proponent);
    }

    public List<Proposal> getProposalsByStatusIn(List<String> statuses) {
        return proposalRepository.findByStatusIn(statuses);
    }

    public Proposal getProposalById(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
    }

    @Transactional
    public Proposal saveProposal(ProposalRequest request, Long proposalId) {
        Proposal proposal;
        boolean isNewProposal = (proposalId == null);
        boolean isRevisionResubmission = false;

        if (proposalId != null) {
            proposal = getProposalById(proposalId);
            // Check if this is a revision resubmission (proposal was returned and is now
            // being resubmitted)
            if ("RPS_RETURNED".equals(proposal.getStatus()) ||
                    "REC_REVISION".equals(proposal.getStatus()) ||
                    "REVISION".equals(proposal.getStatus())) {
                isRevisionResubmission = true;
            }
        } else {
            proposal = new Proposal();
        }

        // Application Cycle gatekeeper (server-side enforcement): new proposals and
        // final submissions are only accepted while an application cycle is active.
        // Revision resubmissions belong to the review lifecycle of an already
        // submitted proposal and remain allowed outside an active cycle.
        boolean isSubmission = request.getStatus() != null
                && "SUBMITTED".equalsIgnoreCase(request.getStatus());
        if ((isNewProposal || isSubmission) && !isRevisionResubmission
                && !applicationCycleService.hasActiveCycle()) {
            throw new IllegalStateException(
                    "Submissions are currently closed. There is no active application cycle at this time.");
        }

        // Generate proposal code and document ID for new proposals
        if (isNewProposal) {
            String proposalCode = generateNextProposalCode();
            proposal.setProposalCode(proposalCode);
            proposal.setRevisionNumber(0);
            proposal.setDocumentId(generateDocumentId(proposalCode, 0));
        }
        // Increment revision number and regenerate document ID for revision
        // resubmissions
        else if (isRevisionResubmission) {
            int newRevisionNumber = (proposal.getRevisionNumber() != null ? proposal.getRevisionNumber() : 0) + 1;
            proposal.setRevisionNumber(newRevisionNumber);
            proposal.setDocumentId(generateDocumentId(proposal.getProposalCode(), newRevisionNumber));
        }

        // Proponent
        if (request.getProponentId() != null) {
            User proponent = userRepository.findById(request.getProponentId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Proponent not found: " + request.getProponentId()));
            proposal.setProponent(proponent);
        }

        // Standard properties
        proposal.setProgramTitle(request.getProgramTitle());
        proposal.setProjectTitle(request.getProjectTitle());
        proposal.setProjectLeader(request.getProjectLeader());
        proposal.setDuration(request.getDuration());
        proposal.setCollege(request.getCollege());
        proposal.setAddress(request.getAddress());
        proposal.setCooperatingAgencies(request.getCooperatingAgencies());
        proposal.setResearchType(request.getResearchType());
        proposal.setInnovationGoals(request.getInnovationGoals());
        proposal.setSectorRelevance(request.getSectorRelevance());
        proposal.setSdg(request.getSdg());
        proposal.setExecutiveSummary(request.getExecutiveSummary());
        proposal.setRationale(request.getRationale());
        proposal.setFramework(request.getFramework());
        proposal.setObjectivesGeneral(request.getObjectivesGeneral());
        proposal.setObjectivesSpecific(request.getObjectivesSpecific());
        proposal.setReview(request.getReview());
        proposal.setReviewFileName(request.getReviewFileName());
        proposal.setMethodology(request.getMethodology());
        proposal.setRoadmapFileName(request.getRoadmapFileName());
        proposal.setTechnologyTrl(request.getTechnologyTrl());
        proposal.setOutputs(request.getOutputs());
        proposal.setOutcomes(request.getOutcomes());
        proposal.setImpactEconomic(request.getImpactEconomic());
        proposal.setImpactSocial(request.getImpactSocial());
        proposal.setBeneficiaries(request.getBeneficiaries());
        proposal.setBeneficiariesFileName(request.getBeneficiariesFileName());
        proposal.setSustainability(request.getSustainability());
        proposal.setGadScore(request.getGadScore());
        proposal.setGadFileName(request.getGadFileName());
        proposal.setRisks(request.getRisks());
        proposal.setOtherProjectsNumber(request.getOtherProjectsNumber());

        // Status
        if (request.getStatus() != null) {
            proposal.setStatus(request.getStatus().toUpperCase());
        } else if (proposal.getStatus() == null) {
            proposal.setStatus("DRAFT");
        }

        // Dates parsing
        try {
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                proposal.setStartDate(LocalDate.parse(request.getStartDate()));
            }
        } catch (DateTimeParseException e) {
            // Ignore format issues or log
        }
        try {
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                proposal.setEndDate(LocalDate.parse(request.getEndDate()));
            }
        } catch (DateTimeParseException e) {
            // Ignore
        }

        // References formatting as text
        if (request.getReferences() != null) {
            StringBuilder sb = new StringBuilder();
            for (ProposalRequest.ReferenceDTO ref : request.getReferences()) {
                if (ref.getAuthor() != null || ref.getTitle() != null || ref.getYear() != null) {
                    sb.append(ref.getAuthor()).append(". (")
                            .append(ref.getYear()).append("). ")
                            .append(ref.getTitle()).append(".\n");
                }
            }
            proposal.setReferencesText(sb.toString().trim());
        }

        // Clear existing child lists to overwrite (cascade all + orphan removal takes
        // care of orphans)
        proposal.getSites().clear();
        proposal.getLogFrames().clear();
        proposal.getPersonnel().clear();
        proposal.getBudget().clear();
        proposal.getOtherProjects().clear();
        proposal.getPriorityAgendas().clear();
        proposal.getLimitations().clear();

        // Save parent proposal to generate ID if new
        proposal = proposalRepository.save(proposal);

        // Sites
        if (request.getSites() != null) {
            for (ProposalRequest.SiteDTO s : request.getSites()) {
                ProposalSite site = new ProposalSite(null, proposal, s.getCountry(), s.getRegion(), s.getProvince(),
                        s.getDistrict(), s.getMunicipality(), s.getBarangay());
                proposal.getSites().add(site);
            }
        }

        // LogFrames
        if (request.getLogFrame() != null) {
            for (ProposalRequest.LogFrameDTO lf : request.getLogFrame()) {
                ProposalLogFrame logFrame = new ProposalLogFrame(null, proposal, lf.getOutcome(), lf.getOutput());
                proposal.getLogFrames().add(logFrame);
            }
        }

        // Personnel
        if (request.getPersonnel() != null) {
            for (ProposalRequest.PersonnelDTO p : request.getPersonnel()) {
                ProposalPersonnel personnel = new ProposalPersonnel(null, proposal, p.getPosition(), p.getTime(),
                        p.getResponsibilities());
                proposal.getPersonnel().add(personnel);
            }
        }

        // Budget
        if (request.getBudget() != null) {
            for (ProposalRequest.BudgetDTO b : request.getBudget()) {
                ProposalBudget budget = new ProposalBudget(null, proposal, b.getAgency(), b.getPs(), b.getMooe(),
                        b.getEo(), b.getTotal());
                proposal.getBudget().add(budget);
            }
        }

        // Other Projects
        if (request.getOtherProjects() != null) {
            for (ProposalRequest.OtherProjectDTO op : request.getOtherProjects()) {
                ProposalOtherProject otherProject = new ProposalOtherProject(null, proposal, op.getTitle(),
                        op.getAgency(), op.getInvolvement());
                proposal.getOtherProjects().add(otherProject);
            }
        }

        // Limitations
        if (request.getLimitationsTable() != null) {
            for (ProposalRequest.LimitationDTO l : request.getLimitationsTable()) {
                ProposalLimitation limitation = new ProposalLimitation(null, proposal, l.getLimitation(),
                        l.getRemarks());
                proposal.getLimitations().add(limitation);
            }
        }

        // Priority Agendas
        if (request.getPriorityAgenda() != null) {
            for (Map.Entry<String, ProposalRequest.PriorityAgendaDTO> entry : request.getPriorityAgenda().entrySet()) {
                ProposalRequest.PriorityAgendaDTO val = entry.getValue();
                ProposalPriorityAgenda agenda = new ProposalPriorityAgenda(null, proposal, entry.getKey(),
                        val.getSelected(), val.getValue());
                proposal.getPriorityAgendas().add(agenda);
            }
        }

        // Save again to cascade save child collections
        proposal = proposalRepository.save(proposal);

        // Send system notification for submissions
        notifySubscribedRoles(
                proposal,
                "Proposal Update: " + proposal.getProjectTitle());

        return proposal;
    }

    @Transactional
    public Proposal updateProposalStatus(Long id, String status) {
        Proposal proposal = getProposalById(id);
        proposal.setStatus(status.toUpperCase());
        proposal = proposalRepository.save(proposal);

        // Dispatch notifications to the next user level and the proponent
        notifySubscribedRoles(proposal, null);

        return proposal;
    }

    /**
     * Save a proposal entity directly (used when fields are set manually on the
     * entity).
     * Also dispatches notifications based on the current status.
     */
    @Transactional
    public Proposal saveProposalDirect(Proposal proposal) {
        proposal = proposalRepository.save(proposal);
        notifySubscribedRoles(proposal, null);
        return proposal;
    }

    @Transactional
    public Proposal updateReviewerInfo(Long id, String reviewedBy, String reviewedByPosition) {
        Proposal proposal = getProposalById(id);
        proposal.setReviewedBy(reviewedBy);
        proposal.setReviewedByPosition(reviewedByPosition);
        return proposalRepository.save(proposal);
    }

    @Transactional
    public Proposal returnForRevision(Long id, String remarks) {
        Proposal proposal = getProposalById(id);
        proposal.setStatus("RPS_RETURNED");
        proposal.setRemarks(remarks);
        proposal = proposalRepository.save(proposal);

        // Notify proponent about the revision request
        notifySubscribedRoles(proposal, null);

        return proposal;
    }

    @Transactional
    public void deleteProposal(Long id) {
        Proposal proposal = getProposalById(id);
        proposalRepository.delete(proposal);
    }

    private void notifySubscribedRoles(Proposal proposal, String message) {
        List<User> allUsers = userRepository.findAll();
        String status = proposal.getStatus();

        for (User user : allUsers) {
            String role = user.getRole();

            switch (status) {
                // Newly submitted proposal -> notify RPS staff/admins to check
                case "SUBMITTED":
                    if ("RPS_ADMIN".equals(role) || "RPS_STAFF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "New proposal submitted for review: \"" + proposal.getProjectTitle() + "\"",
                                "New Proposal Submitted",
                                "PROPOSAL_UPDATE",
                                proposal.getId());
                    }
                    break;

                // RPS endorsed it to OVCRIGE -> notify OVCRIGE to review & coordinate
                case "ENDORSED":
                    if ("OVCRIGE".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal has been endorsed to OVCRIGE: \"" + proposal.getProjectTitle() + "\"",
                                "Proposal Endorsed",
                                "ENDORSEMENT",
                                proposal.getId());
                    }
                    break;

                // OVCRIGE forwarded to REC -> notify REC to conduct technical/merit evaluation
                case "UNDER_REVIEW":
                    if ("REC".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "Proposal awaiting REC review and evaluation: \"" + proposal.getProjectTitle() + "\"",
                                "Review Request",
                                "REVIEW_REQUEST",
                                proposal.getId());
                    }
                    break;

                // REC approved & recommended -> notify OVCRIGE to prepare chancellor final
                // approval list
                case "REC_APPROVED":
                    if ("OVCRIGE".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "REC has evaluated and recommended proposal for approval: \""
                                        + proposal.getProjectTitle() + "\"",
                                "Proposal Approved by REC",
                                "APPROVAL",
                                proposal.getId());
                    }
                    break;

                // OVCRIGE forwarded to OVCAF -> notify OVCAF to review & endorse
                case "FOR_OVCAF_APPROVAL":
                    if ("OVCAF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal is ready for your review and endorsement: \"" + proposal.getProjectTitle()
                                        + "\"",
                                "Proposal for OVCAF Review",
                                "ENDORSEMENT",
                                proposal.getId());
                    }
                    break;

                // OVCAF endorsed to OC -> notify OC (Chancellor) for final approval
                case "FOR_OC_APPROVAL":
                    if ("OC".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal is ready for your final approval: \"" + proposal.getProjectTitle() + "\"",
                                "Final Approval Required",
                                "APPROVAL",
                                proposal.getId());
                    }
                    break;

                // REC returned for revision -> notify proponent to revise
                case "REC_REVISION":
                case "REVISION":
                    // RPS returned for revision -> notify proponent to revise
                case "RPS_RETURNED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Your proposal \"" + proposal.getProjectTitle()
                                        + "\" has been returned for revision. Please check feedback.",
                                "Revision Required",
                                "REVISION",
                                proposal.getId());
                    }
                    break;

                // REC rejected or Chancellor rejected -> notify proponent
                case "REC_REJECTED":
                case "REJECTED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Your proposal \"" + proposal.getProjectTitle() + "\" has been rejected.",
                                "Proposal Rejected",
                                "REJECTION",
                                proposal.getId());
                    }
                    break;

                // Final approved by Chancellor -> notify proponent & OVCAF (Finance) to
                // authorize budget
                case "APPROVED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Congratulations! Your proposal \"" + proposal.getProjectTitle()
                                        + "\" has received final approval by the Chancellor.",
                                "Proposal Approved",
                                "APPROVAL",
                                proposal.getId());
                    } else if ("OVCAF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal is approved and ready for budget endorsement: \""
                                        + proposal.getProjectTitle() + "\"",
                                "Budget Endorsement Required",
                                "FUNDING",
                                proposal.getId());
                    }
                    break;

                // Budget ready for release -> notify OVCAF (Finance)
                case "READY_FOR_RELEASE":
                    if ("OVCAF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "Budget processing requested for proposal: \"" + proposal.getProjectTitle() + "\"",
                                "Budget Processing Request",
                                "FUNDING",
                                proposal.getId());
                    }
                    break;

                // Funds released -> notify proponent & RPS (RII/DARES) to monitor
                // implementation
                case "RELEASED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Implementation funds have been successfully released for your project: \""
                                        + proposal.getProjectTitle() + "\"",
                                "Funds Released",
                                "FUNDING",
                                proposal.getId());
                    } else if ("RPS_ADMIN".equals(role) || "RPS_STAFF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "Funds released. Ready for implementation monitoring: \"" + proposal.getProjectTitle()
                                        + "\"",
                                "Implementation Monitoring",
                                "PROPOSAL_UPDATE",
                                proposal.getId());
                    }
                    break;

                // Returned by OVCAF -> notify proponent & RPS
                case "RETURNED":
                    if (proposal.getProponent() != null && proposal.getProponent().getId().equals(user.getId())) {
                        notificationService.createNotification(
                                user.getId(),
                                "Budget release request for your proposal \"" + proposal.getProjectTitle()
                                        + "\" was returned.",
                                "Budget Request Returned",
                                "REVISION",
                                proposal.getId());
                    } else if ("RPS_ADMIN".equals(role) || "RPS_STAFF".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "Budget release was returned by Finance Office for proposal: \""
                                        + proposal.getProjectTitle() + "\"",
                                "Budget Request Returned",
                                "REVISION",
                                proposal.getId());
                    }
                    break;
            }
        }
    }
}
