package com.example.loanapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "config_snapshot_item")
public class ConfigSnapshotItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "version_type", nullable = false)
    private String versionType;

    @Column(name = "version_value", nullable = false)
    private Integer versionValue;

    @Column(name = "strategy_bean_name")
    private String strategyBeanName;

    // Internal: carries the tabId for TAB_TEMPLATE items; not in the public API shape.
    @Column(name = "entity_id")
    private String entityId;

    public Long getId() { return id; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long v) { this.snapshotId = v; }
    public String getVersionType() { return versionType; }
    public void setVersionType(String v) { this.versionType = v; }
    public Integer getVersionValue() { return versionValue; }
    public void setVersionValue(Integer v) { this.versionValue = v; }
    public String getStrategyBeanName() { return strategyBeanName; }
    public void setStrategyBeanName(String v) { this.strategyBeanName = v; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String v) { this.entityId = v; }
}
