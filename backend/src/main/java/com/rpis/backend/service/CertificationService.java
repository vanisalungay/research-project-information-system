package com.rpis.backend.service;

import com.rpis.backend.dto.CertificationResponse;
import com.rpis.backend.model.Certification;
import com.rpis.backend.model.EndorsementRequest;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.CertificationRepository;
import com.rpis.backend.repository.EndorsementRequestRepository;
import com.rpis.backend.storage.UploadStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * The Proponent's Certifications section.
 *
 * <p>An entry is created automatically when a Dean issues an official endorsement
 * letter - the Proponent never uploads it - and it is owned by the Proponent it was
 * issued to. Issuance is idempotent: the entry is keyed by the endorsement request,
 * so a retried or concurrent approval reuses the entry it already created instead of
 * adding a second one.
 */
@Service
@RequiredArgsConstructor
public class CertificationService {

    /** Label of the endorsement letter document type. */
    public static final String LABEL_ENDORSEMENT_LETTER = "Endorsement Letter";

    private final CertificationRepository certificationRepository;
    private final EndorsementRequestRepository endorsementRequestRepository;
    private final UploadStorage uploadStorage;

    /** The authenticated Proponent's own certification documents, newest first. */
    public List<CertificationResponse> listForProponent(Long proponentId) {
        return certificationRepository.findByProponentIdOrderByCreatedAtDesc(proponentId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Creates the certification entry of an issued endorsement letter, or returns the
     * one that already exists. Called inside the approval transaction, after the PDF
     * has been written, so an approved request always has its certification.
     */
    @Transactional
    public Certification issueEndorsementLetter(EndorsementRequest request) {
        return certificationRepository.findByEndorsementRequestId(request.getId())
                .orElseGet(() -> certificationRepository.save(buildEntry(request)));
    }

    private Certification buildEntry(EndorsementRequest request) {
        Certification certification = new Certification();
        certification.setProponent(request.getProponent());
        certification.setEndorsementRequestId(request.getId());
        certification.setProposalId(request.getProposalId());
        certification.setDocumentType(Certification.DOCUMENT_TYPE_ENDORSEMENT_LETTER);
        certification.setTitle("Endorsement Letter - " + request.getProjectTitle());
        certification.setFileName(request.getLetterFileName());
        certification.setFilePath(request.getLetterFilePath());
        certification.setEndorsementDate(request.getEndorsementDate());
        certification.setIssuingDeanName(request.getDecidedByName());
        certification.setStatus(Certification.STATUS_ISSUED);
        return certification;
    }

    /**
     * Loads one certification for the caller, enforcing that it is the caller's own
     * document. Only the owning Proponent, the Dean who issued it and the RPS staff /
     * administrator roles may read it.
     */
    public Certification requireReadable(Long certificationId, Long requesterId,
            String requesterRole) {
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Certification not found."));

        Long ownerId = certification.getProponent() != null
                ? certification.getProponent().getId()
                : null;
        boolean owner = ownerId != null && ownerId.equals(requesterId);
        boolean staff = UserRoles.RPS_ADMIN.equalsIgnoreCase(requesterRole)
                || UserRoles.RPS_STAFF.equalsIgnoreCase(requesterRole);
        boolean issuingDean = false;
        if (certification.getEndorsementRequestId() != null) {
            // The Dean who issued the letter may re-read it; anyone else is refused.
            issuingDean = isIssuingDean(certification.getEndorsementRequestId(), requesterId,
                    requesterRole);
        }
        if (!owner && !staff && !issuingDean) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to access this certification document.");
        }
        return certification;
    }

    /** True when the caller is the Dean who decided the endorsement request. */
    private boolean isIssuingDean(Long endorsementRequestId, Long requesterId, String requesterRole) {
        if (!UserRoles.DEAN.equalsIgnoreCase(requesterRole)) {
            return false;
        }
        return endorsementRequestRepository.findById(endorsementRequestId)
                .map(EndorsementRequest::getDecidedByUserId)
                .filter(requesterId::equals)
                .isPresent();
    }

    /** Read model of one certification document. */
    public CertificationResponse toResponse(Certification certification) {
        CertificationResponse response = new CertificationResponse();
        response.setId(certification.getId());
        response.setDocumentType(certification.getDocumentType());
        response.setDocumentTypeLabel(labelOf(certification.getDocumentType()));
        response.setTitle(certification.getTitle());
        response.setFileName(certification.getFileName());
        response.setEndorsementDate(certification.getEndorsementDate());
        response.setIssuingDeanName(certification.getIssuingDeanName());
        response.setStatus(certification.getStatus());
        response.setCreatedAt(certification.getCreatedAt());
        response.setEndorsementRequestId(certification.getEndorsementRequestId());
        response.setProposalId(certification.getProposalId());
        response.setDocumentAvailable(resolveStoredFile(certification) != null);
        return response;
    }

    private static String labelOf(String documentType) {
        if (Certification.DOCUMENT_TYPE_ENDORSEMENT_LETTER.equals(documentType)) {
            return LABEL_ENDORSEMENT_LETTER;
        }
        return documentType == null ? "Document" : documentType;
    }

    /**
     * Resolves the stored file of a certification. Only paths inside the uploads
     * directory are served, so a corrupted value can never reach anything else.
     */
    public StoredDocument loadDocument(Certification certification) {
        UploadStorage.StoredUpload stored = resolveStoredFile(certification);
        if (stored == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The stored certification document is no longer available.");
        }
        String fileName = certification.getFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = stored.fileName();
        }
        return new StoredDocument(stored, fileName);
    }

    /**
     * Opens the stored file of a certification. The storage layer resolves the value
     * inside its own root, so a corrupted column value can never reach anything else.
     */
    private UploadStorage.StoredUpload resolveStoredFile(Certification certification) {
        String stored = certification.getFilePath();
        if (stored == null || stored.isBlank()) {
            return null;
        }
        return uploadStorage.open(stored).orElse(null);
    }

    /** A stored certification document: the readable bytes and the name to offer. */
    public record StoredDocument(UploadStorage.StoredUpload stored, String fileName) {
    }

    /**
     * Points the existing certification entry at the letter's current version after an
     * authorized reissue. The same row is kept, so the Proponent never ends up with
     * two certifications for one endorsement.
     */
    @Transactional
    public void refreshDocument(EndorsementRequest request) {
        certificationRepository.findByEndorsementRequestId(request.getId())
                .ifPresent(certification -> {
                    certification.setFileName(request.getLetterFileName());
                    certification.setFilePath(request.getLetterFilePath());
                    certification.setEndorsementDate(request.getEndorsementDate());
                    certification.setIssuingDeanName(request.getDecidedByName());
                    certificationRepository.save(certification);
                });
    }

    /** Records on the certification which proposal the endorsement was used for. */
    @Transactional
    public void linkProposal(EndorsementRequest request) {
        certificationRepository.findByEndorsementRequestId(request.getId())
                .ifPresent(certification -> {
                    certification.setProposalId(request.getProposalId());
                    certificationRepository.save(certification);
                });
    }
}
