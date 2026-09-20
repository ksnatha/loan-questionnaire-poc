package com.example.lookup.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "code_set")
public class CodeSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_set_type", nullable = false, length = 50)
    private String codeSetType;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "active_ind", nullable = false, length = 1)
    private String activeInd = "Y";

    @Column(name = "display_order")
    private Integer displayOrder;

    public CodeSet() {}

    public CodeSet(String codeSetType, String code, String label,
                   LocalDate startDate, Integer displayOrder) {
        this.codeSetType = codeSetType;
        this.code = code;
        this.label = label;
        this.startDate = startDate;
        this.activeInd = "Y";
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getCodeSetType() { return codeSetType; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getActiveInd() { return activeInd; }
    public Integer getDisplayOrder() { return displayOrder; }

    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setActiveInd(String activeInd) { this.activeInd = activeInd; }
}
