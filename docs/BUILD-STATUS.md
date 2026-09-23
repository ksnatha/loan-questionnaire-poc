# Build Status

Last updated: 2026-09-23

---

## Phase / Slice completion

| Phase / Slice | Status | Notes |
|---|---|---|
| **Phase 0** — Workspace setup | Complete | All 5 modules build; each service has a port and H2 DB; both React apps serve |
| **Phase 1** — Contract gate | Complete | `docs/contracts.md`, `docs/decisions.md`, and all common POJOs in `common/` |
| **Slice A** — Walking skeleton | Complete | 3 DEDICATED fields, create→save-draft→submit, real pipe end-to-end |
| **Slice B** — EAV + CODE_SET | Complete | All EAV fields, `USER_ANSWERS`, `lookup-stub-service` CODE_SET with date filtering |
| **Slice C** — Conditionals | Complete | Visibility and validation rule engines (server-side + client-side), hidden-field purge |
| **Slice D** — Grids | Complete | EAV grid (Collateral Properties), `row_index` in `USER_ANSWERS`, add/remove rows in UI |
| **Slice E** — Borrower data + rating | Complete | `LoanParty`, `RiskRatingStrategy` dispatch via Spring bean-name map, `ratingV1`, live recompute |
| **Slice F** — Three-axis versioning | Complete | All 3 axes as automated integration tests (see test coverage section) |
| **Phase 7** — Hardening / 200+ fields | Complete | 7 sections, 200+ fields across Proposal tab; Phase 7 dedicated extension tables added |
| **Phase 8** — questionnaire-admin-ui | Complete (functional) | Section template JSON editor with draft/publish workflow; see known gaps below |

---

## Deviations from the architecture / build instructions

### Improvements over the spec

**1. Phase 7 dedicated extension tables (application-service)**
The architecture doc put financial, employment, and compliance columns directly on `LOAN_PARTY`. Phase 7 introduced five dedicated domain tables instead:
`loan_financial_summary`, `loan_compliance_record`, `loan_employment_detail`, `loan_property_info`, `loan_guarantor`.
This is strictly better than the doc's design (narrower rows, schema evolution by domain, supports 200+ fields without a fat single table) and should be treated as the authoritative design going forward.

**2. `config_snapshot_item.entity_id` column**
The architecture doc's `CONFIG_SNAPSHOT_ITEM` schema has no `entity_id` column. The implementation adds it to carry the `tabId` for `TAB_TEMPLATE` items (the tab has a logical identity separate from its version integer). This is a correct detail the doc didn't anticipate — kept as-is.

**3. `ratingV2` strategy implemented**
Slice F's build instructions described a `ratingV2` that factors in the collateral grid. The implementation includes a full `RatingV2Strategy` bean (not just the dispatch wiring), making Slice F's axis-3 test self-contained.

### Gaps from the spec (now partially closed)

**4. `tab_template_section` table — closed in latest commit**
The architecture doc specifies a proper `TAB_TEMPLATE_SECTION(id, tab_template_id FK, section_id, display_order)` table. The initial POC stored section refs as a JSON array inside `tab_template.template_json`. This was identified as the most significant production gap (pre-21c Oracle cannot efficiently query JSON CLOBs). The table now exists and is populated by `DataInitializer` on startup.

**5. `effective_start` / `effective_end` on templates — closed in latest commit**
Both `section_template` and `tab_template` now carry these columns. They are set on `publish()`: `effective_start = today` on the newly activated version; `effective_end = today - 1` on the previously active version. Old versions retain `ACTIVE` status so `CONFIG_SNAPSHOT`-pinned loans can still fetch them by version number; the date columns serve audit/query purposes only.

### Remaining gaps from the spec (not yet implemented)

**6. `LABEL_TRANSLATION` table — explicitly deferred**
All label values are stored as translation keys (`label_key`) in every entity and template, satisfying Decision 9's "i18n from day one" requirement. The `LABEL_TRANSLATION` table itself (and locale-based resolution) is not built. The POC renders keys directly. Deferred by design.

**7. `EXTERNAL` dropdown type — stub only**
`DropdownSource.type = EXTERNAL` is handled in `DropdownHydrationService` by delegating to `LookupClient.getLookup(sourceKey)`. The lookup-stub-service's `DataInitializer` does not seed any EXTERNAL lookup data. The code path exists and is tested; no real external source is wired.

**8. `TemplateStatus` has no `RETIRED` value**
The architecture doc references a `RETIRED` status for template versions superseded by a newer publish. The enum currently has only `DRAFT` and `ACTIVE`. Old active versions are date-closed via `effective_end` but keep `ACTIVE` status. This is functionally correct for the POC (pinned snapshots still resolve), but the enum should grow a `RETIRED` value before production so `listSections()` can cleanly exclude superseded versions.

**9. Slice F — no written walkthrough doc**
The build instructions require "a short written or recorded walkthrough for human review" alongside the automated tests. The three tests in `VersioningIntegrationTest` exist and pass, but no prose walkthrough doc was produced.

**10. Phase 7 — no findings / load-test doc**
The build instructions require "a written findings doc validating or revising the enterprise-ready claim with real numbers." Not produced; the 200+ fields exist and the service boots cleanly, but no formal load-test or Oracle-specific performance notes were written.

**11. questionnaire-admin-ui — no tests**
The build instructions call for full coverage of both React UIs' generic renderers. `application-ui` has Vitest tests; `questionnaire-admin-ui` has none. This is the only component missing its test suite.

---

## Module and file structure (as built)

```
loan-questionnaire-poc/
├── common/                         # Shared POJOs for template JSON (no Spring, no JPA)
│   └── src/main/java/com/example/common/template/
│       ├── SectionTemplateJson.java, TabTemplateJson.java, TabSectionRef.java
│       ├── FieldDefinition.java, FieldType.java, FieldOption.java
│       ├── GridDefinition.java, StorageDefinition.java, StorageType.java
│       ├── DropdownSource.java, DropdownSourceType.java
│       ├── VisibilityRule.java, ValidationRule.java, ValidationType.java
│       └── RuleCondition.java, RuleOperator.java, RuleScope.java
│
├── questionnaire-service/          # port 8082 — template authoring + publishing
│   └── src/main/java/com/example/questionnaire/
│       ├── entity/   SectionTemplate, TabTemplate, TabTemplateSection, TemplateStatus
│       ├── repository/   SectionTemplateRepository, TabTemplateRepository,
│       │                 TabTemplateSectionRepository
│       ├── service/  SectionTemplateService, TabTemplateService, AllowlistValidator
│       ├── controller/   SectionTemplateController, TabTemplateController,
│       │                 GlobalExceptionHandler
│       ├── dto/      CreateSectionRequest, CreateTabRequest, DraftRevisionRequest,
│       │             UpdateSectionRequest, SectionTemplateResponse, TabTemplateResponse,
│       │             SectionListItem
│       └── init/     DataInitializer  (seeds 7 sections + Proposal tab on startup)
│   └── src/test/...  AllowlistValidatorTest
│
├── application-service/            # port 8083 — loan lifecycle + rendering + rating
│   └── src/main/java/com/example/loanapp/
│       ├── entity/   LoanApplication, ApplicationStatus, ApplicationNumberCounter,
│       │             ConfigSnapshot, ConfigSnapshotItem, SnapshotStatus,
│       │             UserAnswer, LoanParty, LoanPartyRole,
│       │             LoanFinancialSummary, LoanComplianceRecord,
│       │             LoanEmploymentDetail, LoanPropertyInfo, LoanGuarantor
│       ├── repository/   (one per entity above, plus ApplicationNumberCounterRepository)
│       ├── service/  ApplicationService, SnapshotService, RuleEvaluator,
│       │             DropdownHydrationService, RiskRatingStrategy (interface),
│       │             RatingV1Strategy, RatingV2Strategy, ValidationException
│       ├── controller/   ApplicationController, SnapshotController,
│       │                 LoanPartyController, GlobalExceptionHandler
│       ├── client/   QuestionnaireClient, LookupClient
│       ├── dto/      (ApplicationResponse, RenderResponse, RenderedTab, RenderedSection,
│       │              SaveDraftRequest/Response, SubmitResponse, CreateApplicationRequest,
│       │              CloneForwardRequest, LoanPartyDto, SnapshotResponse,
│       │              ValidationError/ErrorResponse)
│       └── init/     DataInitializer  (seeds CONFIG_SNAPSHOT V1 on startup)
│   └── src/test/...  ApplicationIntegrationTest, VersioningIntegrationTest,
│                     ApplicationNumberCounterTest, RatingDispatchTest,
│                     RatingV1StrategyTest, RuleEvaluatorTest,
│                     DropdownHydrationServiceTest
│
├── lookup-stub-service/            # port 8081 — CODE_SET reference data
│   └── src/main/java/com/example/lookup/
│       ├── entity/     CodeSet
│       ├── repository/ CodeSetRepository
│       ├── controller/ CodeSetController
│       └── init/       DataInitializer  (seeds 15+ CODE_SET types)
│   └── src/test/...    CodeSetRepositoryTest
│
├── application-ui/                 # port 5173 — React loan application form
│   └── src/
│       ├── components/   LoanApplicationForm, SectionRenderer, FieldRenderer, GridRenderer
│       ├── ruleEvaluator.js
│       └── __tests__/    FieldRenderer.test.jsx, SectionRenderer.test.jsx,
│                         ruleEvaluator.test.js
│
├── questionnaire-admin-ui/         # port 5174 — React template editor
│   └── src/
│       ├── App.jsx  (section list → draft/edit → publish workflow)
│       └── (no tests — see known gaps)
│
└── docs/
    ├── questionnaire-poc-architecture.md
    ├── questionnaire-poc-ai-agent-instructions.md
    ├── questionnaire-poc-sample-data.md
    ├── decisions.md
    ├── contracts.md
    ├── er-diagram-actual.html       (actual H2 schema ER diagram)
    ├── er-diagram-design.html       (architecture-doc design ER diagram)
    ├── schema-alignment-instructions.md
    ├── loan-app-ui-design.html
    ├── questionnaire-fields-design.html
    └── BUILD-STATUS.md              (this file)
```

---

## Test coverage (Requirement 11)

### questionnaire-service

| Test class | Tests | Coverage |
|---|---|---|
| `AllowlistValidatorTest` | 5 | DEDICATED allowlist accept/reject, grid column validation, duplicate fieldKey detection |

**Gap:** No `@SpringBootTest` / `MockMvc` integration tests for `SectionTemplateService` or `TabTemplateService`. The build instructions require integration tests covering create→publish lifecycle and the DEDICATED-rejection at publish time. The unit test covers the validator in isolation; a full `@DataJpaTest` wiring through the service layer is missing.

### application-service

| Test class | Tests | What it covers |
|---|---|---|
| `ApplicationIntegrationTest` | 6 | create→save-draft→submit (DEDICATED fields); DEDICATED+EAV mixed save; hidden-field server-side purge; validation rejects missing required fields; grid rows persist with row identity; `LoanParty` add/edit |
| `VersioningIntegrationTest` | 3 | Axis 1 — in-flight loan pinned to old section version; Axis 2 — CODE_SET label resolves as of creation date; Axis 3 — in-flight loan keeps old rating strategy after clone-forward |
| `ApplicationNumberCounterTest` | 1 | Sequential IDs generated without collision |
| `RatingDispatchTest` | 2 | Dispatch is a pure map lookup (throwaway `ratingV3` in test proves zero dispatch code change needed); `ratingV1` is in the map |
| `RatingV1StrategyTest` | 11 | Threshold tiers, collateral bump, null amount, edge cases |
| `RuleEvaluatorTest` | 9 | Visibility Demo 1, conditional required Demo 2, base required, ROW-scope reuses same evaluator class (no branching), missing trigger field |
| `DropdownHydrationServiceTest` | 4 | STATIC, CODE_SET, EXTERNAL delegation; null source |

**All 36 tests pass.** No failures or skips.

### lookup-stub-service

| Test class | Tests | What it covers |
|---|---|---|
| `CodeSetRepositoryTest` | 5 | Date-filter logic: active, expired, future-dated, inactive-flag, `asOf=today` |

Sanity-level coverage as prescribed (throwaway stub).

### application-ui (Vitest)

| Test file | Tests | What it covers |
|---|---|---|
| `ruleEvaluator.test.js` | 8 | Client-side rule evaluator mirrors server-side: visibility, conditional required, ROW scope |
| `FieldRenderer.test.jsx` | 3 | Text, number, dropdown field rendering from template JSON |
| `SectionRenderer.test.jsx` | 7 | Full section render, visibility toggle on answer change, required marker |

**All 18 tests pass.**

### questionnaire-admin-ui

**No tests.** This is the only component that does not meet Requirement 11.

### Total: 64 tests, 64 passing, 0 failing

---

## Known issues and TODOs

| # | Area | Issue | Priority |
|---|---|---|---|
| 1 | `questionnaire-admin-ui` | No test suite (Requirement 11 gap) | Medium |
| 2 | `questionnaire-service` | No `@SpringBootTest` integration tests for template lifecycle (create, publish, draft-revision) | Medium |
| 3 | `TemplateStatus` enum | Missing `RETIRED` value — superseded versions stay `ACTIVE` (functionally correct for POC, but wrong for production `listSections()` filtering) | Low |
| 4 | `LABEL_TRANSLATION` table | Not built; keys rendered raw in UI | Low (deferred by design) |
| 5 | Phase 7 findings doc | No load-test or Oracle-gap findings written (build instructions require it) | Low |
| 6 | Slice F walkthrough | No prose walkthrough alongside the automated tests | Low |
| 7 | `questionnaire-admin-ui` | No tab-level template editor (sections only); publish triggers `cloneForward` in application-service but no UI surface for confirming the new snapshot code | Low |
| 8 | `application-ui` | `GridRenderer` has no Vitest tests (only `SectionRenderer` and `FieldRenderer` covered) | Low |
