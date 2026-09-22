package com.example.loanapp.service;

import com.example.common.template.*;
import com.example.loanapp.client.QuestionnaireClient;
import com.example.loanapp.client.QuestionnaireClient.SectionData;
import com.example.loanapp.dto.*;
import com.example.loanapp.entity.*;
import com.example.loanapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApplicationService {

    // ── DEDICATED field maps (instance — lambdas capture repo references) ──────

    private final Map<String, BiConsumer<LoanApplication, String>> dedicatedWriters;
    private final Map<String, Function<LoanApplication, String>> dedicatedReaders;

    // ── Repositories ─────────────────────────────────────────────────────────────

    private final LoanApplicationRepository appRepo;
    private final ConfigSnapshotRepository snapshotRepo;
    private final ConfigSnapshotItemRepository itemRepo;
    private final ApplicationNumberCounterRepository counterRepo;
    private final UserAnswerRepository userAnswerRepo;
    private final LoanFinancialSummaryRepository financialRepo;
    private final LoanComplianceRecordRepository complianceRepo;
    private final LoanEmploymentDetailRepository employmentRepo;
    private final LoanPropertyInfoRepository propertyRepo;
    private final LoanGuarantorRepository guarantorRepo;
    private final QuestionnaireClient questionnaireClient;
    private final DropdownHydrationService dropdownHydration;
    private final RuleEvaluator ruleEvaluator;
    private final Map<String, RiskRatingStrategy> strategies;

    public ApplicationService(LoanApplicationRepository appRepo,
                               ConfigSnapshotRepository snapshotRepo,
                               ConfigSnapshotItemRepository itemRepo,
                               ApplicationNumberCounterRepository counterRepo,
                               UserAnswerRepository userAnswerRepo,
                               LoanFinancialSummaryRepository financialRepo,
                               LoanComplianceRecordRepository complianceRepo,
                               LoanEmploymentDetailRepository employmentRepo,
                               LoanPropertyInfoRepository propertyRepo,
                               LoanGuarantorRepository guarantorRepo,
                               QuestionnaireClient questionnaireClient,
                               DropdownHydrationService dropdownHydration,
                               RuleEvaluator ruleEvaluator,
                               Map<String, RiskRatingStrategy> strategies) {
        this.appRepo = appRepo;
        this.snapshotRepo = snapshotRepo;
        this.itemRepo = itemRepo;
        this.counterRepo = counterRepo;
        this.userAnswerRepo = userAnswerRepo;
        this.financialRepo = financialRepo;
        this.complianceRepo = complianceRepo;
        this.employmentRepo = employmentRepo;
        this.propertyRepo = propertyRepo;
        this.guarantorRepo = guarantorRepo;
        this.questionnaireClient = questionnaireClient;
        this.dropdownHydration = dropdownHydration;
        this.ruleEvaluator = ruleEvaluator;
        this.strategies = strategies;

        this.dedicatedWriters = buildDedicatedWriters();
        this.dedicatedReaders = buildDedicatedReaders();
    }

    // ── DEDICATED field map builders ─────────────────────────────────────────────

    private Map<String, BiConsumer<LoanApplication, String>> buildDedicatedWriters() {
        Map<String, BiConsumer<LoanApplication, String>> m = new HashMap<>();

        // loan_application
        m.put("loan_application.proposal_name",
            (app, v) -> app.setProposalName(v));
        m.put("loan_application.loan_amount",
            (app, v) -> app.setLoanAmount(v == null || v.isBlank() ? null : new BigDecimal(v)));
        m.put("loan_application.proposal_description",
            (app, v) -> app.setProposalDescription(v));

        // loan_financial_summary
        m.put("loan_financial_summary.net_worth",
            (app, v) -> withFinancial(app.getId(), fs -> fs.setNetWorth(decimal(v))));
        m.put("loan_financial_summary.annual_revenue",
            (app, v) -> withFinancial(app.getId(), fs -> fs.setAnnualRevenue(decimal(v))));
        m.put("loan_financial_summary.total_liabilities",
            (app, v) -> withFinancial(app.getId(), fs -> fs.setTotalLiabilities(decimal(v))));
        m.put("loan_financial_summary.credit_score",
            (app, v) -> withFinancial(app.getId(), fs ->
                fs.setCreditScore(v == null || v.isBlank() ? null : Integer.parseInt(v))));

        // loan_compliance_record
        m.put("loan_compliance_record.aml_check_status",
            (app, v) -> withCompliance(app.getId(), cr -> cr.setAmlCheckStatus(v)));
        m.put("loan_compliance_record.aml_check_date",
            (app, v) -> withCompliance(app.getId(), cr -> cr.setAmlCheckDate(v)));
        m.put("loan_compliance_record.pep_status",
            (app, v) -> withCompliance(app.getId(), cr -> cr.setPepStatus(v)));
        m.put("loan_compliance_record.sanctions_status",
            (app, v) -> withCompliance(app.getId(), cr -> cr.setSanctionsStatus(v)));

        // loan_employment_detail
        m.put("loan_employment_detail.employment_status",
            (app, v) -> withEmployment(app.getId(), ed -> ed.setEmploymentStatus(v)));
        m.put("loan_employment_detail.employer_name",
            (app, v) -> withEmployment(app.getId(), ed -> ed.setEmployerName(v)));
        m.put("loan_employment_detail.gross_annual_income",
            (app, v) -> withEmployment(app.getId(), ed -> ed.setGrossAnnualIncome(decimal(v))));
        m.put("loan_employment_detail.net_monthly_income",
            (app, v) -> withEmployment(app.getId(), ed -> ed.setNetMonthlyIncome(decimal(v))));

        // loan_property_info
        m.put("loan_property_info.property_address_full",
            (app, v) -> withProperty(app.getId(), pi -> pi.setPropertyAddressFull(v)));
        m.put("loan_property_info.property_state",
            (app, v) -> withProperty(app.getId(), pi -> pi.setPropertyState(v)));
        m.put("loan_property_info.property_postcode",
            (app, v) -> withProperty(app.getId(), pi -> pi.setPropertyPostcode(v)));
        m.put("loan_property_info.property_purchase_price",
            (app, v) -> withProperty(app.getId(), pi -> pi.setPropertyPurchasePrice(decimal(v))));
        m.put("loan_property_info.property_valuation",
            (app, v) -> withProperty(app.getId(), pi -> pi.setPropertyValuation(decimal(v))));

        return Collections.unmodifiableMap(m);
    }

    private Map<String, Function<LoanApplication, String>> buildDedicatedReaders() {
        Map<String, Function<LoanApplication, String>> m = new HashMap<>();

        // loan_application
        m.put("loan_application.proposal_name", LoanApplication::getProposalName);
        m.put("loan_application.loan_amount",
            app -> app.getLoanAmount() == null ? null
                : app.getLoanAmount().stripTrailingZeros().toPlainString());
        m.put("loan_application.proposal_description", LoanApplication::getProposalDescription);

        // loan_financial_summary
        m.put("loan_financial_summary.net_worth",
            app -> financialRepo.findByApplicationId(app.getId())
                .map(fs -> bdStr(fs.getNetWorth())).orElse(null));
        m.put("loan_financial_summary.annual_revenue",
            app -> financialRepo.findByApplicationId(app.getId())
                .map(fs -> bdStr(fs.getAnnualRevenue())).orElse(null));
        m.put("loan_financial_summary.total_liabilities",
            app -> financialRepo.findByApplicationId(app.getId())
                .map(fs -> bdStr(fs.getTotalLiabilities())).orElse(null));
        m.put("loan_financial_summary.credit_score",
            app -> financialRepo.findByApplicationId(app.getId())
                .map(fs -> fs.getCreditScore() == null ? null : String.valueOf(fs.getCreditScore()))
                .orElse(null));

        // loan_compliance_record
        m.put("loan_compliance_record.aml_check_status",
            app -> complianceRepo.findByApplicationId(app.getId())
                .map(LoanComplianceRecord::getAmlCheckStatus).orElse(null));
        m.put("loan_compliance_record.aml_check_date",
            app -> complianceRepo.findByApplicationId(app.getId())
                .map(LoanComplianceRecord::getAmlCheckDate).orElse(null));
        m.put("loan_compliance_record.pep_status",
            app -> complianceRepo.findByApplicationId(app.getId())
                .map(LoanComplianceRecord::getPepStatus).orElse(null));
        m.put("loan_compliance_record.sanctions_status",
            app -> complianceRepo.findByApplicationId(app.getId())
                .map(LoanComplianceRecord::getSanctionsStatus).orElse(null));

        // loan_employment_detail
        m.put("loan_employment_detail.employment_status",
            app -> employmentRepo.findByApplicationId(app.getId())
                .map(LoanEmploymentDetail::getEmploymentStatus).orElse(null));
        m.put("loan_employment_detail.employer_name",
            app -> employmentRepo.findByApplicationId(app.getId())
                .map(LoanEmploymentDetail::getEmployerName).orElse(null));
        m.put("loan_employment_detail.gross_annual_income",
            app -> employmentRepo.findByApplicationId(app.getId())
                .map(ed -> bdStr(ed.getGrossAnnualIncome())).orElse(null));
        m.put("loan_employment_detail.net_monthly_income",
            app -> employmentRepo.findByApplicationId(app.getId())
                .map(ed -> bdStr(ed.getNetMonthlyIncome())).orElse(null));

        // loan_property_info
        m.put("loan_property_info.property_address_full",
            app -> propertyRepo.findByApplicationId(app.getId())
                .map(LoanPropertyInfo::getPropertyAddressFull).orElse(null));
        m.put("loan_property_info.property_state",
            app -> propertyRepo.findByApplicationId(app.getId())
                .map(LoanPropertyInfo::getPropertyState).orElse(null));
        m.put("loan_property_info.property_postcode",
            app -> propertyRepo.findByApplicationId(app.getId())
                .map(LoanPropertyInfo::getPropertyPostcode).orElse(null));
        m.put("loan_property_info.property_purchase_price",
            app -> propertyRepo.findByApplicationId(app.getId())
                .map(pi -> bdStr(pi.getPropertyPurchasePrice())).orElse(null));
        m.put("loan_property_info.property_valuation",
            app -> propertyRepo.findByApplicationId(app.getId())
                .map(pi -> bdStr(pi.getPropertyValuation())).orElse(null));

        return Collections.unmodifiableMap(m);
    }

    // ── Entity helper methods (find-or-create + apply + save) ────────────────────

    private void withFinancial(Long appId, java.util.function.Consumer<LoanFinancialSummary> fn) {
        LoanFinancialSummary fs = financialRepo.findByApplicationId(appId)
            .orElseGet(() -> { var e = new LoanFinancialSummary(appId); return financialRepo.save(e); });
        fn.accept(fs);
        financialRepo.save(fs);
    }

    private void withCompliance(Long appId, java.util.function.Consumer<LoanComplianceRecord> fn) {
        LoanComplianceRecord cr = complianceRepo.findByApplicationId(appId)
            .orElseGet(() -> { var e = new LoanComplianceRecord(appId); return complianceRepo.save(e); });
        fn.accept(cr);
        complianceRepo.save(cr);
    }

    private void withEmployment(Long appId, java.util.function.Consumer<LoanEmploymentDetail> fn) {
        LoanEmploymentDetail ed = employmentRepo.findByApplicationId(appId)
            .orElseGet(() -> { var e = new LoanEmploymentDetail(appId); return employmentRepo.save(e); });
        fn.accept(ed);
        employmentRepo.save(ed);
    }

    private void withProperty(Long appId, java.util.function.Consumer<LoanPropertyInfo> fn) {
        LoanPropertyInfo pi = propertyRepo.findByApplicationId(appId)
            .orElseGet(() -> { var e = new LoanPropertyInfo(appId); return propertyRepo.save(e); });
        fn.accept(pi);
        propertyRepo.save(pi);
    }

    private static BigDecimal decimal(String v) {
        return (v == null || v.isBlank()) ? null : new BigDecimal(v);
    }

    private static String bdStr(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    // ── Public API ───────────────────────────────────────────────────────────────

    public ApplicationResponse create(String createdUser) {
        ConfigSnapshot snapshot = snapshotRepo.findByStatus(SnapshotStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException("No ACTIVE snapshot found"));
        LoanApplication app = new LoanApplication();
        app.setHumanReadableId(nextHumanReadableId());
        app.setConfigSnapshotId(snapshot.getId());
        app.setCreatedDate(LocalDate.now());
        app.setCreatedUser(createdUser);
        appRepo.save(app);
        return toApplicationResponse(app);
    }

    @Transactional(readOnly = true)
    public RenderResponse render(Long id) {
        LoanApplication app = loadApp(id);
        LoadedTemplates loaded = loadTemplates(app.getConfigSnapshotId());

        List<UserAnswer> allUserAnswers = userAnswerRepo.findByApplicationId(app.getId());
        Map<String, String> flatEavAnswers = allUserAnswers.stream()
            .filter(ua -> ua.getRowIndex() == 0)
            .collect(Collectors.toMap(UserAnswer::getFieldKey,
                ua -> ua.getValue() != null ? ua.getValue() : ""));

        RenderedTab tab = new RenderedTab();
        tab.tabId = loaded.tab().tabId;
        tab.labelKey = loaded.tab().labelKey;
        tab.sections = new ArrayList<>();

        Map<String, String> answers = new LinkedHashMap<>();
        Map<String, List<Map<String, String>>> gridAnswers = new LinkedHashMap<>();

        for (TabSectionRef sectionRef : loaded.tab().sections) {
            SectionData sd = loaded.sections().get(sectionRef.sectionId);
            RenderedSection section = new RenderedSection();
            section.sectionId = sectionRef.sectionId;
            section.labelKey = sd.labelKey();
            section.fields = sd.template().fields;
            section.grids = sd.template().grids;
            tab.sections.add(section);

            for (FieldDefinition field : sd.template().fields) {
                if (field.storage != null && field.storage.type == StorageType.DEDICATED) {
                    String ref = field.storage.tableName + "." + field.storage.columnName;
                    String value = dedicatedReaders.getOrDefault(ref, a -> null).apply(app);
                    if (value != null) answers.put(field.fieldKey, value);
                } else if (field.storage != null && field.storage.type == StorageType.EAV) {
                    String value = flatEavAnswers.get(field.fieldKey);
                    if (value != null) answers.put(field.fieldKey, value);
                }
                if (field.dropdownSource != null) {
                    field.resolvedOptions = dropdownHydration.resolve(field, app.getCreatedDate());
                }
            }

            for (GridDefinition grid : sd.template().grids) {
                for (FieldDefinition col : grid.columns) {
                    if (col.dropdownSource != null) {
                        col.resolvedOptions = dropdownHydration.resolve(col, app.getCreatedDate());
                    }
                }
                if ("DEDICATED".equals(grid.gridStorageType)) {
                    gridAnswers.put(grid.gridKey, loadDedicatedGridRows(app.getId(), grid));
                } else {
                    gridAnswers.put(grid.gridKey, loadEavGridRows(allUserAnswers, grid));
                }
            }
        }

        RenderResponse resp = new RenderResponse();
        resp.application = toApplicationResponse(app);
        resp.tab = tab;
        resp.answers = answers;
        resp.gridAnswers = gridAnswers;
        return resp;
    }

    public SaveDraftResponse saveDraft(Long id, SaveDraftRequest req) {
        LoanApplication app = loadApp(id);
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Application is not in DRAFT status");
        }
        LoadedTemplates loaded = loadTemplates(app.getConfigSnapshotId());
        Map<String, String> incoming = req.answers != null ? req.answers : Map.of();
        Map<String, List<Map<String, String>>> incomingGrids =
            req.gridAnswers != null ? req.gridAnswers : Map.of();

        for (SectionData sd : loaded.sections().values()) {
            for (FieldDefinition field : sd.template().fields) {
                if (!incoming.containsKey(field.fieldKey)) continue;
                String value = incoming.get(field.fieldKey);
                if (field.storage != null && field.storage.type == StorageType.DEDICATED) {
                    String ref = field.storage.tableName + "." + field.storage.columnName;
                    dedicatedWriters.getOrDefault(ref, (a, v) -> {}).accept(app, value);
                } else if (field.storage != null && field.storage.type == StorageType.EAV) {
                    upsertFlatEav(app.getId(), field.fieldKey, value);
                }
            }
            for (GridDefinition grid : sd.template().grids) {
                List<Map<String, String>> rows = incomingGrids.get(grid.gridKey);
                if (rows != null) {
                    saveGridRows(app.getId(), grid, rows);
                }
            }
        }
        appRepo.save(app);

        Map<String, String> currentAnswers = buildCurrentAnswerMap(app, loaded);
        purgeHiddenFields(app, loaded, currentAnswers);

        Map<String, String> ratingAnswers = buildCurrentAnswerMap(app, loaded);
        String rating = computeRating(app, loaded.ratingBeanName(), ratingAnswers);
        if (rating != null) {
            app.setRiskRating(rating);
            app.setRiskRatingComputedDate(LocalDateTime.now());
            appRepo.save(app);
        }

        SaveDraftResponse resp = new SaveDraftResponse();
        resp.riskRating = rating;
        resp.riskRatingComputedDate = app.getRiskRatingComputedDate() != null
            ? app.getRiskRatingComputedDate().toString() : null;
        return resp;
    }

    public SubmitResponse submit(Long id) {
        LoanApplication app = loadApp(id);
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Application is not in DRAFT status");
        }
        LoadedTemplates loaded = loadTemplates(app.getConfigSnapshotId());

        Map<String, String> currentAnswers = buildCurrentAnswerMap(app, loaded);
        purgeHiddenFields(app, loaded, currentAnswers);
        Map<String, String> answersForValidation = buildCurrentAnswerMap(app, loaded);

        Map<String, List<Map<String, String>>> gridAnswers = loadAllGridAnswers(app.getId(), loaded);
        validateRequired(loaded, answersForValidation, gridAnswers);

        String rating = computeRating(app, loaded.ratingBeanName(), answersForValidation);
        if (rating != null) {
            app.setRiskRating(rating);
            app.setRiskRatingComputedDate(LocalDateTime.now());
        }
        app.setStatus(ApplicationStatus.SUBMITTED);
        appRepo.save(app);

        SubmitResponse resp = new SubmitResponse();
        resp.status = "SUBMITTED";
        resp.riskRating = app.getRiskRating();
        return resp;
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listAll() {
        return appRepo.findAllByOrderByIdDesc().stream()
            .map(this::toApplicationResponse)
            .collect(Collectors.toList());
    }

    // ── Grid helpers ─────────────────────────────────────────────────────────────

    private List<Map<String, String>> loadEavGridRows(List<UserAnswer> allUserAnswers,
                                                       GridDefinition grid) {
        Set<String> colKeys = grid.columns.stream()
            .map(c -> c.fieldKey).collect(Collectors.toSet());
        Map<Integer, Map<String, String>> rowsByIndex = new TreeMap<>();
        allUserAnswers.stream()
            .filter(ua -> ua.getRowIndex() > 0 && colKeys.contains(ua.getFieldKey()))
            .forEach(ua -> rowsByIndex
                .computeIfAbsent(ua.getRowIndex(), k -> new LinkedHashMap<>())
                .put(ua.getFieldKey(), ua.getValue() != null ? ua.getValue() : ""));
        return new ArrayList<>(rowsByIndex.values());
    }

    private List<Map<String, String>> loadDedicatedGridRows(Long appId, GridDefinition grid) {
        if ("loan_guarantor".equals(grid.gridBackingTable)) {
            return guarantorRepo.findByApplicationIdOrderBySortOrder(appId).stream()
                .map(this::guarantorToRow)
                .collect(Collectors.toList());
        }
        return List.of();
    }

    private Map<String, String> guarantorToRow(LoanGuarantor g) {
        Map<String, String> row = new LinkedHashMap<>();
        putIfNonNull(row, "guarantor_name",    g.getGuarantorName());
        putIfNonNull(row, "date_of_birth",     g.getDateOfBirth());
        putIfNonNull(row, "contact_number",    g.getContactNumber());
        putIfNonNull(row, "relationship",      g.getRelationship());
        putIfNonNull(row, "guaranteed_amount", bdStr(g.getGuaranteedAmount()));
        putIfNonNull(row, "independent_advice",g.getIndependentAdvice());
        return row;
    }

    private static void putIfNonNull(Map<String, String> m, String k, String v) {
        if (v != null) m.put(k, v);
    }

    private void saveGridRows(Long appId, GridDefinition grid,
                               List<Map<String, String>> rows) {
        if ("DEDICATED".equals(grid.gridStorageType)) {
            saveDedicatedGridRows(appId, grid, rows);
        } else {
            saveEavGridRows(appId, grid, rows);
        }
    }

    private void saveEavGridRows(Long appId, GridDefinition grid,
                                  List<Map<String, String>> rows) {
        List<String> colKeys = grid.columns.stream()
            .map(c -> c.fieldKey).collect(Collectors.toList());
        if (!colKeys.isEmpty()) {
            userAnswerRepo.deleteGridRows(appId, colKeys);
        }
        int rowIndex = 1;
        for (Map<String, String> row : rows) {
            for (String key : colKeys) {
                String value = row.get(key);
                if (value != null && !value.isBlank()) {
                    UserAnswer ua = new UserAnswer(appId, key, rowIndex);
                    ua.setValue(value);
                    userAnswerRepo.save(ua);
                }
            }
            rowIndex++;
        }
    }

    private void saveDedicatedGridRows(Long appId, GridDefinition grid,
                                        List<Map<String, String>> rows) {
        if ("loan_guarantor".equals(grid.gridBackingTable)) {
            guarantorRepo.deleteByApplicationId(appId);
            int order = 1;
            for (Map<String, String> row : rows) {
                LoanGuarantor g = new LoanGuarantor();
                g.setApplicationId(appId);
                g.setSortOrder(order++);
                g.setGuarantorName(row.get("guarantor_name"));
                g.setDateOfBirth(row.get("date_of_birth"));
                g.setContactNumber(row.get("contact_number"));
                g.setRelationship(row.get("relationship"));
                String amt = row.get("guaranteed_amount");
                g.setGuaranteedAmount(amt != null && !amt.isBlank() ? new BigDecimal(amt) : null);
                g.setIndependentAdvice(row.get("independent_advice"));
                guarantorRepo.save(g);
            }
        }
    }

    // ── Validation helpers ───────────────────────────────────────────────────────

    private Map<String, List<Map<String, String>>> loadAllGridAnswers(Long appId,
                                                                        LoadedTemplates loaded) {
        List<UserAnswer> gridUas = userAnswerRepo
            .findByApplicationIdAndRowIndexGreaterThanOrderByRowIndex(appId, 0);
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        for (SectionData sd : loaded.sections().values()) {
            for (GridDefinition grid : sd.template().grids) {
                if ("DEDICATED".equals(grid.gridStorageType)) {
                    result.put(grid.gridKey, loadDedicatedGridRows(appId, grid));
                } else {
                    result.put(grid.gridKey, loadEavGridRows(gridUas, grid));
                }
            }
        }
        return result;
    }

    private void purgeHiddenFields(LoanApplication app, LoadedTemplates loaded,
                                    Map<String, String> answers) {
        boolean dedicatedDirty = false;
        for (SectionData sd : loaded.sections().values()) {
            for (FieldDefinition field : sd.template().fields) {
                if (ruleEvaluator.isVisible(field, answers)) continue;
                if (field.storage != null && field.storage.type == StorageType.EAV) {
                    userAnswerRepo.findByApplicationIdAndFieldKeyAndRowIndex(
                        app.getId(), field.fieldKey, 0)
                        .ifPresent(userAnswerRepo::delete);
                } else if (field.storage != null && field.storage.type == StorageType.DEDICATED) {
                    String ref = field.storage.tableName + "." + field.storage.columnName;
                    dedicatedWriters.getOrDefault(ref, (a, v) -> {}).accept(app, null);
                    if (ref.startsWith("loan_application.")) dedicatedDirty = true;
                }
            }
        }
        if (dedicatedDirty) appRepo.save(app);
    }

    private Map<String, String> buildCurrentAnswerMap(LoanApplication app,
                                                        LoadedTemplates loaded) {
        Map<String, String> map = new LinkedHashMap<>();
        for (SectionData sd : loaded.sections().values()) {
            for (FieldDefinition field : sd.template().fields) {
                if (field.storage != null && field.storage.type == StorageType.DEDICATED) {
                    String ref = field.storage.tableName + "." + field.storage.columnName;
                    String value = dedicatedReaders.getOrDefault(ref, a -> null).apply(app);
                    if (value != null) map.put(field.fieldKey, value);
                }
            }
        }
        userAnswerRepo.findByApplicationId(app.getId()).stream()
            .filter(ua -> ua.getRowIndex() == 0)
            .forEach(ua -> { if (ua.getValue() != null) map.put(ua.getFieldKey(), ua.getValue()); });
        return map;
    }

    private void validateRequired(LoadedTemplates loaded, Map<String, String> answers,
                                   Map<String, List<Map<String, String>>> gridAnswers) {
        List<ValidationError> errors = new ArrayList<>();
        for (SectionData sd : loaded.sections().values()) {
            for (FieldDefinition field : sd.template().fields) {
                if (!ruleEvaluator.isVisible(field, answers)) continue;
                if (!ruleEvaluator.isRequired(field, answers)) continue;
                String value = answers.get(field.fieldKey);
                if (value == null || value.isBlank()) {
                    errors.add(new ValidationError(field.fieldKey,
                        "required-" + field.fieldKey, "REQUIRED", "validation.required"));
                }
            }
            for (GridDefinition grid : sd.template().grids) {
                List<Map<String, String>> rows = gridAnswers.getOrDefault(grid.gridKey, List.of());
                for (Map<String, String> rowAnswers : rows) {
                    for (FieldDefinition col : grid.columns) {
                        if (!ruleEvaluator.isRequired(col, rowAnswers)) continue;
                        String value = rowAnswers.get(col.fieldKey);
                        if (value == null || value.isBlank()) {
                            errors.add(new ValidationError(col.fieldKey,
                                "required-" + col.fieldKey, "REQUIRED", "validation.required"));
                        }
                    }
                }
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    // ── Template loading ─────────────────────────────────────────────────────────

    private LoadedTemplates loadTemplates(Long snapshotId) {
        List<ConfigSnapshotItem> items = itemRepo.findBySnapshotId(snapshotId);

        ConfigSnapshotItem tabItem = items.stream()
            .filter(i -> "TAB_TEMPLATE".equals(i.getVersionType()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No TAB_TEMPLATE item in snapshot"));

        TabTemplateJson tab = questionnaireClient.getTabTemplate(
            tabItem.getEntityId(), tabItem.getVersionValue());

        Map<String, SectionData> sections = new LinkedHashMap<>();
        for (TabSectionRef sectionRef : tab.sections) {
            String versionType = "SECTION_" + sectionRef.sectionId.toUpperCase().replace("-", "_");
            int version = items.stream()
                .filter(i -> versionType.equals(i.getVersionType()))
                .mapToInt(ConfigSnapshotItem::getVersionValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "No snapshot item for " + versionType));
            sections.put(sectionRef.sectionId,
                questionnaireClient.getSectionData(sectionRef.sectionId, version));
        }
        String ratingBeanName = items.stream()
            .filter(i -> "RATING_LOGIC".equals(i.getVersionType()))
            .map(ConfigSnapshotItem::getStrategyBeanName)
            .findFirst()
            .orElse(null);

        return new LoadedTemplates(tab, sections, ratingBeanName);
    }

    // ── Other helpers ────────────────────────────────────────────────────────────

    private void upsertFlatEav(Long appId, String fieldKey, String value) {
        UserAnswer ua = userAnswerRepo
            .findByApplicationIdAndFieldKeyAndRowIndex(appId, fieldKey, 0)
            .orElseGet(() -> new UserAnswer(appId, fieldKey, 0));
        ua.setValue(value);
        userAnswerRepo.save(ua);
    }

    private LoanApplication loadApp(Long id) {
        return appRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Application not found: " + id));
    }

    private String nextHumanReadableId() {
        int year = LocalDate.now().getYear();
        ApplicationNumberCounter counter = counterRepo.findByYearForUpdate(year)
            .orElseGet(() -> {
                ApplicationNumberCounter c = new ApplicationNumberCounter();
                c.setYear(year);
                c.setLastValue(0L);
                return counterRepo.saveAndFlush(c);
            });
        counter.setLastValue(counter.getLastValue() + 1);
        counterRepo.save(counter);
        return "L" + year + "-" + counter.getLastValue();
    }

    private ApplicationResponse toApplicationResponse(LoanApplication app) {
        ApplicationResponse r = new ApplicationResponse();
        r.id = app.getId();
        r.humanReadableId = app.getHumanReadableId();
        r.status = app.getStatus().name();
        r.configSnapshotId = app.getConfigSnapshotId();
        r.riskRating = app.getRiskRating();
        r.proposalName = app.getProposalName();
        r.loanAmount = app.getLoanAmount() == null ? null
            : app.getLoanAmount().stripTrailingZeros().toPlainString();
        r.createdDate = app.getCreatedDate() != null ? app.getCreatedDate().toString() : null;
        return r;
    }

    private String computeRating(LoanApplication app, String beanName,
                                  Map<String, String> answers) {
        if (beanName == null) return null;
        RiskRatingStrategy strategy = strategies.get(beanName);
        if (strategy == null) return null;
        Map<String, String> enriched = new LinkedHashMap<>(answers);
        long collateralCount = userAnswerRepo
            .findByApplicationIdAndRowIndexGreaterThanOrderByRowIndex(app.getId(), 0)
            .stream()
            .map(UserAnswer::getRowIndex)
            .distinct()
            .count();
        enriched.put("collateral_count", String.valueOf(collateralCount));
        return strategy.compute(app, enriched);
    }

    private record LoadedTemplates(TabTemplateJson tab, Map<String, SectionData> sections,
                                    String ratingBeanName) {}
}
