package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalRequest;
import com.rpis.backend.model.*;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

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

    public Proposal getProposalById(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
    }

    @Transactional
    public Proposal saveProposal(ProposalRequest request, Long proposalId) {
        Proposal proposal;
        if (proposalId != null) {
            proposal = getProposalById(proposalId);
        } else {
            proposal = new Proposal();
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

        // Notify proponent of status updates
        if (proposal.getProponent() != null) {
            notificationService.createNotification(
                    proposal.getProponent().getId(),
                    "Your proposal \"" + proposal.getProjectTitle() + "\" status is now: " + status);
        }
        return proposal;
    }

    private void notifySubscribedRoles(Proposal proposal, String message) {

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {

            String role = user.getRole();

            switch (proposal.getStatus()) {

                // Newly submitted proposal
                case "SUBMITTED":
                    if ("RPS_ADMIN".equals(role) || "RPS_STAFF".equals(role)) {
                        notificationService.createNotification(user.getId(), message);
                    }
                    break;

                // RPS endorsed it to OVCRIGE
                case "ENDORSED":
                    if ("OVCRIGE".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "A proposal has been endorsed to OVCRIGE: "
                                        + proposal.getProjectTitle());
                    }
                    break;

                // OVCRIGE forwarded to REC
                case "UNDER_REVIEW":
                    if ("REC".equals(role)) {
                        notificationService.createNotification(
                                user.getId(),
                                "Proposal awaiting REC review: "
                                        + proposal.getProjectTitle());
                    }
                    break;

                // REC approved
                case "APPROVED":
                    if (proposal.getProponent() != null) {
                        notificationService.createNotification(
                                proposal.getProponent().getId(),
                                "Your proposal \"" +
                                        proposal.getProjectTitle() +
                                        "\" has been approved.");
                    }
                    break;

                // REC returned for revision
                case "REVISION":
                    if (proposal.getProponent() != null) {
                        notificationService.createNotification(
                                proposal.getProponent().getId(),
                                "Your proposal \"" +
                                        proposal.getProjectTitle() +
                                        "\" requires revision.");
                    }
                    break;

                // REC rejected
                case "REJECTED":
                    if (proposal.getProponent() != null) {
                        notificationService.createNotification(
                                proposal.getProponent().getId(),
                                "Your proposal \"" +
                                        proposal.getProjectTitle() +
                                        "\" has been rejected.");
                    }
                    break;
            }
        }
    }
}
