package com.example.loanapp.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "config_snapshot")
public class ConfigSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_code", unique = true, nullable = false)
    private String snapshotCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SnapshotStatus status;

    @Column(name = "effective_start")
    private LocalDate effectiveStart;

    @Column(name = "effective_end")
    private LocalDate effectiveEnd;

    public Long getId() { return id; }
    public String getSnapshotCode() { return snapshotCode; }
    public void setSnapshotCode(String v) { this.snapshotCode = v; }
    public SnapshotStatus getStatus() { return status; }
    public void setStatus(SnapshotStatus v) { this.status = v; }
    public LocalDate getEffectiveStart() { return effectiveStart; }
    public void setEffectiveStart(LocalDate v) { this.effectiveStart = v; }
    public LocalDate getEffectiveEnd() { return effectiveEnd; }
    public void setEffectiveEnd(LocalDate v) { this.effectiveEnd = v; }
}
