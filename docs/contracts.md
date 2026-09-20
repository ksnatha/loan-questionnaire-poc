# REST Contracts

All services use JSON over HTTP. Base URLs are local dev ports.
Cross-service references (e.g. application-service calling questionnaire-service)
are logical REST calls — no shared database, no DB-level foreign keys.

---

## lookup-stub-service — port 8081

### GET /code-sets/{type}?asOf={date}
Returns all active code-set options for a given type on the specified date.
`asOf` defaults to today if omitted.

**Response 200:**
```json
[
  { "code": "REFI",  "value": "Refinance",  "type": "LOAN_PURPOSE" },
  { "code": "EQUIP", "value": "Equipment purchase", "type": "LOAN_PURPOSE" }
]
```

### GET /code-sets/{type}/{code}?asOf={date}
Returns a single code-set option. `404` if the code is not active on that date.

**Response 200:**
```json
{ "code": "REFI", "value": "Refinance", "type": "LOAN_PURPOSE" }
```

### GET /lookups/{sourceKey}
Returns all options for an external lookup source key.

**Response 200:**
```json
[
  { "optionKey": "EXT-001", "label": "External Option A" }
]
```

---

## questionnaire-service — port 8082

### Section template lifecycle

#### POST /sections
Create a new DRAFT section template (version 1).

**Request:**
```json
{
  "sectionId": "key-information",
  "labelKey": "section.key_information",
  "template": { /* SectionTemplateJson — see schema below */ }
}
```

**Response 201:** `{ "sectionId": "key-information", "version": 1, "status": "DRAFT" }`

#### GET /sections/{sectionId}/versions/{version}
Returns the full section template including `template_json`.

**Response 200:**
```json
{
  "sectionId": "key-information",
  "version": 1,
  "status": "ACTIVE",
  "labelKey": "section.key_information",
  "hasGrid": false,
  "dedicatedColumnRefs": ["loan_application.proposal_name", "loan_application.loan_amount"],
  "template": { /* SectionTemplateJson */ }
}
```

#### POST /sections/{sectionId}/versions/{version}/publish
Publish a DRAFT section template to ACTIVE. Runs the DEDICATED-field allowlist
check (decisions.md §3). Rejects with `400` if any field fails validation.

**Response 200:** `{ "sectionId": "...", "version": 1, "status": "ACTIVE" }`

### Tab template lifecycle

#### POST /tabs
Create a new DRAFT tab template.

**Request:**
```json
{
  "tabId": "proposal",
  "labelKey": "tab.proposal",
  "sections": [
    { "sectionId": "key-information", "displayOrder": 1 }
  ]
}
```

**Response 201:** `{ "tabId": "proposal", "version": 1, "status": "DRAFT" }`

#### GET /tabs/{tabId}/versions/{version}
Returns the full tab template.

#### POST /tabs/{tabId}/versions/{version}/publish
Publish a DRAFT tab template to ACTIVE.

---

## application-service — port 8083

### Loan application lifecycle

#### POST /applications
Create a new DRAFT loan application. Pins to current ACTIVE CONFIG_SNAPSHOT.
Generates `human_readable_id` via the yearly counter.

**Request:**
```json
{ "createdUser": "jsmith" }
```

**Response 201:**
```json
{
  "id": 1,
  "humanReadableId": "L2026-1",
  "status": "DRAFT",
  "configSnapshotId": 1,
  "riskRating": null
}
```

#### GET /applications/{id}/render
Returns the application header plus the fully hydrated tab template for rendering.
Resolves CODE_SET dropdown options from lookup-stub-service using `asOf=application.createdDate`
(not today — see decisions.md §1).
Returns current USER_ANSWERS keyed by fieldKey (and rowIndex for grid fields).

**Response 200:**
```json
{
  "application": { "id": 1, "humanReadableId": "L2026-1", "status": "DRAFT", "riskRating": null },
  "tab": {
    "tabId": "proposal",
    "sections": [
      {
        "sectionId": "key-information",
        "labelKey": "section.key_information",
        "fields": [ /* hydrated FieldDefinition list with options populated */ ],
        "grid": null
      }
    ]
  },
  "answers": {
    "proposal_name": "Acme Corp expansion facility",
    "loan_amount": "2450000"
  }
}
```

#### PUT /applications/{id}/draft
Save answers for a draft application. Recomputes and returns `riskRating`.
Hidden fields (per visibility rules evaluated server-side) have their answers purged.

**Request:**
```json
{
  "answers": {
    "proposal_name": "Acme Corp expansion facility",
    "loan_amount": "2450000",
    "proposal_description": "Term facility..."
  },
  "gridAnswers": {
    "collateral_properties": [
      { "rowIndex": 0, "answers": { "property_address": "123 Main St", "property_type": "COMMERCIAL" } }
    ]
  }
}
```

**Response 200:** `{ "riskRating": "MEDIUM", "riskRatingComputedDate": "2026-09-20T10:00:00Z" }`

#### POST /applications/{id}/submit
Final submit. Runs full server-side validation against the pinned snapshot's template.
Computes final risk rating. Transitions status to `SUBMITTED`.

**Response 200:** `{ "status": "SUBMITTED", "riskRating": "MEDIUM" }`

**Response 422:** validation errors if required fields are missing or visibility rules fail.
```json
{
  "status": 422,
  "errors": [
    {
      "fieldKey": "rate_cap_percentage",
      "ruleId": "rate-cap-required-when-variable",
      "validationType": "REQUIRED",
      "message": "validation.required"
    }
  ]
}
```

> **LoanParty (borrower/co-borrower) endpoints** are intentionally deferred to Slice E.
> They are not missing by oversight — they are out of scope for Slices A–D.

### Snapshot management

#### GET /snapshots/active
Returns the current active CONFIG_SNAPSHOT and all its items.

**Response 200:**
```json
{
  "id": 1,
  "snapshotCode": "SNAPSHOT-2026-V1",
  "status": "ACTIVE",
  "effectiveStart": "2026-09-20",
  "items": [
    { "versionType": "TAB_TEMPLATE",                   "versionValue": 1 },
    { "versionType": "SECTION_KEY_INFORMATION",        "versionValue": 1 },
    { "versionType": "SECTION_ADDITIONAL_INFORMATION", "versionValue": 1 },
    { "versionType": "SECTION_ASSOCIATED_RECORDS",     "versionValue": 1 },
    { "versionType": "RATING_LOGIC",                   "versionValue": 1, "strategyBeanName": "ratingV1" }
  ]
}
```

#### POST /snapshots/publish
Clone-forward a new ACTIVE snapshot with the specified overrides (see decisions.md §5).

**Request:**
```json
{
  "snapshotCode": "SNAPSHOT-2026-V2",
  "overrides": [
    { "versionType": "SECTION_ADDITIONAL_INFORMATION", "versionValue": 2 }
  ]
}
```

**Response 201:** the newly activated snapshot (same shape as GET /snapshots/active response).

---

## Section template JSON schema

This is the structure stored in `SECTION_TEMPLATE.template_json` (CLOB) and returned
inside the `/render` response.

`dropdownSource` is always present on every field; `null` for non-dropdown fields.
STATIC option lists live inside `dropdownSource.options` — there is no top-level
`staticOptions` key. This keeps the hydration client genuinely branch-free on source
type (Architecture.md Decision 4).

```json
{
  "fields": [
    {
      "fieldKey": "proposal_name",
      "fieldType": "TEXT",
      "labelKey": "field.proposal_name",
      "storage": { "type": "DEDICATED", "tableName": "loan_application", "columnName": "proposal_name" },
      "dropdownSource": null,
      "required": true,
      "visibilityRules": [],
      "validationRules": [
        {
          "ruleId": "proposal-name-min",
          "scope": "SECTION",
          "condition": null,
          "targetFieldKey": "proposal_name",
          "validationType": "MIN_LENGTH",
          "minLength": 3
        },
        {
          "ruleId": "proposal-name-max",
          "scope": "SECTION",
          "condition": null,
          "targetFieldKey": "proposal_name",
          "validationType": "MAX_LENGTH",
          "maxLength": 200
        }
      ]
    },
    {
      "fieldKey": "loan_purpose",
      "fieldType": "DROPDOWN",
      "labelKey": "field.loan_purpose",
      "storage": { "type": "EAV" },
      "dropdownSource": { "type": "CODE_SET", "codeSetType": "LOAN_PURPOSE" },
      "required": true,
      "visibilityRules": [],
      "validationRules": []
    },
    {
      "fieldKey": "loan_category",
      "fieldType": "DROPDOWN",
      "labelKey": "field.loan_category",
      "storage": { "type": "EAV" },
      "dropdownSource": {
        "type": "STATIC",
        "options": [
          { "code": "COMMERCIAL", "labelKey": "loan_category.commercial" },
          { "code": "RESIDENTIAL", "labelKey": "loan_category.residential" }
        ]
      },
      "required": true,
      "visibilityRules": [],
      "validationRules": []
    },
    {
      "fieldKey": "existing_relationship_id",
      "fieldType": "TEXT",
      "labelKey": "field.existing_relationship_id",
      "storage": { "type": "EAV" },
      "dropdownSource": null,
      "required": false,
      "visibilityRules": [
        {
          "ruleId": "show-rel-id",
          "scope": "SECTION",
          "condition": {
            "fieldKey": "has_existing_relationship",
            "operator": "EQUALS",
            "value": "YES"
          },
          "targetFieldKey": "existing_relationship_id",
          "show": true
        }
      ],
      "validationRules": []
    },
    {
      "fieldKey": "rate_cap_percentage",
      "fieldType": "NUMBER",
      "labelKey": "field.rate_cap_percentage",
      "storage": { "type": "EAV" },
      "dropdownSource": null,
      "required": false,
      "visibilityRules": [],
      "validationRules": [
        {
          "ruleId": "rate-cap-required-when-variable",
          "scope": "SECTION",
          "condition": {
            "fieldKey": "interest_rate_type",
            "operator": "EQUALS",
            "value": "VARIABLE"
          },
          "targetFieldKey": "rate_cap_percentage",
          "validationType": "REQUIRED"
        },
        {
          "ruleId": "rate-cap-max",
          "scope": "SECTION",
          "condition": null,
          "targetFieldKey": "rate_cap_percentage",
          "validationType": "MAX_VALUE",
          "maxValue": 100.0
        }
      ]
    }
  ],
  "grids": [
    {
      "gridKey": "collateral_properties",
      "labelKey": "grid.collateral_properties",
      "columns": [
        {
          "fieldKey": "property_address",
          "fieldType": "TEXT",
          "labelKey": "grid.property_address",
          "storage": { "type": "EAV" },
          "dropdownSource": null,
          "required": true,
          "visibilityRules": [],
          "validationRules": []
        },
        {
          "fieldKey": "property_type",
          "fieldType": "DROPDOWN",
          "labelKey": "grid.property_type",
          "storage": { "type": "EAV" },
          "dropdownSource": { "type": "CODE_SET", "codeSetType": "PROPERTY_TYPE" },
          "required": true,
          "visibilityRules": [],
          "validationRules": []
        },
        {
          "fieldKey": "lien_position",
          "fieldType": "DROPDOWN",
          "labelKey": "grid.lien_position",
          "storage": { "type": "EAV" },
          "dropdownSource": { "type": "CODE_SET", "codeSetType": "LIEN_POSITION" },
          "required": false,
          "visibilityRules": [],
          "validationRules": [
            {
              "ruleId": "lien-position-required-commercial",
              "scope": "ROW",
              "condition": {
                "fieldKey": "property_type",
                "operator": "EQUALS",
                "value": "COMMERCIAL"
              },
              "targetFieldKey": "lien_position",
              "validationType": "REQUIRED"
            }
          ]
        }
      ]
    }
  ]
}
```

### Rule scope — how `condition.fieldKey` resolves

| scope   | `condition.fieldKey` resolves against …                                    |
|---------|----------------------------------------------------------------------------|
| SECTION | the flat field list of the same section (whole-section answer map)         |
| ROW     | other columns in the same grid row at the same `rowIndex`                  |
| TAB     | all fields across every section in the tab (cross-section answer map)      |

The same evaluator class handles all three scopes. The only difference is the
answer map it receives — section answers, a single row's answers, or the full
tab's answers. No per-scope branching inside the evaluator.

## Tab template JSON schema

`TAB_TEMPLATE_SECTION` stores composition only — `section_id` and `display_order`,
no section version. Each section's pinned version lives in its own
`CONFIG_SNAPSHOT_ITEM` row (`version_type = 'SECTION_<SECTION_ID>'`), resolved at
render time from the loan's pinned snapshot. The JSON below is the wire format
returned to the UI inside the `/render` response.

```json
{
  "tabId": "proposal",
  "labelKey": "tab.proposal",
  "sections": [
    { "sectionId": "key-information",    "displayOrder": 1 },
    { "sectionId": "additional-info",    "displayOrder": 2 },
    { "sectionId": "associated-records", "displayOrder": 3 }
  ]
}
```

## CONFIG_SNAPSHOT schema

See ERD in `questionnaire-poc-architecture.md`. The key invariant: exactly one
`ACTIVE` snapshot at all times. New loan applications pin to it at creation.
Clone-forward publish contract: `decisions.md §5`.

---

## Canonical enum values

Authoritative source of truth. Both `questionnaire-service` and `application-service`
derive their Java enums from this list. Any value not listed here is invalid.

| Enum               | Values                                                          |
|--------------------|-----------------------------------------------------------------|
| `FieldType`        | `TEXT`, `NUMBER`, `TEXTAREA`, `DROPDOWN`, `RADIO`               |
| `DropdownSourceType` | `STATIC`, `CODE_SET`, `EXTERNAL`                              |
| `ValidationType`   | `REQUIRED`, `MIN_LENGTH`, `MAX_LENGTH`, `MIN_VALUE`, `MAX_VALUE` |
| `RuleOperator`     | `EQUALS`, `NOT_EQUALS`, `IN`, `NOT_IN`, `IS_EMPTY`, `IS_NOT_EMPTY` |
| `RuleScope`        | `SECTION`, `ROW`, `TAB`                                         |
| `StorageType`      | `DEDICATED`, `EAV`                                              |
