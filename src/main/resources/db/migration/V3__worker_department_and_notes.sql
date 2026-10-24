-- =============================================================================
-- V3: Add Department Link to Users (Workers) and Worker Notes to Complaints
-- =============================================================================

ALTER TABLE users ADD COLUMN department_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL;

ALTER TABLE complaints ADD COLUMN worker_notes TEXT;
