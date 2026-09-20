package com.example.loanapp.controller;

import com.example.loanapp.dto.LoanPartyDto;
import com.example.loanapp.entity.LoanParty;
import com.example.loanapp.entity.LoanPartyRole;
import com.example.loanapp.repository.LoanApplicationRepository;
import com.example.loanapp.repository.LoanPartyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/applications/{appId}/parties")
public class LoanPartyController {

    private final LoanPartyRepository partyRepo;
    private final LoanApplicationRepository appRepo;

    public LoanPartyController(LoanPartyRepository partyRepo, LoanApplicationRepository appRepo) {
        this.partyRepo = partyRepo;
        this.appRepo = appRepo;
    }

    @GetMapping
    public List<LoanPartyDto> list(@PathVariable Long appId) {
        verifyAppExists(appId);
        return partyRepo.findByApplicationId(appId).stream()
            .map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanPartyDto create(@PathVariable Long appId, @RequestBody LoanPartyDto req) {
        verifyAppExists(appId);
        LoanParty party = new LoanParty();
        party.setApplicationId(appId);
        applyRequest(party, req);
        return toDto(partyRepo.save(party));
    }

    @PutMapping("/{partyId}")
    public LoanPartyDto update(@PathVariable Long appId,
                                @PathVariable Long partyId,
                                @RequestBody LoanPartyDto req) {
        LoanParty party = partyRepo.findById(partyId)
            .filter(p -> p.getApplicationId().equals(appId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party not found"));
        applyRequest(party, req);
        return toDto(partyRepo.save(party));
    }

    private void applyRequest(LoanParty party, LoanPartyDto req) {
        if (req.role != null) party.setRole(LoanPartyRole.valueOf(req.role));
        party.setFirstName(req.firstName);
        party.setLastName(req.lastName);
        party.setDateOfBirth(req.dateOfBirth);
        party.setEmail(req.email);
    }

    private LoanPartyDto toDto(LoanParty p) {
        LoanPartyDto dto = new LoanPartyDto();
        dto.id = p.getId();
        dto.role = p.getRole() != null ? p.getRole().name() : null;
        dto.firstName = p.getFirstName();
        dto.lastName = p.getLastName();
        dto.dateOfBirth = p.getDateOfBirth();
        dto.email = p.getEmail();
        return dto;
    }

    private void verifyAppExists(Long appId) {
        if (!appRepo.existsById(appId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + appId);
        }
    }
}
