package com.example.loanapp.init;

import com.example.loanapp.entity.*;
import com.example.loanapp.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ConfigSnapshotRepository snapshotRepo;
    private final ConfigSnapshotItemRepository itemRepo;
    private final ApplicationNumberCounterRepository counterRepo;

    public DataInitializer(ConfigSnapshotRepository snapshotRepo,
                           ConfigSnapshotItemRepository itemRepo,
                           ApplicationNumberCounterRepository counterRepo) {
        this.snapshotRepo = snapshotRepo;
        this.itemRepo = itemRepo;
        this.counterRepo = counterRepo;
    }

    @Override
    public void run(String... args) {
        seedInitialSnapshot();
        seedCounter();
    }

    private void seedInitialSnapshot() {
        if (snapshotRepo.findByStatus(SnapshotStatus.ACTIVE).isPresent()) return;

        ConfigSnapshot snapshot = new ConfigSnapshot();
        snapshot.setSnapshotCode("SNAPSHOT-2026-V1");
        snapshot.setStatus(SnapshotStatus.ACTIVE);
        snapshot.setEffectiveStart(LocalDate.now());
        snapshotRepo.save(snapshot);

        snapshotItem(snapshot.getId(), "TAB_TEMPLATE", 1, null, "proposal");
        snapshotItem(snapshot.getId(), "SECTION_KEY_INFORMATION", 1, null, null);
        snapshotItem(snapshot.getId(), "SECTION_ASSOCIATED_RECORDS", 1, null, null);
        snapshotItem(snapshot.getId(), "RATING_LOGIC", 1, "ratingV1", null);
    }

    private void seedCounter() {
        int year = LocalDate.now().getYear();
        if (counterRepo.findById(year).isEmpty()) {
            ApplicationNumberCounter c = new ApplicationNumberCounter();
            c.setYear(year);
            c.setLastValue(0L);
            counterRepo.save(c);
        }
    }

    private void snapshotItem(Long snapshotId, String versionType, int versionValue,
                               String strategyBeanName, String entityId) {
        ConfigSnapshotItem item = new ConfigSnapshotItem();
        item.setSnapshotId(snapshotId);
        item.setVersionType(versionType);
        item.setVersionValue(versionValue);
        item.setStrategyBeanName(strategyBeanName);
        item.setEntityId(entityId);
        itemRepo.save(item);
    }
}
