package com.example.lookup.controller;

import com.example.lookup.dto.CodeSetOptionDto;
import com.example.lookup.repository.CodeSetRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class CodeSetController {

    private final CodeSetRepository repo;

    public CodeSetController(CodeSetRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/code-sets/{type}")
    public List<CodeSetOptionDto> getCodeSet(
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return repo.findActiveByTypeAsOf(type, asOf).stream()
            .map(cs -> new CodeSetOptionDto(cs.getCode(), cs.getLabel()))
            .collect(Collectors.toList());
    }

    @GetMapping("/code-sets/{type}/{code}")
    public ResponseEntity<CodeSetOptionDto> getCodeSetItem(
            @PathVariable String type,
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return repo.findByTypeAndCodeAsOf(type, code, asOf)
            .map(cs -> ResponseEntity.ok(new CodeSetOptionDto(cs.getCode(), cs.getLabel())))
            .orElse(ResponseEntity.notFound().build());
    }
}
