package com.example.questionnaire.controller;

import com.example.questionnaire.dto.*;
import com.example.questionnaire.entity.SectionTemplate;
import com.example.questionnaire.service.QuestionnaireFieldsBulkUploadService;
import com.example.questionnaire.service.SectionTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/sections")
public class SectionTemplateController {

    private final SectionTemplateService service;
    private final QuestionnaireFieldsBulkUploadService bulkUploadService;

    public SectionTemplateController(SectionTemplateService service,
                                      QuestionnaireFieldsBulkUploadService bulkUploadService) {
        this.service = service;
        this.bulkUploadService = bulkUploadService;
    }

    @GetMapping
    public List<SectionListItem> list() {
        return service.listSections();
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

    @PostMapping("/{sectionId}/draft-revision")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionTemplateResponse createDraftRevision(@PathVariable String sectionId,
                                                        @RequestBody(required = false) DraftRevisionRequest req) {
        SectionTemplate st = service.createDraftRevision(
            sectionId, req != null ? req.template : null);
        return service.toResponse(st);
    }

    @PutMapping("/{sectionId}/versions/{version}")
    public SectionTemplateResponse updateDraft(@PathVariable String sectionId,
                                                @PathVariable Integer version,
                                                @RequestBody UpdateSectionRequest req) {
        SectionTemplate st = service.updateDraft(sectionId, version, req.template);
        return service.toResponse(st);
    }

    @PostMapping("/{sectionId}/versions/{version}/publish")
    public SectionTemplateResponse publish(@PathVariable String sectionId,
                                            @PathVariable Integer version) {
        SectionTemplate st = service.publish(sectionId, version);
        return service.toResponse(st);
    }

    @PostMapping("/{sectionId}/bulk-upload")
    public BulkUploadFieldsResponse bulkUpload(@PathVariable String sectionId,
                                                @RequestParam("file") MultipartFile file) {
        return bulkUploadService.bulkUpload(sectionId, file);
    }
}
