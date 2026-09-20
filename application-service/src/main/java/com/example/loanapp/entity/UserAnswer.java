package com.example.loanapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_answers",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"application_id", "field_key", "row_index"}))
public class UserAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "row_index", nullable = false)
    private int rowIndex = 0;

    @Column(name = "answer_value", length = 4000)
    private String value;

    public UserAnswer() {}

    public UserAnswer(Long applicationId, String fieldKey, int rowIndex) {
        this.applicationId = applicationId;
        this.fieldKey = fieldKey;
        this.rowIndex = rowIndex;
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public String getFieldKey() { return fieldKey; }
    public int getRowIndex() { return rowIndex; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
