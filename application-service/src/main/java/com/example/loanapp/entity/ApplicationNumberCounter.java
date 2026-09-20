package com.example.loanapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "application_number_counter")
public class ApplicationNumberCounter {

    @Id
    @Column(name = "counter_year")
    private Integer year;

    @Column(name = "last_value", nullable = false)
    private Long lastValue = 0L;

    public Integer getYear() { return year; }
    public void setYear(Integer v) { this.year = v; }
    public Long getLastValue() { return lastValue; }
    public void setLastValue(Long v) { this.lastValue = v; }
}
