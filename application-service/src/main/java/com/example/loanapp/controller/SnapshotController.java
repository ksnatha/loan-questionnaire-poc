package com.example.loanapp.controller;

import com.example.loanapp.dto.*;
import com.example.loanapp.entity.*;
import com.example.loanapp.repository.*;
import com.example.loanapp.service.SnapshotService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/snapshots")
public class SnapshotController {

    private final ConfigSnapshotRepository snapshotRepo;
    private final ConfigSnapshotItemRepository itemRepo;
    private final SnapshotService snapshotService;

    public SnapshotController(ConfigSnapshotRepository snapshotRepo,
                               ConfigSnapshotItemRepository itemRepo,
                               SnapshotService snapshotService) {
        this.snapshotRepo = snapshotRepo;
        this.itemRepo = itemRepo;
        this.snapshotService = snapshotService;
    }

    @GetMapping("/active")
    public SnapshotResponse getActive() {
        ConfigSnapshot snapshot = snapshotRepo.findByStatus(SnapshotStatus.ACTIVE)
            .orElseThrow(() -> new NoSuchElementException("No ACTIVE snapshot"));
        return toResponse(snapshot);
    }

    @PostMapping("/clone-forward")
    @ResponseStatus(HttpStatus.CREATED)
    public SnapshotResponse cloneForward(@RequestBody CloneForwardRequest req) {
        ConfigSnapshot snap = snapshotService.cloneForward(req.newSnapshotCode, req.overrides);
        return toResponse(snap);
    }

    private SnapshotResponse toResponse(ConfigSnapshot snapshot) {
        SnapshotResponse r = new SnapshotResponse();
        r.id = snapshot.getId();
        r.snapshotCode = snapshot.getSnapshotCode();
        r.status = snapshot.getStatus().name();
        r.effectiveStart = snapshot.getEffectiveStart() != null
            ? snapshot.getEffectiveStart().toString() : null;
        r.items = itemRepo.findBySnapshotId(snapshot.getId()).stream()
            .map(item -> {
                SnapshotItemResponse ir = new SnapshotItemResponse();
                ir.versionType = item.getVersionType();
                ir.versionValue = item.getVersionValue();
                ir.strategyBeanName = item.getStrategyBeanName();
                return ir;
            })
            .collect(Collectors.toList());
        return r;
    }
}
