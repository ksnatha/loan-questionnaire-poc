# Dynamic Questionnaire Engine POC — AI Agent Build Instructions

Read alongside **Architecture.md** — that document has the *why* behind every
decision referenced here (storage options, versioning options, dispatch
pattern, full domain model, ERD). This document is the *build order*: what to
hand Claude Code, in what sequence, and what "done" looks like at each step.

**Approach: walking skeleton, not module-by-module.** Instead of finishing
questionnaire-service completely, then application-service completely, then
the UI completely, build one thin slice through all three at once, get it
running end-to-end, then add one capability at a time. This surfaces contract
mismatches in Slice A instead of at the very end, and keeps each Claude Code
session's blast radius small enough to actually review.

Stack: Java, Spring Boot, JPA/Hibernate, H2 (POC) / Oracle pre-21c (production
target). Five components per Architecture.md.

---

## Phase 0 — Workspace setup

Multi-module Gradle (or Maven) workspace: `questionnaire-service`,
`application-service`, `lookup-stub-service`, a shared `common` module for
template JSON model classes, plus two separate React directories
(`application-ui`, `questionnaire-admin-ui`). Each backend module gets its own
file-based H2 database and its own port. Root README naming the five
components. No business logic yet — should build clean with empty
controllers.

**Acceptance:** all modules build; each service serves an empty
`/actuator/health`; both React apps show a placeholder page.

---

## Phase 1 — The contract (hard gate — review before Slice A)

Produce, as documentation plus shared POJOs/DTOs in `common` (not full
implementations):

1. **Section template JSON schema** — per Architecture.md Decision 1 (Option
   C: `template_json` + `has_grid`/`dedicated_column_refs`), Decision 4
   (`dropdown_source.type` = `STATIC`/`CODE_SET`/`EXTERNAL`), Decision 7
   (`visibility_rule`/`validation_rule` with explicit `scope`).
2. **Tab template schema** — Decision 5, independent versioning from sections.
3. **CONFIG_SNAPSHOT / CONFIG_SNAPSHOT_ITEM schema** — Decision 2, including
   the clone-forward publish contract (what a "publish new snapshot" API call
   actually does).
4. **REST contracts**: questionnaire-service (template + tab lifecycle),
   application-service (create/save-draft/submit/get-for-render, snapshot
   resolution), lookup-stub-service (`/code-sets/{type}?asOf=`,
   `/code-sets/{type}/{code}`, `/lookups/{sourceKey}`).
5. **`/docs/decisions.md`** — one paragraph each on: dropdown snapshot-vs-live
   policy, mid-flight snapshot-change behavior (already decided: stay pinned
   until resubmission), and anything else Architecture.md left as "decide
   during build."

**Acceptance:** you review and approve before Slice A starts. Don't let a
session proceed past this gate without explicit sign-off.

---

## Slice A — Walking skeleton

**Scope, deliberately minimal:** Key Information's 3 DEDICATED fields only
(`proposal_name`, `loan_amount`, `proposal_description`). No EAV, no
conditionals, no grids, no rating engine yet.

- questionnaire-service: author + publish one `SECTION_TEMPLATE` (Key
  Information, DEDICATED fields only) and one `TAB_TEMPLATE` (Proposal,
  referencing it).
- application-service: one `CONFIG_SNAPSHOT` with a `TAB_TEMPLATE` item
  pointing at it; `LoanApplication` create (DRAFT, human-readable ID via
  `application_number_counter`) → fetch-for-render → save → submit.
- application-ui: render the tab, render the section, render three DEDICATED
  fields, save, submit.

**Acceptance:** a loan application can be created, rendered in React, saved,
and submitted, entirely through the real pipe — questionnaire-service →
application-service → application-ui — with nothing stubbed or hardcoded on
the UI side. This is the proof that the pipe itself works before anything
complex is layered on.

---

## Slice B — EAV + CODE_SET

- Add Key Information's 2 EAV fields (`has_existing_relationship`,
  `existing_relationship_id` — no conditional logic yet, just render + save
  + persist to `USER_ANSWERS`).
  fields (`loan_purpose`, `loan_term_months`, `repayment_frequency`,
  `interest_rate_type`, `rate_cap_percentage`, `requires_collateral`,
  `additional_comments`).
- lookup-stub-service: `CODE_SET` table (mirrored exactly per Architecture.md
  — real production columns), seeded for `LOAN_PURPOSE`, `REPAYMENT_FREQ`,
  `INTEREST_RATE_TYPE`. `GET /code-sets/{type}?asOf=` and
  `GET /code-sets/{type}/{code}`.
- application-service: dropdown hydration through the one client interface
  covering `STATIC`/`CODE_SET`/`EXTERNAL` (Decision 4), snapshot-vs-live
  policy per `/docs/decisions.md`.

**Acceptance:** a section template with both DEDICATED and EAV fields renders
and saves correctly; a CODE_SET-sourced dropdown resolves through the real
lookup-stub-service call.

---

## Slice C — Conditionals

- `visibility_rule` engine, scope `SECTION`: Demo 1 —
  `existing_relationship_id` shown only when `has_existing_relationship =
  Yes`.
- `validation_rule` engine, scope `SECTION`: Demo 2 — `rate_cap_percentage`
  always visible, required only when `interest_rate_type = VARIABLE`.
- Both server-side (never trust client-side visibility state on submit) and
  client-side (for responsive UX) — same rule JSON, same evaluator logic on
  both sides per Architecture.md Decision 7.
- Purge any answer value submitted under a hidden field.

**Acceptance:** both conditional examples work correctly, are enforced
server-side even if bypassed client-side, and the rule evaluator has no
field-specific branching in it — it's generic over any `SECTION`-scoped rule.

---

## Slice D — Grids

- Associated Records section: Collateral Properties grid (`property_address`,
  `property_type` [CODE_SET `PROPERTY_TYPE`], `estimated_value`,
  `lien_position` [CODE_SET `LIEN_POSITION`], `notes`), `row_index` in
  `USER_ANSWERS`.
- application-ui: add/remove row.
- Confirm the same rule evaluator from Slice C handles a `ROW`-scoped rule
  without special-casing (add one simple row-scoped example if none exists
  yet from earlier slices).

**Acceptance:** multiple collateral rows can be added, saved, and rendered
back correctly with row identity preserved.

---

## Slice E — Borrower data + rating engine

- `LoanParty` table (dedicated, role-discriminated) — add/edit
  borrower/co-borrower on a loan application.
- `RiskRatingStrategy` interface + `@Component` implementations, dispatched
  via `Map<String, RiskRatingStrategy>` keyed by bean name (Architecture.md
  Decision 3) — **not** a version-number switch.
- `CONFIG_SNAPSHOT_ITEM` gets a `RATING_LOGIC` row pointing at
  `strategy_bean_name = "ratingV1"`.
- v1 strategy: loan-amount-threshold base rating, bumped one tier if
  `requires_collateral = No` — Demo 3.
- Live recompute: every `save-draft` call recomputes and returns
  `risk_rating` against current answers and the loan's pinned snapshot.

**Acceptance:** toggling `requires_collateral` on a draft visibly changes the
displayed rating on the next save, with zero `if`/`switch` on version number
anywhere in the dispatch path.

---

## Slice F — Three-axis versioning demonstration

This is the actual point of the POC — treat it as more important than the
hardening numbers in the next phase.

### Axis 1 — UI/template versioning
1. Create loan A under the current `ACTIVE` snapshot.
2. Publish a new Additional Information section version (clone-forward a new
   snapshot per Architecture.md Decision 2, overriding only the
   `TAB_TEMPLATE` item).
3. Reopen A (still `DRAFT`) — renders under its originally pinned snapshot,
   unchanged.
4. Create loan B fresh — renders under the new snapshot.
5. Confirm `TAB_TEMPLATE` version only bumps on section *composition*
   changes, not content-only changes within an existing section.

### Axis 2 — reference/static data (code_set) versioning
1. On loan A, set `loan_purpose = REFI`.
2. Change `REFI`'s label the correct way in code_set — close the old row
   (`end_date`), insert a new row with a new `start_date`.
3. Re-render loan A — the stored key `REFI` still resolves to the label
   active on A's original creation date, per the snapshot/live policy in
   `/docs/decisions.md`.
4. Fetch `LOAN_PURPOSE` `asOf=today` — new loans see the new label.

### Axis 3 — business rule / rating-logic versioning
1. Compute loan A's rating under its pinned snapshot's `RATING_LOGIC` item
   (`ratingV1`).
2. Clone-forward a new snapshot with `RATING_LOGIC` repointed to a new
   `@Component` (`ratingV2`) that also factors in the Collateral Properties
   grid.
3. Confirm loan A, still pinned to its original snapshot, keeps computing
   under `ratingV1` on every save — even though the new snapshot is now
   `ACTIVE`.
4. Create loan C fresh — pinned to the new snapshot, rating reflects
   `ratingV2`'s collateral-aware logic, still live-recomputing.

**Acceptance:** a short written or recorded walkthrough, one per axis,
showing "old loans keep behaving old, new loans pick up new" holding
independently for UI, reference data, and business logic — via the snapshot
mechanism, not per-field version columns.

---

## Phase 7 — Hardening / scale validation

- Extend the Proposal tab to its full real scale — all six sections, 200+
  fields total, several grids, realistic conditional-rule density — and
  load-test hydration/validation against it.
- Exercise a DEDICATED field referenced in a template with no matching
  column — should fail fast at publish time (Architecture.md Decision 6),
  not at submission time.
- Sketch (doc only) what a read/report-model projection off `USER_ANSWERS`
  would look like (Decision 8), even if not built in this POC.
- **Flag any H2-specific finding rather than presenting it as an Oracle
  prediction** — `JSON_TABLE`/`JSON_VALUE` performance against a CLOB-backed
  `template_json` on pre-21c Oracle should be validated separately (e.g.
  against a scratch Oracle instance) before treating H2's numbers as the
  go/no-go call.

**Acceptance:** a written findings doc validating or revising the
"enterprise-ready" claim with real numbers.

---

## Phase 8 — questionnaire-admin-ui (React) — deliberately last

- Structured JSON-editing UI with schema validation is sufficient for the
  POC — a full drag-and-drop builder (e.g. SurveyJS-based) is an optional
  later upgrade, not required here.
- Draft/publish workflow against questionnaire-service's lifecycle
  endpoints, and against `CONFIG_SNAPSHOT`'s clone-forward publish operation.

**Acceptance:** edit a question's label, publish (clone-forward a snapshot),
confirm application-ui renders the change with zero redeploy.

---

## How to use this with Claude Code

Run each slice/phase as its own session, with Architecture.md and this
document as context, plus `/docs/decisions.md` once it exists from Phase 1.
Phase 1 is a hard gate — don't let a session run past it without your review.
Each subsequent slice should end in something you can actually run and click
through, not just code that compiles.
