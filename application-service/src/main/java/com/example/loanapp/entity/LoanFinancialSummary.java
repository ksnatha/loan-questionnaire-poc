package com.example.loanapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "loan_financial_summary")
public class LoanFinancialSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "net_worth", precision = 18, scale = 2)
    private BigDecimal netWorth;

    @Column(name = "annual_revenue", precision = 18, scale = 2)
    private BigDecimal annualRevenue;

    @Column(name = "total_liabilities", precision = 18, scale = 2)
    private BigDecimal totalLiabilities;

    @Column(name = "credit_score")
    private Integer creditScore;

    public LoanFinancialSummary() {}
    public LoanFinancialSummary(Long applicationId) { this.applicationId = applicationId; }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public BigDecimal getNetWorth() { return netWorth; }
    public void setNetWorth(BigDecimal v) { this.netWorth = v; }
    public BigDecimal getAnnualRevenue() { return annualRevenue; }
    public void setAnnualRevenue(BigDecimal v) { this.annualRevenue = v; }
    public BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(BigDecimal v) { this.totalLiabilities = v; }
    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer v) { this.creditScore = v; }
}
