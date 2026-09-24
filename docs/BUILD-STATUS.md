# Build Status

Last updated: 2026-09-24 (overnight run)

---

## Overnight run — Bulk Upload feature (2026-09-23 → 2026-09-24)

Worked through `docs/feature-bulk-upload.md.html` Phase 0 → Phase 4 in order, unattended,
per the instructions left before going to bed. **All five phases completed; nothing is
blocked.** No genuine design ambiguity came up that needed a stop — two narrow judgment
calls were made along the way and are called out below so they're not silently baked in.

One naming note: the run instructions referred to `docs/Architecture.md`,
`docs/AI-Agent-Build-Instructions.md`, and `docs/feature-bulk-upload.md`. The actual files
in this repo are `docs/questionnaire-poc-architecture.md`,
`docs/questionnaire-poc-ai-agent-instructions.md`, and `docs/feature-bulk-upload.md.html`
(an HTML export, not a `.md` file — it was untracked at the start of the run and has now
been committed). Content matched the intended spec in all three cases; only the filenames
differed. Flagging this since it's worth checking whether the `.md.html` export is the
canonical copy or whether a plain `.md` was meant to replace it.

| Phase | Status | Commit |
|---|---|---|
| **Phase 0** — fix `questionnaire-admin-ui` grid-discard bug + stale proxy comment | Complete | `Phase 0: fix questionnaire-admin-ui grid-discard bug and stale proxy comment` |
| **Phase 1** — CODE_SET bulk upload | Complete | `Phase 1: CODE_SET bulk upload` |
| **Phase 2** — Questionnaire fields bulk upload (+ rule-preservation merge, + allowlist-at-upload, + dedicated-map hardening) | Complete | `Phase 2: questionnaire fields bulk upload` |
| **Phase 3** — Section rename via API | Complete | `Phase 3: section rename via API` |
| **Phase 4** — Field editor dropdown_source / static-values provisioning | Complete | `Phase 4: field editor dropdown_source / static-values provisioning` |

**Test suite: 72 tests, 72 passing, 0 failing** as of this run (backend 54 + `application-ui`
18 — see the updated Test coverage section below for the full breakdown, including the 15
tests added this run).

### Judgment calls made along the way (not blockers — noted for visibility)

1. **Phase 3 label-key on first save.** The spec scoped the rename support to
   `UpdateSectionRequest`/`updateDraft()` only, not `DraftRevisionRequest`/
   `createDraftRevision()`. Taken literally, editing the label on a section that has no
   existing DRAFT yet and clicking "Save draft" once would silently drop the rename (the
   first call is `createDraftRevision`, which doesn't carry `labelKey`). Rather than expand
   the backend's scope beyond what the spec asked for, `SectionEditor`'s save handler now
   makes a follow-up `updateDraft` call with the label change immediately after
   `createDraftRevision` when the label changed and this was the first save — same two
   endpoints the spec defined, just sequenced so one client action reliably renames a
   brand-new draft too.
2. **`lookup-stub-service` had no `GlobalExceptionHandler`** (unlike `questionnaire-service`,
   which already had one). Added a minimal one mapping `IllegalArgumentException` → 400, so
   the new bulk-upload endpoint's parsing errors (missing columns, bad rows) return a proper
   4xx with the message instead of a generic 500. Matches the existing pattern in
   `questionnaire-service` rather than inventing a new one.
3. **CSV parsing is a naive `split(",")`** in both new bulk-upload parsers (lookup-stub-service
   and questionnaire-service), matching the spec's explicit "simple split... if preferred"
   guidance. Does not handle quoted fields containing commas. Fine for the demo/admin-tool
   templates provided; would need a real CSV library if hand-authored files start using
   quoted commas.

### What's new (files, not previously in the module structure list below)

- `lookup-stub-service`: `CodeSetBulkUploadService`, `GlobalExceptionHandler`,
  `BulkUploadResponse` DTO, `CodeSetRepository.deleteByCodeSetType()`.
- `questionnaire-service`: `QuestionnaireFieldsBulkUploadService`,
  `BulkUploadFieldsResponse` DTO, `SectionTemplateService.updateDraft(..., labelKey)` overload.
- `application-service`: `ApplicationService.dedicatedWriter()`/`dedicatedReader()` helpers
  (throw `IllegalStateException` on an allowlist/wiring-map drift instead of silently
  no-opping — see Phase 2 commit message for the full rationale).
- `questionnaire-admin-ui`: top nav (Section Templates / Code Sets — there was none before),
  `CodeSetsPage`, bulk-upload button + label-key input + dropdown-source controls in
  `SectionEditor`, `apiUploadFile()` shared multipart helper.
- New static template files under `questionnaire-admin-ui/public/`: `codeset-template.csv`,
  `questionnaire-fields-template.csv`, `questionnaire-fields-grid-template.csv`.

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
| `QuestionnaireFieldsBulkUploadIntegrationTest` | 3 | Creates a DRAFT from a file when none exists; rejects a DEDICATED field not in the allowlist with the specific ref named; full-replace upload preserves visibilityRules/validationRules on every existing fieldKey it didn't touch (financial-details, 15 fields, 3 conditional Yes/No/N-A pairs) |
| `SectionRenameIntegrationTest` | 1 | Rename via `updateDraft` survives `publish` and shows up in both the single-section GET and the section list |

**9 tests pass.**

**Gap (pre-existing, narrowed but not closed):** Still no `@SpringBootTest` integration tests for `TabTemplateService`, and no coverage of `SectionTemplateService.create()`/`publish()` in isolation from the two new integration tests above (which exercise them indirectly). The build instructions require dedicated create→publish lifecycle tests; the two new test classes cover the bulk-upload and rename paths specifically, not the general case.

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
| `CodeSetBulkUploadIntegrationTest` | 4 | Full replace per type + untouched types + new type in one upload; `display_order` defaults to file row order when omitted; XLSX parsing (not just CSV); rejects a file missing a required column |

**9 tests pass.** Sanity-level coverage as prescribed (throwaway stub), extended to cover the new bulk-upload endpoint.

### application-ui (Vitest)

| Test file | Tests | What it covers |
|---|---|---|
| `ruleEvaluator.test.js` | 8 | Client-side rule evaluator mirrors server-side: visibility, conditional required, ROW scope |
| `FieldRenderer.test.jsx` | 3 | Text, number, dropdown field rendering from template JSON |
| `SectionRenderer.test.jsx` | 7 | Full section render, visibility toggle on answer change, required marker |

**All 18 tests pass.**

### questionnaire-admin-ui

**No tests.** This is the only component that does not meet Requirement 11 — and as of this
run it carries meaningfully more untested logic than before (Code Sets page, bulk-upload
flows, label rename, dropdown-source editor). Verified manually instead: `npm run build`
after every change, plus one live round-trip against a running `questionnaire-service` for
the Phase 4 acceptance criterion (see the Phase 4 commit message). No browser/E2E check was
done for any of the four phases' UI — genuinely unverified beyond "it builds and the API
contracts it calls are tested server-side."

### Total: 72 tests, 72 passing, 0 failing

---

## Known issues and TODOs

| # | Area | Issue | Priority |
|---|---|---|---|
| 1 | `questionnaire-admin-ui` | No test suite (Requirement 11 gap) — now covers Code Sets, bulk upload, rename, and dropdown-source editing with zero automated coverage | Medium |
| 2 | `questionnaire-service` | No `@SpringBootTest` integration tests for the general template lifecycle (`create`, `publish` in isolation) — the two new integration tests cover bulk-upload and rename specifically | Medium |
| 3 | `TemplateStatus` enum | Missing `RETIRED` value — superseded versions stay `ACTIVE` (functionally correct for POC, but wrong for production `listSections()` filtering) | Low |
| 4 | `LABEL_TRANSLATION` table | Not built; keys rendered raw in UI | Low (deferred by design) |
| 5 | Phase 7 findings doc | No load-test or Oracle-gap findings written (build instructions require it) | Low |
| 6 | Slice F walkthrough | No prose walkthrough alongside the automated tests | Low |
| 7 | `questionnaire-admin-ui` | No tab-level template editor (sections only); publish triggers `cloneForward` in application-service but no UI surface for confirming the new snapshot code | Low |
| 8 | `application-ui` | `GridRenderer` has no Vitest tests (only `SectionRenderer` and `FieldRenderer` covered) | Low |
| 9 | Bulk-upload CSV parsing | Naive `split(",")` in both new parsers — doesn't handle quoted fields containing commas. Matches the spec's explicit guidance; would need a real CSV library if hand-authored files start using quoted commas | Low |
| 10 | `questionnaire-admin-ui` grid editing | Still out of scope (per Phase 0's own fix — grids round-trip unmodified but aren't editable in this UI); grid bulk-upload (Phase 2) is the only way to change grid columns without a direct API call | Low |
