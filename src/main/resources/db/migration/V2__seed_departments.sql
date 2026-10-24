-- =============================================================================
-- V2: Seed Default Civic Departments
-- =============================================================================

INSERT INTO departments (name) VALUES
    ('Sanitation'),
    ('Public Works'),
    ('Electrical'),
    ('General Administration')
ON CONFLICT (name) DO NOTHING;
