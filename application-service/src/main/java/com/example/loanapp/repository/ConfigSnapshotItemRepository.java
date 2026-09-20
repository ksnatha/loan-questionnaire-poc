package com.example.loanapp.repository;

import com.example.loanapp.entity.ConfigSnapshotItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConfigSnapshotItemRepository extends JpaRepository<ConfigSnapshotItem, Long> {
    List<ConfigSnapshotItem> findBySnapshotId(Long snapshotId);
    Optional<ConfigSnapshotItem> findBySnapshotIdAndVersionType(Long snapshotId, String versionType);
}
