package com.example.loanapp.repository;

import com.example.loanapp.entity.ConfigSnapshot;
import com.example.loanapp.entity.SnapshotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfigSnapshotRepository extends JpaRepository<ConfigSnapshot, Long> {
    Optional<ConfigSnapshot> findByStatus(SnapshotStatus status);
}
