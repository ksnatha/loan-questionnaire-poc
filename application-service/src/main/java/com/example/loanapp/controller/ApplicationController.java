package com.example.loanapp.controller;

import com.example.loanapp.dto.*;
import com.example.loanapp.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicationResponse> list() {
        return service.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@RequestBody CreateApplicationRequest req) {
        return service.create(req.createdUser);
    }

    @GetMapping("/{id}/render")
    public RenderResponse render(@PathVariable Long id) {
        return service.render(id);
    }

    @PutMapping("/{id}/draft")
    public SaveDraftResponse saveDraft(@PathVariable Long id,
                                        @RequestBody SaveDraftRequest req) {
        return service.saveDraft(id, req);
    }

    @PostMapping("/{id}/submit")
    public SubmitResponse submit(@PathVariable Long id) {
        return service.submit(id);
    }
}
