# Schema Alignment Instructions

Compares **actual H2 schema** (what was built) against **architecture-doc design** (what was specified),
identifies gaps, and gives change-by-change instructions for aligning them before production.

Ignoring `LABEL_TRANSLATION` (explicitly deferred) and `EXTERNAL_LOOKUP_OPTION` (stub-only concern).

---

## Differences at a glance

| # | Table / Column | Architecture doc | Actual H2 | Service | Priority |
|---|---|---|---|---|---|
| 1 | `tab_template_section` | Separate DB table (FK to tab_template) | **Missing — stored as JSON inside `tab_template.template_json`** | questionnaire-service | **High** |
| 2 | `tab_template` | `effective_start`, `effective_end` columns | Not present | questionnaire-service | Low |
| 3 | `section_template` | `effective_start`, `effective_end` columns | Not present | questionnaire-service | Low |
| 4 | `loan_party` | Column names: `loan_application_id`, `party_role` | `application_id`, `role` | application-service | Low |
| 5 | `loan_party` | Includes `annual_income`, `employment_status`, `credit_score` | These columns don't exist — moved to Phase 7 dedicated tables | application-service | **Don't add — actual is better (see § Scalability)** |
| 6 | `user_answers` | Column names: `submission_id`, `raw_value` | `application_id`, `answer_value` | application-service | Low |
| 7 | `config_snapshot_item` | No `entity_id` column | Has `entity_id` (carries tabId for TAB_TEMPLATE items) | application-service | Keep as-is |

---

## Change 1 — Create `tab_template_section` table (HIGH priority)

This is the only structurally significant gap. The doc specifies a real FK table; the POC embedded
section refs as a JSON array inside `tab_template.template_json`. The table approach is needed for
production (pre-21c Oracle) because it makes tab-composition queries SQL-level — no JSON scanning.

### 1a. New entity (questionnaire-service)

```java
// questionnaire-service/.../entity/TabTemplateSection.java
@Entity
@Table(name = "tab_template_section")
public class TabTemplateSection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tab_template_id", nullable = false)
    private Long tabTemplateId;

    @Column(name = "section_id", nullable = false, length = 100)
    private String sectionId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // getters / setters
}
```

### 1b. New repository

```java
// TabTemplateSectionRepository.java
public interface TabTemplateSectionRepository extends JpaRepository<TabTemplateSection, Long> {
    List<TabTemplateSection> findByTabTemplateIdOrderByDisplayOrder(Long tabTemplateId);
    void deleteByTabTemplateId(Long tabTemplateId);
}
```

### 1c. Update `TabTemplate` entity

`template_json` currently carries both the section list and any other tab metadata. Once
`tab_template_section` rows exist, remove section refs from the JSON payload. Keep `template_json`
only for future tab-level metadata (layout hints, etc.) — or drop the column entirely if nothing
else uses it.

```java
// Remove or retain as empty-object placeholder:
@Lob
@Column(name = "template_json")
private String templateJson; // can be dropped after migration
```

### 1d. Update `TabTemplateService` (questionnaire-service)

Replace all JSON array parsing for section refs with DB queries:

```java
// When publishing a tab version, persist section refs:
public TabTemplate publishTabTemplate(String tabId, List<TabSectionRef> sections) {
    TabTemplate tab = buildAndSave(tabId);
    sectionRepo.deleteByTabTemplateId(tab.getId());
    IntStream.range(0, sections.size()).forEach(i -> {
        TabTemplateSection row = new TabTemplateSection();
        row.setTabTemplateId(tab.getId());
        row.setSectionId(sections.get(i).sectionId());
        row.setDisplayOrder(i + 1);
        sectionRepo.save(row);
    });
    return tab;
}

// When reading sections for a tab:
public List<TabTemplateSection> getSections(Long tabTemplateId) {
    return sectionRepo.findByTabTemplateIdOrderByDisplayOrder(tabTemplateId);
}
```

### 1e. Update `DataInitializer` (questionnaire-service)

`seedProposalTab()` currently writes section refs into the JSON. Change it to insert rows:

```java
// Before (JSON-based):
new TabSectionRef("key-information", 1), ...

// After (row-based): after saving the TabTemplate entity, insert section rows
insertSection(tabId, "key-information",    1);
insertSection(tabId, "associated-records", 1);
insertSection(tabId, "guarantors",         1);
// ... etc.
```

### 1f. Update `ApplicationService` (application-service)

The render path currently calls `QuestionnaireClient.getTabTemplate()` and parses section refs
from the returned JSON. Change it to call a new endpoint that returns the section list separately,
or expand the existing response DTO to include `sections` from the DB join rather than JSON parsing.

```java
// QuestionnaireClient — new method or extend existing:
List<TabSectionRef> sections = questionnaireClient.getTabSections(tabId, tabVersion);
```

### 1g. Data migration (existing H2 rows)

On first startup after the change, the `DataInitializer` guard (`if (repo.count() > 0) return;`)
means migration only runs on a fresh DB. For production Oracle, write a one-time migration script:

```sql
-- Pseudocode — parse JSON via application layer, not SQL, on pre-21c Oracle
-- Run this Java migration once:
tabTemplateRepository.findAll().forEach(tab -> {
    List<TabSectionRef> refs = objectMapper.readValue(tab.getTemplateJson(), ...);
    refs.forEach(ref -> insertTabTemplateSection(tab.getId(), ref.sectionId, ref.order));
});
```

---

## Change 2 — Add `effective_start` / `effective_end` to templates (LOW priority)

Both `section_template` and `tab_template` are missing these date-range columns present in the
doc schema. They support querying "which template version was active on date X?" without scanning
status + version logic in application code.

### Schema additions

```java
// SectionTemplate.java — add:
@Column(name = "effective_start")
private LocalDate effectiveStart;

@Column(name = "effective_end")
private LocalDate effectiveEnd;

// TabTemplate.java — same two fields
```

Set `effective_start = now()` when status changes to `ACTIVE`; set `effective_end = now() - 1 day`
on the previous ACTIVE version when it is retired. `SectionTemplateService.publish()` and
`TabTemplateService.publish()` are the right place to set these.

---

## Change 3 — Rename `loan_party` columns (LOW priority)

Column names in the actual H2 entity differ from the doc. This only matters for SQL readability
and production Oracle schema conventions — behaviour is identical.

| Actual | Doc | Change |
|---|---|---|
| `application_id` | `loan_application_id` | Rename column + update `@Column` annotation + all JPQL queries |
| `role` | `party_role` | Rename column + update `@Column` annotation |

```java
// LoanParty.java — update annotations:
@Column(name = "loan_application_id", nullable = false)
private Long applicationId;

@Column(name = "party_role", nullable = false)
@Enumerated(EnumType.STRING)
private LoanPartyRole role;
```

No logic changes — purely column name alignment.

---

## Change 4 — Rename `user_answers` columns (LOW priority)

| Actual | Doc | Change |
|---|---|---|
| `application_id` | `submission_id` | Rename column only — same FK semantics |
| `answer_value` | `raw_value` | Rename column only |

Same pattern as Change 3 — only `@Column(name=...)` annotations and any native SQL.

---

## Change 5 — Keep `config_snapshot_item.entity_id` (no change needed)

The doc does not include this column. It was added during implementation to carry the `tabId` for
`TAB_TEMPLATE` snapshot items (the tab has a logical entity identity separate from version).
This is a correct implementation detail the doc didn't anticipate — keep it.

---

## What NOT to change — Phase 7 dedicated tables are better than the doc

The architecture doc's `LOAN_PARTY` included `annual_income`, `employment_status`, `credit_score`
directly on the table. Phase 7 instead created separate dedicated tables:
`loan_financial_summary`, `loan_compliance_record`, `loan_employment_detail`, `loan_property_info`.

**Do not roll these back to a fat `LOAN_PARTY` row.** The dedicated table approach is strictly
better (see Scalability section below).

---

## Scalability comparison — which ER design is better?

### questionnaire-service: architecture doc wins

| Factor | Doc (tab_template_section table) | Actual (JSON in tab_template) |
|---|---|---|
| "Which tabs use section X?" query | `SELECT tab_template_id FROM tab_template_section WHERE section_id = 'X'` — one indexed SQL query | Load all tab JSON rows, parse each one in application code — O(N tabs) |
| Pre-21c Oracle friendliness | Full SQL, uses B-tree index on `section_id` | `JSON_TABLE` / `JSON_VALUE` over a CLOB — no B-tree index, CPU-per-row scan |
| Tab composition audit | Row-level insert/delete per change — standard DB audit tools work | Full JSON diff required to detect what changed |
| Clone-forward publish | Copy section rows for new tab version | Copy one JSON column — simpler |
| Admin UI "sections in tab" | Single JOIN | JSON parse per tab |

**Verdict for questionnaire-service:** the doc approach is more scalable. The architecture's
Decision 1 reasoning ("materializing the columns that get queried constantly is worth the schema
surface on pre-21c Oracle") applies here with equal force. `tab_template_section` as a table is
the right production design. The POC shortcut (JSON) is acceptable now but must be addressed
before production.

### application-service: actual H2 schema wins (Phase 7 exceeded the doc)

The doc proposed a single `LOAN_PARTY` table with financial and employment columns. Phase 7 split
these into domain-scoped dedicated tables. This is better for several reasons:

| Factor | Doc (fat LOAN_PARTY) | Actual (dedicated extension tables) |
|---|---|---|
| Row width | Grows without bound as new fields added | Each table grows only in its own domain |
| Column nullability | Many nullable columns on one row | Each table has a clear purpose; all fields belong |
| Independent query | All domain fields require a full loan_party scan | Query only the domain you need |
| 200+ field scale | Single wide table becomes unwieldy | Each section's 20–30 columns live in their own table |
| Schema evolution | Adding a compliance field touches loan_party | Add to loan_compliance_record only |

**Verdict for application-service:** Phase 7 dedicated tables are the correct production design.
The doc was a placeholder — it didn't anticipate the 200+ field requirement in the data model
itself. The implemented approach is the right answer.

### Overall recommendation

| Area | Best design | Action |
|---|---|---|
| Tab composition | Architecture doc (`tab_template_section` table) | Implement Change 1 before production |
| Effective dating | Architecture doc (`effective_start`, `effective_end`) | Low-effort; do with Change 1 |
| Column naming | Architecture doc (more descriptive) | Low-effort rename; cosmetic |
| Loan domain extensions | Actual H2 (Phase 7 dedicated tables) | Keep as-is; update doc ERD |
| EAV answers | Both are identical in substance | No change |
| Config snapshot | Both are identical | No change |

The single most impactful change before going to production is **Change 1**: adding the
`tab_template_section` table. Every other difference is naming convention or a low-effort
additive column. The Phase 7 dedicated tables are an improvement over the doc and should be
reflected in the architecture doc's ERD rather than rolled back.
