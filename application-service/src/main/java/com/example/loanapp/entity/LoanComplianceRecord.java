package com.example.loanapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "loan_compliance_record")
public class LoanComplianceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "aml_check_status", length = 20)
    private String amlCheckStatus;

    @Column(name = "aml_check_date", length = 20)
    private String amlCheckDate;

    @Column(name = "pep_status", length = 20)
    private String pepStatus;

    @Column(name = "sanctions_status", length = 20)
    private String sanctionsStatus;

    public LoanComplianceRecord() {}
    public LoanComplianceRecord(Long applicationId) { this.applicationId = applicationId; }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public String getAmlCheckStatus() { return amlCheckStatus; }
    public void setAmlCheckStatus(String v) { this.amlCheckStatus = v; }
    public String getAmlCheckDate() { return amlCheckDate; }
    public void setAmlCheckDate(String v) { this.amlCheckDate = v; }
    public String getPepStatus() { return pepStatus; }
    public void setPepStatus(String v) { this.pepStatus = v; }
    public String getSanctionsStatus() { return sanctionsStatus; }
    public void setSanctionsStatus(String v) { this.sanctionsStatus = v; }
}
