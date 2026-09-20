package com.example.questionnaire.controller;

import com.example.questionnaire.dto.*;
import com.example.questionnaire.entity.TabTemplate;
import com.example.questionnaire.service.TabTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tabs")
public class TabTemplateController {

    private final TabTemplateService service;

    public TabTemplateController(TabTemplateService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TabTemplateResponse create(@RequestBody CreateTabRequest req) {
        TabTemplate tt = service.create(req.tabId, req.labelKey, req.sections);
        return service.toResponse(tt);
    }

    @GetMapping("/{tabId}/versions/{version}")
    public TabTemplateResponse get(@PathVariable String tabId,
                                    @PathVariable Integer version) {
        return service.getResponse(tabId, version);
    }

    @PostMapping("/{tabId}/versions/{version}/publish")
    public TabTemplateResponse publish(@PathVariable String tabId,
                                        @PathVariable Integer version) {
        TabTemplate tt = service.publish(tabId, version);
        return service.toResponse(tt);
    }
}
