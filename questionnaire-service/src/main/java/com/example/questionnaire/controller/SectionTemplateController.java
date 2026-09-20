package com.example.questionnaire.controller;

import com.example.questionnaire.dto.*;
import com.example.questionnaire.entity.SectionTemplate;
import com.example.questionnaire.service.SectionTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sections")
public class SectionTemplateController {

    private final SectionTemplateService service;

    public SectionTemplateController(SectionTemplateService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SectionTemplateResponse create(@RequestBody CreateSectionRequest req) {
        SectionTemplate st = service.create(req.sectionId, req.labelKey, req.template);
        return service.toResponse(st);
    }

    @GetMapping("/{sectionId}/versions/{version}")
    public SectionTemplateResponse get(@PathVariable String sectionId,
                                        @PathVariable Integer version) {
        return service.getResponse(sectionId, version);
    }

    @PostMapping("/{sectionId}/versions/{version}/publish")
    public SectionTemplateResponse publish(@PathVariable String sectionId,
                                            @PathVariable Integer version) {
        SectionTemplate st = service.publish(sectionId, version);
        return service.toResponse(st);
    }
}
