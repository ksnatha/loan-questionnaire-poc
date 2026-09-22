package com.example.loanapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "loan_guarantor")
public class LoanGuarantor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "guarantor_name", length = 200)
    private String guarantorName;

    @Column(name = "date_of_birth", length = 20)
    private String dateOfBirth;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(name = "relationship", length = 50)
    private String relationship;

    @Column(name = "guaranteed_amount", precision = 18, scale = 2)
    private BigDecimal guaranteedAmount;

    @Column(name = "independent_advice", length = 20)
    private String independentAdvice;

    public LoanGuarantor() {}

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long v) { this.applicationId = v; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int v) { this.sortOrder = v; }
    public String getGuarantorName() { return guarantorName; }
    public void setGuarantorName(String v) { this.guarantorName = v; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String v) { this.dateOfBirth = v; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String v) { this.contactNumber = v; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String v) { this.relationship = v; }
    public BigDecimal getGuaranteedAmount() { return guaranteedAmount; }
    public void setGuaranteedAmount(BigDecimal v) { this.guaranteedAmount = v; }
    public String getIndependentAdvice() { return independentAdvice; }
    public void setIndependentAdvice(String v) { this.independentAdvice = v; }
}
