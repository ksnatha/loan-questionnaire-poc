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

    private static final Map<String, BiConsumer<LoanApplication, String>> DEDICATED_WRITERS = Map.of(
        "loan_application.proposal_name",
            (app, v) -> app.setProposalName(v),
        "loan_application.loan_amount",
            (app, v) -> app.setLoanAmount(v == null || v.isBlank() ? null : new BigDecimal(v)),
        "loan_application.proposal_description",
            (app, v) -> app.setProposalDescription(v)
    );

    private static final Map<String, Function<LoanApplication, String>> DEDICATED_READERS = Map.of(
        "loan_application.proposal_name",
            LoanApplication::getProposalName,
        "loan_application.loan_amount",
            app -> app.getLoanAmount() == null ? null
                : app.getLoanAmount().stripTrailingZeros().toPlainString(),
        "loan_application.proposal_description",
            LoanApplication::getProposalDescription
    );

    private final LoanApplicationRepository appRepo;
    private final ConfigSnapshotRepository snapshotRepo;
    private final ConfigSnapshotItemRepository itemRepo;
    private final ApplicationNumberCounterRepository counterRepo;
    private final UserAnswerRepository userAnswerRepo;
    private final QuestionnaireClient questionnaireClient;
    private final DropdownHydrationService dropdownHydration;
    private final RuleEvaluator ruleEvaluator;
    private final Map<String, RiskRatingStrategy> strategies;

    public ApplicationService(LoanApplicationRepository appRepo,
                               ConfigSnapshotRepository snapshotRepo,
                               ConfigSnapshotItemRepository itemRepo,
                               ApplicationNumberCounterRepository counterRepo,
                               UserAnswerRepository userAnswerRepo,
                               QuestionnaireClient questionnaireClient,
                               DropdownHydrationService dropdownHydration,
                               RuleEvaluator ruleEvaluator,
                               Map<String, RiskRatingStrategy> strategies) {
        this.appRepo = appRepo;
        this.snapshotRepo = snapshotRepo;
        this.itemRepo = itemRepo;
        this.counterRepo = counterRepo;
        this.userAnswerRepo = userAnswerRepo;
        this.questionnaireClient = questionnaireClient;
        this.dropdownHydration = dropdownHydration;
        this.ruleEvaluator = ruleEvaluator;
        this.strategies = strategies;
    }

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

        // Load all user answers once
        List<UserAnswer> allUserAnswers = userAnswerRepo.findByApplicationId(app.getId());
        Map<String, String> flatEavAnswers = allUserAnswers.stream()
            .filter(ua -> ua.getRowIndex() == 0)
            .collect(Collectors.toMap(UserAnswer::getFieldKey,
                ua -> ua.getValue() != null ? ua.getValue() : ""));

        // Build grid answers grouped by rowIndex
        Map<String, Map<Integer, Map<String, String>>> rawGridAnswers = new LinkedHashMap<>();
        allUserAnswers.stream()
            .filter(ua -> ua.getRowIndex() > 0)
            .forEach(ua -> rawGridAnswers
                .computeIfAbsent(ua.getFieldKey(), k -> new TreeMap<>())
                .computeIfAbsent(ua.getRowIndex(), k -> new LinkedHashMap<>())
                .put(ua.getFieldKey(), ua.getValue()));

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

            // Flat fields
            for (FieldDefinition field : sd.template().fields) {
                if (field.storage != null && field.storage.type == StorageType.DEDICATED) {
                    String ref = field.storage.tableName + "." + field.storage.columnName;
                    String value = DEDICATED_READERS.getOrDefault(ref, a -> null).apply(app);
                    if (value != null) answers.put(field.fieldKey, value);
                } else if (field.storage != null && field.storage.type == StorageType.EAV) {
                    String value = flatEavAnswers.get(field.fieldKey);
                    if (value != null) answers.put(field.fieldKey, value);
                }
                if (field.dropdownSource != null) {
                    field.resolvedOptions = dropdownHydration.resolve(field, app.getCreatedDate());
                }
            }

            // Grid fields
            for (GridDefinition grid : sd.template().grids) {
                // Hydrate column dropdown options
                for (FieldDefinition col : grid.columns) {
                    if (col.dropdownSource != null) {
                        col.resolvedOptions = dropdownHydration.resolve(col, app.getCreatedDate());
                    }
                }

                // Reconstruct rows from user_answers
                Set<String> colKeys = grid.columns.stream()
                    .map(c -> c.fieldKey).collect(Collectors.toSet());
                Map<Integer, Map<String, String>> rowsByIndex = new TreeMap<>();
                allUserAnswers.stream()
                    .filter(ua -> ua.getRowIndex() > 0 && colKeys.contains(ua.getFieldKey()))
                    .forEach(ua -> rowsByIndex
                        .computeIfAbsent(ua.getRowIndex(), k -> new LinkedHashMap<>())
                        .put(ua.getFieldKey(), ua.getValue() != null ? ua.getValue() : ""));

                gridAnswers.put(grid.gridKey, new ArrayList<>(rowsByIndex.values()));
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

        // Save flat answers
        for (SectionData sd : loaded.sections().values()) {
            for (FieldDefinition field : sd.template().fields) {
                if (!incoming.containsKey(field.fieldKey)) continue;
                String value = incoming.get(field.fieldKey);
                if (field.storage != null && field.storage.type == StorageType.DEDICATED) {
                    String ref = field.storage.tableName + "." + field.storage.columnName;
                    DEDICATED_WRITERS.getOrDefault(ref, (a, v) -> {}).accept(app, value);
                } else if (field.storage != null && field.storage.type == StorageType.EAV) {
                    upsertFlatEav(app.getId(), field.fieldKey, value);
                }
            }

            // Save grid answers
            for (GridDefinition grid : sd.template().grids) {
                List<Map<String, String>> rows = incomingGrids.get(grid.gridKey);
                if (rows != null) {
                    saveGridRows(app.getId(), grid, rows);
                }
            }
        }
        appRepo.save(app);

        // Purge hidden flat fields
        Map<String, String> currentAnswers = buildCurrentAnswerMap(app, loaded);
        purgeHiddenFields(app, loaded, currentAnswers);

        // Compute and persist risk rating
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

        // Purge hidden flat fields
        Map<String, String> currentAnswers = buildCurrentAnswerMap(app, loaded);
        purgeHiddenFields(app, loaded, currentAnswers);
        Map<String, String> answersForValidation = buildCurrentAnswerMap(app, loaded);

        // Load grid answers for validation
        Map<String, List<Map<String, String>>> gridAnswers = loadGridAnswers(app.getId(), loaded);

        validateRequired(loaded, answersForValidation, gridAnswers);

        // Final rating before submit
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

    private void upsertFlatEav(Long appId, String fieldKey, String value) {
        UserAnswer ua = userAnswerRepo
            .findByApplicationIdAndFieldKeyAndRowIndex(appId, fieldKey, 0)
            .orElseGet(() -> new UserAnswer(appId, fieldKey, 0));
        ua.setValue(value);
        userAnswerRepo.save(ua);
    }

    private void saveGridRows(Long appId, GridDefinition grid, List<Map<String, String>> rows) {
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

    private Map<String, List<Map<String, String>>> loadGridAnswers(Long appId, LoadedTemplates loaded) {
        List<UserAnswer> gridUas = userAnswerRepo
            .findByApplicationIdAndRowIndexGreaterThanOrderByRowIndex(appId, 0);
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        for (SectionData sd : loaded.sections().values()) {
            for (GridDefinition grid : sd.template().grids) {
                Set<String> colKeys = grid.columns.stream()
                    .map(c -> c.fieldKey).collect(Collectors.toSet());
                Map<Integer, Map<String, String>> rowsByIndex = new TreeMap<>();
                gridUas.stream()
                    .filter(ua -> colKeys.contains(ua.getFieldKey()))
                    .forEach(ua -> rowsByIndex
                        .computeIfAbsent(ua.getRowIndex(), k -> new LinkedHashMap<>())
                        .put(ua.getFieldKey(), ua.getValue() != null ? ua.getValue() : ""));
                result.put(grid.gridKey, new ArrayList<>(rowsByIndex.values()));
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
                    DEDICATED_WRITERS.getOrDefault(ref, (a, v) -> {}).accept(app, null);
                    dedicatedDirty = true;
                }
            }
        }
        if (dedicatedDirty) appRepo.save(app);
    }

    private Map<String, String> buildCurrentAnswerMap(LoanApplication app, LoadedTemplates loaded) {
        Map<String, String> map = new LinkedHashMap<>();
        for (SectionData sd : loaded.sections().values()) {
            for (FieldDefinition field : sd.template().fields) {
                if (field.storage != null && field.storage.type == StorageType.DEDICATED) {
                    String ref = field.storage.tableName + "." + field.storage.columnName;
                    String value = DEDICATED_READERS.getOrDefault(ref, a -> null).apply(app);
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
            // Flat fields
            for (FieldDefinition field : sd.template().fields) {
                if (!ruleEvaluator.isVisible(field, answers)) continue;
                if (!ruleEvaluator.isRequired(field, answers)) continue;
                String value = answers.get(field.fieldKey);
                if (value == null || value.isBlank()) {
                    errors.add(new ValidationError(field.fieldKey,
                        "required-" + field.fieldKey, "REQUIRED", "validation.required"));
                }
            }
            // Grid columns (ROW-scoped rule: evaluator gets the row's own answers)
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
                .orElseThrow(() -> new IllegalStateException("No snapshot item for " + versionType));
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

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listAll() {
        return appRepo.findAllByOrderByIdDesc().stream()
            .map(this::toApplicationResponse)
            .collect(Collectors.toList());
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

    private String computeRating(LoanApplication app, String beanName, Map<String, String> answers) {
        if (beanName == null) return null;
        RiskRatingStrategy strategy = strategies.get(beanName);
        if (strategy == null) return null;
        // Enrich with grid-row count so strategies like ratingV2 can factor in collateral depth
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

    private record LoadedTemplates(TabTemplateJson tab, Map<String, SectionData> sections, String ratingBeanName) {}
}
