# Sample data — understanding versioning end to end

Companion to Architecture.md. These are illustrative rows, not seed data to
load verbatim — the point is to make the versioning mechanism concrete by
walking through one realistic timeline.

**The scenario:** three publishes happen over time. First the tab and its
sections are set up. Then Additional Information gets a new question
(template change only). Then the rating logic is upgraded to factor in
collateral (business logic change only). Three loans are created at three
different points, each pinned to whatever was `ACTIVE` at that moment.

---

## TAB_TEMPLATE

Only **composition** lives here — no section version. It never changes in
this scenario, because no section is added, removed, or reordered:

| id | tab_id | version | status |
|---|---|---|---|
| 1 | PROPOSAL | 1 | ACTIVE |

## TAB_TEMPLATE_SECTION (children of TAB_TEMPLATE id=1)

| id | tab_template_id | section_id | display_order |
|---|---|---|---|
| 1 | 1 | KEY_INFORMATION | 1 |
| 2 | 1 | ADDITIONAL_INFORMATION | 2 |
| 3 | 1 | LEGAL_VEHICLES | 3 |
| 4 | 1 | ELIGIBILITY_CHECK | 4 |
| 5 | 1 | ASSOCIATED_RECORDS | 5 |
| 6 | 1 | APPROVER_ATTESTATION | 6 |

Note the absence of a version column — this is the corrected design. If this
table pinned a section version directly, a section content change would
force a new `TAB_TEMPLATE` row, which is exactly what Decision 5 says
shouldn't happen.

*(For context, not a requested table: `SECTION_TEMPLATE` has two published
rows for `ADDITIONAL_INFORMATION` in this scenario — version 1, and version
2 after a question gets added. `KEY_INFORMATION` and `ASSOCIATED_RECORDS`
stay at version 1 throughout.)*

---

## CONFIG_SNAPSHOT

| id | snapshot_code | status | effective_start | effective_end |
|---|---|---|---|---|
| 1 | V1 | RETIRED | 2026-01-05 | 2026-02-14 |
| 2 | V2 | RETIRED | 2026-02-15 | 2026-03-09 |
| 3 | V3 | ACTIVE | 2026-03-10 | — |

- **V1 → V2**: Additional Information gets a new question. Template change
  only.
- **V2 → V3**: rating logic upgraded to factor in the collateral grid.
  Business logic change only.

## CONFIG_SNAPSHOT_ITEM (children)

**V1** — the initial published state:

| id | snapshot_id | version_type | version_value | strategy_bean_name |
|---|---|---|---|---|
| 1 | 1 | TAB_TEMPLATE | 1 | — |
| 2 | 1 | SECTION_KEY_INFORMATION | 1 | — |
| 3 | 1 | SECTION_ADDITIONAL_INFORMATION | 1 | — |
| 4 | 1 | SECTION_ASSOCIATED_RECORDS | 1 | — |
| 5 | 1 | RATING_LOGIC | 1 | ratingV1 |

**V2** — clone-forward from V1, only the Additional Information item changes:

| id | snapshot_id | version_type | version_value | strategy_bean_name |
|---|---|---|---|---|
| 6 | 2 | TAB_TEMPLATE | 1 | — |
| 7 | 2 | SECTION_KEY_INFORMATION | 1 | — |
| 8 | 2 | **SECTION_ADDITIONAL_INFORMATION** | **2** | — |
| 9 | 2 | SECTION_ASSOCIATED_RECORDS | 1 | — |
| 10 | 2 | RATING_LOGIC | 1 | ratingV1 |

**V3** — clone-forward from V2, only the rating item changes:

| id | snapshot_id | version_type | version_value | strategy_bean_name |
|---|---|---|---|---|
| 11 | 3 | TAB_TEMPLATE | 1 | — |
| 12 | 3 | SECTION_KEY_INFORMATION | 1 | — |
| 13 | 3 | SECTION_ADDITIONAL_INFORMATION | 2 | — |
| 14 | 3 | SECTION_ASSOCIATED_RECORDS | 1 | — |
| 15 | 3 | **RATING_LOGIC** | **2** | **ratingV2** |

This is the clone-forward mechanism made concrete: each publish touches
exactly one row, everything else carries forward byte-for-byte.

---

## LOAN_APPLICATION

One loan created under each snapshot:

| id | human_readable_id | proposal_name | status | config_snapshot_id | risk_rating |
|---|---|---|---|---|---|
| 1 | L2026-1 | Acme Corp expansion facility | DRAFT | 1 (V1) | MEDIUM |
| 2 | L2026-2 | Bell Manufacturing term loan | DRAFT | 2 (V2) | LOW |
| 3 | L2026-3 | Delta Logistics equipment loan | SUBMITTED | 3 (V3) | HIGH |

**What each loan actually sees, today, with V3 now `ACTIVE`:**

- **L2026-1** (pinned to V1): renders Additional Information **version 1**
  (no new question) and computes its rating via **ratingV1** (no collateral
  factor) — even though V3 is current. Reopening this DRAFT does not pick up
  either later change.
- **L2026-2** (pinned to V2): renders Additional Information **version 2**
  (has the new question) but still computes via **ratingV1** — it existed
  before the rating logic changed, so it never picked that part up, even
  though it did get the newer template.
- **L2026-3** (pinned to V3): renders Additional Information **version 2**
  and computes via **ratingV2** — created after both changes, gets both.

This is the concrete proof that the two versioned concerns are genuinely
independent: L2026-2 shows the new template with the old rating logic, a
combination that couldn't exist if `TAB_TEMPLATE`/rating were coupled
together.

---

## CODE_SET

The `LOAN_PURPOSE` code `REFI` gets relabeled between L2026-1's creation and
today — via a new row, never an in-place edit:

| id | type | code | value | start_date | end_date | active_ind |
|---|---|---|---|---|---|---|
| 1 | LOAN_PURPOSE | REFI | Refinance existing debt | 2025-01-01 | 2026-02-14 | N |
| 2 | LOAN_PURPOSE | REFI | Debt refinancing | 2026-02-15 | — | Y |
| 3 | LOAN_PURPOSE | WORKING_CAPITAL | Working capital | 2025-01-01 | — | Y |
| 4 | LOAN_PURPOSE | EQUIPMENT | Equipment purchase | 2025-01-01 | — | Y |

L2026-1 was created 2026-01-05 and selected `REFI`. Re-rendering it today —
per the decided default policy (Architecture.md Decision 4): resolve as of
the record's original creation date, even while still in DRAFT — resolves
to **"Refinance existing debt"**, row 1, active on its creation date, not
**"Debt refinancing"**, the current label. A brand-new loan selecting `REFI`
today gets row 2. This holds regardless of how long L2026-1 sits in DRAFT.

---

## The pattern across all three

Every versioned concern in this system follows the same shape: **never
overwrite, always add a new row with a clear boundary, and let the
reference — a pinned snapshot, or a resolved-as-of date — decide which row
applies.** `CONFIG_SNAPSHOT`/`CONFIG_SNAPSHOT_ITEM` do this with discrete,
named versions; `CODE_SET` does it with continuous date ranges. Different
mechanisms for different jobs (Architecture.md Decision 4), same underlying
principle.
