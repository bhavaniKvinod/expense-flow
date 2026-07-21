CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    manager_id    BIGINT REFERENCES users (id),
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE policy_config (
    id                   BIGINT PRIMARY KEY,
    receipt_threshold    NUMERIC(12, 2) NOT NULL,
    total_report_cap     NUMERIC(12, 2) NOT NULL,
    max_expense_age_days INT            NOT NULL
);

CREATE TABLE expense_reports (
    id               BIGSERIAL PRIMARY KEY,
    employee_id      BIGINT       NOT NULL REFERENCES users (id),
    title            VARCHAR(255) NOT NULL,
    purpose          VARCHAR(1000),
    status           VARCHAR(20)  NOT NULL,
    total_amount     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    submitted_at     TIMESTAMPTZ,
    decided_at       TIMESTAMPTZ,
    decided_by       BIGINT REFERENCES users (id),
    rejection_reason VARCHAR(1000),
    created_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_reports_employee ON expense_reports (employee_id);
CREATE INDEX idx_reports_status ON expense_reports (status);

CREATE TABLE expense_line_items (
    id           BIGSERIAL PRIMARY KEY,
    report_id    BIGINT         NOT NULL REFERENCES expense_reports (id) ON DELETE CASCADE,
    expense_date DATE           NOT NULL,
    category     VARCHAR(20)    NOT NULL,
    amount       NUMERIC(12, 2) NOT NULL,
    merchant     VARCHAR(255)   NOT NULL,
    notes        VARCHAR(500)
);

CREATE INDEX idx_line_items_report ON expense_line_items (report_id);

CREATE TABLE receipts (
    id           BIGSERIAL PRIMARY KEY,
    line_item_id BIGINT       NOT NULL UNIQUE REFERENCES expense_line_items (id) ON DELETE CASCADE,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    size_bytes   BIGINT,
    uploaded_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE audit_events (
    id         BIGSERIAL PRIMARY KEY,
    report_id  BIGINT       NOT NULL REFERENCES expense_reports (id) ON DELETE CASCADE,
    actor_id   BIGINT       NOT NULL REFERENCES users (id),
    action     VARCHAR(20)  NOT NULL,
    note       VARCHAR(1000),
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_report ON audit_events (report_id);

CREATE TABLE reimbursements (
    id                BIGSERIAL PRIMARY KEY,
    report_id         BIGINT       NOT NULL UNIQUE REFERENCES expense_reports (id),
    payment_reference VARCHAR(255) NOT NULL,
    paid_by           BIGINT       NOT NULL REFERENCES users (id),
    paid_at           TIMESTAMPTZ  NOT NULL
);
