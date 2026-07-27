-- users.manager_id has no index today even though the manager approval queue joins on it
-- on every load (Postgres does not auto-index FK columns).
CREATE INDEX idx_users_manager_id ON users (manager_id);

-- Replace the single-column report indexes with composites matching the actual query shapes:
-- (status, submitted_at) for the approval/reimbursement queues, (employee_id, status) for the
-- dashboard's per-status counts. Both still serve lookups on just their leading column, so the
-- single-column indexes they replace are now redundant.
DROP INDEX idx_reports_employee;
DROP INDEX idx_reports_status;

CREATE INDEX idx_reports_status_submitted_at ON expense_reports (status, submitted_at);
CREATE INDEX idx_reports_employee_status ON expense_reports (employee_id, status);
