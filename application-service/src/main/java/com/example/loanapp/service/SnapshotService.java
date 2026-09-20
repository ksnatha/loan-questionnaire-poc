package com.example.loanapp.service;

import com.example.loanapp.dto.CloneForwardRequest;
import com.example.loanapp.entity.*;
import com.example.loanapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SnapshotService {

    private final ConfigSnapshotRepository snapshotRepo;
    private final ConfigSnapshotItemRepository itemRepo;

    public SnapshotService(ConfigSnapshotRepository snapshotRepo,
                           ConfigSnapshotItemRepository itemRepo) {
        this.snapshotRepo = snapshotRepo;
        this.itemRepo = itemRepo;
    }

    public ConfigSnapshot cloneForward(String newCode,
                                        Map<String, CloneForwardRequest.ItemOverride> overrides) {
        ConfigSnapshot active = snapshotRepo.findByStatus(SnapshotStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException("No ACTIVE snapshot"));

        ConfigSnapshot newSnap = new ConfigSnapshot();
        newSnap.setSnapshotCode(newCode);
        newSnap.setStatus(SnapshotStatus.DRAFT);
        newSnap.setEffectiveStart(LocalDate.now());
        snapshotRepo.save(newSnap);

        List<ConfigSnapshotItem> items = itemRepo.findBySnapshotId(active.getId());
        for (ConfigSnapshotItem src : items) {
            ConfigSnapshotItem item = new ConfigSnapshotItem();
            item.setSnapshotId(newSnap.getId());
            item.setVersionType(src.getVersionType());

            CloneForwardRequest.ItemOverride override = overrides != null
                ? overrides.get(src.getVersionType()) : null;
            if (override != null) {
                item.setVersionValue(override.versionValue != null
                    ? override.versionValue : src.getVersionValue());
                item.setStrategyBeanName(override.strategyBeanName != null
                    ? override.strategyBeanName : src.getStrategyBeanName());
                item.setEntityId(override.entityId != null
                    ? override.entityId : src.getEntityId());
            } else {
                item.setVersionValue(src.getVersionValue());
                item.setStrategyBeanName(src.getStrategyBeanName());
                item.setEntityId(src.getEntityId());
            }
            itemRepo.save(item);
        }

        // Atomic swap: retire old, activate new
        active.setStatus(SnapshotStatus.RETIRED);
        snapshotRepo.save(active);
        newSnap.setStatus(SnapshotStatus.ACTIVE);
        snapshotRepo.save(newSnap);

        return newSnap;
    }
}
