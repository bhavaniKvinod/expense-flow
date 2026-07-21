# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ExpenseFlow — an internal expense approval and reimbursement platform. Employees submit expense reports with line items, company policy is enforced automatically at submission time, managers approve/reject, and finance (admin) reimburses. File uploads and real payments are mocked (metadata only, no actual money movement).

Stack: Spring Boot 3.3 (Java 17) + PostgreSQL + Flyway on the backend; React 18 + Vite + react-router + axios on the frontend.

## Commands

### Backend (from `backend/`)
- Start Postgres first: `docker-compose up -d` (from repo root) — runs on port 5432, db/user/pass all `expenseflow`. Note the app's `application.yml` actually points at db `ems`, not `expenseflow` from docker-compose — reconcile before running against a fresh container.
- Run the app: `./mvnw spring-boot:run` (serves on port **8085**)
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=PolicyEngineTest`
- Run a single test method: `./mvnw test -Dtest=PolicyEngineTest#methodName`
- Build jar: `./mvnw clean package`
- Tests run against H2 in-memory (`application-test.yml`, Flyway disabled, `ddl-auto: create-drop`) — no Postgres needed for `mvnw test`.

### Frontend (from `frontend/`)
- Install: `npm install`
- Dev server: `npm run dev` (serves on port 5173, proxies `/api/*` to `http://localhost:8085`, see `vite.config.js`)
- Build: `npm run build`
- Preview production build: `npm run preview`

### Demo login
`DataSeeder` seeds one user per role on first backend startup, all with password `password123`:
- `admin@expenseflow.test` (ADMIN — finance)
- `manager@expenseflow.test` (MANAGER)
- `employee@expenseflow.test` / `dev@expenseflow.test` (EMPLOYEE, both report to the manager above)

## Architecture

### Roles and permissions
Three roles: `EMPLOYEE`, `MANAGER`, `ADMIN`. Each `User` has an optional `manager` (self-referential FK). Permission checks are hand-rolled in service methods (not annotation-based), following consistent patterns:
- **Owner-only**: employees can only view/edit/delete/submit/cancel their own reports (`ExpenseReportService.assertOwner`).
- **Manager-of-employee**: a manager may only decide on reports whose employee's `manager_id` equals their own id (`ApprovalService.assertCanDecide`, `ExpenseReportService.assertCanView`).
- **Admin bypasses scoping**: admin sees/decides all submitted reports and is the only role with reimbursement/user-management/policy-settings access.

Auth is stateless JWT (`JwtAuthFilter` + `JwtService`), configured in `SecurityConfig`. `/api/auth/login` and `/actuator/health` are the only public endpoints; everything else requires a valid bearer token. `CurrentUserProvider`/`AppUserPrincipal` resolve the authenticated `User` entity inside services.

### Expense report lifecycle
States (`ExpenseStatus`): `DRAFT → SUBMITTED → APPROVED → REIMBURSED`, with `REJECTED` (resubmittable back to `SUBMITTED`) and `CANCELLED` (withdrawal by employee) as side branches. Only `DRAFT`/`REJECTED` reports are editable; only `DRAFT` reports are deletable; only `SUBMITTED` reports can be approved/rejected/cancelled; only `APPROVED` reports can be reimbursed. These transition guards live in `ExpenseReportService`, `ApprovalService`, and `ReimbursementService` — when adding a new transition, add the guard in the owning service, not the controller.

Every state transition is recorded via `AuditService` → `AuditEvent` (see `AuditAction` enum), which drives the audit timeline UI (`AuditTimeline.jsx`). Keep this pairing intact when adding new transitions.

### Policy engine (`policy/PolicyEngine.java`)
Enforces company rules at submission time, and is deliberately pure w.r.t. persistence: given a report + `PolicyConfig`, it returns the full list of `PolicyViolation`s (not just the first) so the UI can display everything at once. Reused for two purposes:
1. `ExpenseReportService.submit` — the actual gate; throws `PolicyViolationException` (mapped to an HTTP error with a `violations` array) if any violation exists.
2. `ExpenseReportService.dryRunCheck` — a live, non-mutating "check before you submit" endpoint the frontend polls (`PolicyViolationBanner.jsx`).

Rules: report must have ≥1 line item, receipts required above a configurable USD threshold, per-line-item date must not be future/too-old (rolling window), no duplicate line items (same date+amount+currency+merchant), and total report amount (converted to USD) must not exceed a configurable cap. `PolicyConfig` (thresholds, cap, max age days, USD/INR rate) is a single admin-editable row, managed via `PolicyConfigService`/`PolicyController`.

### Multi-currency
Line items carry a `Currency` (`USD`/`INR`) and amount in that currency. All policy checks and report totals normalize to USD via `CurrencyConverter.toUsd(...)` using the admin-configured `usdToInrRate` on `PolicyConfig`. `ExpenseReport.recomputeTotal(rate)` must be called (and is called by `ExpenseReportService`/`PolicyEngine`) any time line items or the exchange rate change, since `total_amount` is a persisted, not computed, column.

### Layering convention
`Controller → Service → Repository`, with DTOs (`dto/*Dtos.java`, one file per feature grouping request/response records) translated to/from JPA entities via `mapper/Mappers.java`. Controllers hold no business logic — validation of state transitions and permissions lives in services, which throw `BadRequestException` / `ForbiddenException` / `NotFoundException` / `PolicyViolationException`, all handled centrally by `GlobalExceptionHandler`. `open-in-view: true` is enabled so lazy JPA associations can still resolve during DTO mapping in controllers.

### Frontend structure
- `api/client.js` — single axios instance; attaches the JWT bearer token to every request, and on a 401 clears the token and redirects to `/login`.
- `auth/AuthContext.jsx` + `auth/guards.jsx` — `RequireAuth` and `RequireRole` route guards used in `App.jsx` to gate `/approvals` (MANAGER/ADMIN), `/reimbursements`, `/users`, `/policy` (ADMIN only).
- `pages/` map roughly 1:1 to backend controllers/features (e.g. `ApprovalsQueue.jsx` ↔ `ApprovalController`, `ReimbursementQueue.jsx` ↔ `ReimbursementController`).
- Policy violations returned from the backend (`{ violations: [...] }`) are surfaced via `errorMessage()` in `client.js` and rendered by `PolicyViolationBanner.jsx`.

### Database migrations
Flyway migrations live in `backend/src/main/resources/db/migration/` (`V1__schema.sql`, `V2__multi_currency.sql`, ...). `ddl-auto: validate` in prod config — schema changes must go through a new versioned Flyway migration, never by hand-editing entities and letting Hibernate auto-DDL apply them.
