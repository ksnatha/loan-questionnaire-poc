# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enterprise-grade **Dynamic Questionnaire Engine** for loan applications. Questions, labels, and business logic must be versionable without redeployment. The system uses three independent versioning axes: UI/template versioning, reference data versioning (code sets), and business rule/rating logic versioning.

Full design rationale is in `docs/questionnaire-poc-architecture.md`. Phase-by-phase build instructions are in `docs/questionnaire-poc-ai-agent-instructions.md`.

## Architecture: Five Components

| Component | Role |
|---|---|
| `questionnaire-service` | Authors, versions, and publishes section/tab templates |
| `application-service` | Consumes templates, manages loan applications, validates answers, computes risk ratings |
| `application-ui` | React app — generically renders tab sections from template JSON |
| `questionnaire-admin-ui` | React app — business users edit and publish templates (lowest priority) |
| `lookup-stub-service` | Reference-data facade for code sets and external lookups |

**Build order**: `lookup-stub-service` → `questionnaire-service` → `application-service` → `application-ui` → `questionnaire-admin-ui`

**Walking skeleton strategy**: Each build slice goes thin through all components simultaneously (not component-by-component), to surface REST contract mismatches early.

## Build Phases

- **Phase 0**: Multi-module Gradle workspace, H2 databases, empty controllers, separate ports — all modules compile and start cleanly
- **Phase 1 (hard gate)**: Finalize JSON schemas, REST contracts, and `/docs/decisions.md` before any slice begins
- **Slice A–F**: Incrementally add DEDICATED fields, EAV/dropdowns, visibility rules, grids, borrower data, then versioning demonstration
- **Phase 7**: Hardening to 200+ fields
- **Phase 8**: `questionnaire-admin-ui`

## Key Architectural Decisions

**Hybrid storage (Decision 1)**: `section_template` stores full JSON in a `template_json` CLOB plus materialized columns (`has_grid`, `dedicated_column_refs`) for queryability on pre-21c Oracle — no JSON functions in queries.

**Config snapshots (Decision 2)**: A `CONFIG_SNAPSHOT` bundles all versioned concerns atomically. Published via "clone-forward" (copy ACTIVE → override changed items → activate). In-flight loan applications stay pinned to their original snapshot; they don't silently upgrade mid-flight.

**Versioned business logic (Decision 3)**: Risk-rating logic dispatches via Spring bean-name lookup from a map keyed by version string — never `if`/`switch` on version numbers.

**Dropdown sources (Decision 4)**: Three types — `STATIC` (baked in template JSON), `CODE_SET` (reference data with effective dating from `lookup-stub-service`), `EXTERNAL` (stub service call).

**Security (Decision 6)**: `DEDICATED` field table/column names are validated at template publish time and never interpolated into SQL queries at runtime.

**Visibility/validation rules (Decision 7)**: Rule scope is explicit (`SECTION`, `ROW`, `TAB`) and shared — no grid-specific branching.

**Write vs. read model (Decision 8)**: `USER_ANSWERS` (EAV with `row_index` for grids) is the write model; reporting queries use a separate read model.

**i18n (Decision 9)**: All labels use translation keys from day one via `LABEL_TRANSLATION` table.

## Domain Model Highlights

- `LOAN_APPLICATION` pins to exactly one `CONFIG_SNAPSHOT`
- `USER_ANSWERS` EAV table: `(application_id, field_key, row_index, value)`; `row_index` enables grid rows
- `LOAN_PARTY` is a single table with a role discriminator (borrower / co-borrower)
- Submission workflow: `DRAFT` → `SUBMIT`; risk rating recomputed on every save-draft

## Technology Stack

- **Backend**: Java, Spring Boot, JPA/Hibernate
- **Databases**: H2 (POC/dev), pre-21c Oracle (production target)
- **Frontend**: React (two apps)
- **Build**: Gradle multi-module workspace
