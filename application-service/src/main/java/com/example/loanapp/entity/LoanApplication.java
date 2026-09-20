package com.example.loanapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "human_readable_id", unique = true, nullable = false)
    private String humanReadableId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "config_snapshot_id", nullable = false)
    private Long configSnapshotId;

    @Column(name = "risk_rating")
    private String riskRating;

    @Column(name = "risk_rating_computed_date")
    private LocalDateTime riskRatingComputedDate;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "created_user")
    private String createdUser;

    @Column(name = "proposal_name")
    private String proposalName;

    @Column(name = "loan_amount", precision = 18, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "proposal_description", length = 2000)
    private String proposalDescription;

    public Long getId() { return id; }
    public String getHumanReadableId() { return humanReadableId; }
    public void setHumanReadableId(String v) { this.humanReadableId = v; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus v) { this.status = v; }
    public Long getConfigSnapshotId() { return configSnapshotId; }
    public void setConfigSnapshotId(Long v) { this.configSnapshotId = v; }
    public String getRiskRating() { return riskRating; }
    public void setRiskRating(String v) { this.riskRating = v; }
    public LocalDateTime getRiskRatingComputedDate() { return riskRatingComputedDate; }
    public void setRiskRatingComputedDate(LocalDateTime v) { this.riskRatingComputedDate = v; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate v) { this.createdDate = v; }
    public String getCreatedUser() { return createdUser; }
    public void setCreatedUser(String v) { this.createdUser = v; }
    public String getProposalName() { return proposalName; }
    public void setProposalName(String v) { this.proposalName = v; }
    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal v) { this.loanAmount = v; }
    public String getProposalDescription() { return proposalDescription; }
    public void setProposalDescription(String v) { this.proposalDescription = v; }
}
