package com.example.loanapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "loan_employment_detail")
public class LoanEmploymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "employment_status", length = 30)
    private String employmentStatus;

    @Column(name = "employer_name", length = 200)
    private String employerName;

    @Column(name = "gross_annual_income", precision = 18, scale = 2)
    private BigDecimal grossAnnualIncome;

    @Column(name = "net_monthly_income", precision = 18, scale = 2)
    private BigDecimal netMonthlyIncome;

    public LoanEmploymentDetail() {}
    public LoanEmploymentDetail(Long applicationId) { this.applicationId = applicationId; }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String v) { this.employmentStatus = v; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String v) { this.employerName = v; }
    public BigDecimal getGrossAnnualIncome() { return grossAnnualIncome; }
    public void setGrossAnnualIncome(BigDecimal v) { this.grossAnnualIncome = v; }
    public BigDecimal getNetMonthlyIncome() { return netMonthlyIncome; }
    public void setNetMonthlyIncome(BigDecimal v) { this.netMonthlyIncome = v; }
}
