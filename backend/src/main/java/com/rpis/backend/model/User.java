package com.rpis.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    // NEW FIELD
    private String departmentOffice;

    // NEW FIELD
    private LocalDate dateRegistered;

    // NEW FIELD
    private Boolean emailVerified = false;

    private String contactNumber;
    private String specialization;

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