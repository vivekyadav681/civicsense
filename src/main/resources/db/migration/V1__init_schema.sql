-- =============================================================================
-- V1: Initial Database Schema for CivicSense Platform
-- =============================================================================

-- 1. Users table
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- 2. Departments table
CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    CONSTRAINT uk_departments_name UNIQUE (name)
);

-- 3. Complaints table (includes self-referencing FK for duplicate grouping)
CREATE TABLE complaints (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(200) NOT NULL,
    description         TEXT NOT NULL,
    category            VARCHAR(30) NOT NULL,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
    before_image_url    VARCHAR(2048),
    after_image_url     VARCHAR(2048),
    department_id       BIGINT,
    reporter_id         BIGINT NOT NULL,
    parent_complaint_id BIGINT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_complaint_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    CONSTRAINT fk_complaint_reporter   FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_complaint_parent     FOREIGN KEY (parent_complaint_id) REFERENCES complaints(id) ON DELETE SET NULL
);

-- 4. Strategic Performance Indexes
CREATE INDEX idx_complaints_status    ON complaints (status);
CREATE INDEX idx_complaints_category  ON complaints (category);
CREATE INDEX idx_complaints_reporter  ON complaints (reporter_id);
CREATE INDEX idx_complaints_coords    ON complaints (latitude, longitude);
CREATE INDEX idx_complaints_parent    ON complaints (parent_complaint_id);
