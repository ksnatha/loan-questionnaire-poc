# Policy Decisions

Companion to `questionnaire-poc-architecture.md`. Records Architecture.md's
pre-decided policies plus any additional policy calls made during Phase 1.

---

## 1 — Dropdown resolution policy (Architecture.md Decision 4 — pre-decided)

**Policy (recorded from Architecture.md, not decided here):** Resolve stored
answers as of the record's original creation date, including for in-progress/DRAFT
records. `CODE_SET` lookups use `asOf=application.createdDate` on every render.
STATIC options are version-stable inside `template_json`.

**Detail:**
- `CODE_SET` dropdowns: call `GET /code-sets/{type}?asOf={application.createdDate}`
  on every render. For a brand-new application this is today; for a reopened draft
  it is the application's original creation date.
- `EXTERNAL` dropdowns: call `GET /lookups/{sourceKey}` on every render (no date
  param — external stubs are not effective-dated in this POC).
- `STATIC` options: embedded in `template_json`; they version with the section
  template and do not change unless a new section version is published.

**Why `createdDate` and not `today`:**
CODE_SET uses `start_date`/`end_date` effective dating. When a code is retired the
old row is closed (`end_date` set), not deleted. If a draft was created when `REFI`
was active, and `REFI` is later retired, using `asOf=today` would fail to resolve
the stored answer code `REFI` — the old record is broken. Using
`asOf=application.createdDate` means the stored code always resolves against the
code set as it existed when the application was started, which is correct.

**Implication:** reopening an old draft shows the dropdown options that were valid
at creation time, not today's options. New code set entries added after creation do
not appear in an old draft's dropdown. This is intentional — the available choices
are stable for the life of a draft.

**Implication for Slice B:** application-service reads `loan_application.created_date`
and passes it as `asOf` on every CODE_SET hydration call. The date is stored on
`LOAN_APPLICATION`, not derived at runtime.

---

## 2 — Mid-flight snapshot change behaviour (already resolved in Architecture.md)

Restated here for completeness:

**Decision:** A DRAFT loan application stays pinned to the `CONFIG_SNAPSHOT` it was
created under until the application is resubmitted. It is never silently migrated
to a newer snapshot.

**Why:** In-flight answers may reference field keys, code-set codes, or rating logic
that exists in the pinned snapshot but not in the new one. Silent migration could
corrupt or invalidate in-progress work without the user's knowledge.

**Resubmission flow (Slice F):** When a user resubmits after a snapshot change,
application-service re-pins to the current ACTIVE snapshot and runs the new
template's validation rules against the saved answers. Any answers that fail the
new rules are flagged, not silently dropped.

---

## 3 — DEDICATED field allowlist validation (Architecture.md Decision 6)

**Decision:** At publish time, questionnaire-service validates every
`dedicated_table_name`/`dedicated_column_name` pair in the section template JSON
against a hardcoded allowlist of (table, column) pairs derived from
application-service's real JPA entity model. Publication is rejected if any pair
is not on the allowlist.

**Allowlist (Slice A scope):**
- `loan_application` / `proposal_name`
- `loan_application` / `loan_amount`
- `loan_application` / `proposal_description`

Additional pairs added to the allowlist as new DEDICATED fields are introduced in
later slices.

**Why:** `String.format("SELECT %s FROM %s ...")` with unvalidated metadata is a
SQL-injection surface the moment the admin UI exists. Validation at publish time —
not at query time — means bad metadata never reaches the database layer.

**Additional publish-time check:** A section template is also rejected if any two
`FieldDefinition` objects in the same `fields` array share the same `fieldKey`, or
if any two `columns` in the same `GridDefinition` share the same `fieldKey`. Duplicate
keys within a single section are a data-integrity bug — the EAV write model is keyed
by `(application_id, field_key, row_index)`, so two fields with the same key would
silently overwrite each other on every save.

---

## 4 — human_readable_id format

**Decision:** `L{year}-{sequence}`, no padding, yearly reset.
Example: `L2026-1`, `L2026-42`, `L2027-1`.

Counter lives in `application_number_counter(year, last_value)`, incremented in
the same transaction as the `LOAN_APPLICATION` insert via a SELECT FOR UPDATE
(or equivalent) on the counter row. No gaps are guaranteed; no uniqueness beyond
the DB unique constraint on `human_readable_id`.

---

## 5 — CONFIG_SNAPSHOT publish (clone-forward) contract

**Decision:** Publishing a new snapshot is always a clone-forward operation:

1. Read every `CONFIG_SNAPSHOT_ITEM` row from the current `ACTIVE` snapshot.
2. Insert a new `CONFIG_SNAPSHOT` row in `DRAFT` status.
3. Copy all items into the new snapshot, then override only the items explicitly
   specified in the publish request.
4. Atomically set the old `ACTIVE` snapshot → `RETIRED` (set `effective_end` =
   today), and the new snapshot → `ACTIVE` (set `effective_start` = today).

There is exactly one `ACTIVE` snapshot at all times. A `DRAFT` snapshot may coexist
while being prepared, but it cannot be used to pin new loans until activated.

**Caller:** Only application-service exposes the `POST /snapshots/publish` endpoint.
questionnaire-service's publish endpoints (for section/tab templates) do NOT
automatically cut a new config snapshot — that is a separate, deliberate admin step.
