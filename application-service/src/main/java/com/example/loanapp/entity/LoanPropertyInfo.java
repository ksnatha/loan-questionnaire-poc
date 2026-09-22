package com.example.loanapp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "loan_property_info")
public class LoanPropertyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "property_address_full", length = 500)
    private String propertyAddressFull;

    @Column(name = "property_state", length = 50)
    private String propertyState;

    @Column(name = "property_postcode", length = 10)
    private String propertyPostcode;

    @Column(name = "property_purchase_price", precision = 18, scale = 2)
    private BigDecimal propertyPurchasePrice;

    @Column(name = "property_valuation", precision = 18, scale = 2)
    private BigDecimal propertyValuation;

    public LoanPropertyInfo() {}
    public LoanPropertyInfo(Long applicationId) { this.applicationId = applicationId; }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public String getPropertyAddressFull() { return propertyAddressFull; }
    public void setPropertyAddressFull(String v) { this.propertyAddressFull = v; }
    public String getPropertyState() { return propertyState; }
    public void setPropertyState(String v) { this.propertyState = v; }
    public String getPropertyPostcode() { return propertyPostcode; }
    public void setPropertyPostcode(String v) { this.propertyPostcode = v; }
    public BigDecimal getPropertyPurchasePrice() { return propertyPurchasePrice; }
    public void setPropertyPurchasePrice(BigDecimal v) { this.propertyPurchasePrice = v; }
    public BigDecimal getPropertyValuation() { return propertyValuation; }
    public void setPropertyValuation(BigDecimal v) { this.propertyValuation = v; }
}
