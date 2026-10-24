-- =============================================================================
-- V4: Seed Mock Users (Admin, Citizens, Workers) and Sample Delhi/Gurgaon Complaints
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. SEED MOCK USERS
-- Default Password for all mock users: Password123!
-- BCrypt Hash: $2a$12$j53am3Kn7g2C8m3X89WLK.MlnmekBMUo/dk.tklyOX.wkOeFZ6Qce
-- -----------------------------------------------------------------------------

-- Admin User
INSERT INTO users (name, email, password, role, department_id, created_at)
VALUES (
    'Admin User',
    'admin@civicsense.gov',
    '$2a$12$j53am3Kn7g2C8m3X89WLK.MlnmekBMUo/dk.tklyOX.wkOeFZ6Qce',
    'ADMIN',
    (SELECT id FROM departments WHERE name = 'General Administration' LIMIT 1),
    NOW() - INTERVAL '30 days'
)
ON CONFLICT (email) DO UPDATE
SET role = 'ADMIN', department_id = (SELECT id FROM departments WHERE name = 'General Administration' LIMIT 1);

-- Citizen 1: Aarav Sharma (Delhi)
INSERT INTO users (name, email, password, role, department_id, created_at)
VALUES (
    'Aarav Sharma',
    'aarav.sharma@example.com',
    '$2a$12$j53am3Kn7g2C8m3X89WLK.MlnmekBMUo/dk.tklyOX.wkOeFZ6Qce',
    'CITIZEN',
    NULL,
    NOW() - INTERVAL '25 days'
)
ON CONFLICT (email) DO NOTHING;

-- Citizen 2: Pooja Verma (Gurgaon)
INSERT INTO users (name, email, password, role, department_id, created_at)
VALUES (
    'Pooja Verma',
    'pooja.verma@example.com',
    '$2a$12$j53am3Kn7g2C8m3X89WLK.MlnmekBMUo/dk.tklyOX.wkOeFZ6Qce',
    'CITIZEN',
    NULL,
    NOW() - INTERVAL '20 days'
)
ON CONFLICT (email) DO NOTHING;

-- Worker 1: Public Works / Roads
INSERT INTO users (name, email, password, role, department_id, created_at)
VALUES (
    'Rajesh Kumar',
    'worker.roads@civicsense.gov',
    '$2a$12$j53am3Kn7g2C8m3X89WLK.MlnmekBMUo/dk.tklyOX.wkOeFZ6Qce',
    'WORKER',
    (SELECT id FROM departments WHERE name = 'Public Works' LIMIT 1),
    NOW() - INTERVAL '28 days'
)
ON CONFLICT (email) DO UPDATE
SET department_id = (SELECT id FROM departments WHERE name = 'Public Works' LIMIT 1);

-- Worker 2: Sanitation / Waste
INSERT INTO users (name, email, password, role, department_id, created_at)
VALUES (
    'Suresh Yadav',
    'worker.waste@civicsense.gov',
    '$2a$12$j53am3Kn7g2C8m3X89WLK.MlnmekBMUo/dk.tklyOX.wkOeFZ6Qce',
    'WORKER',
    (SELECT id FROM departments WHERE name = 'Sanitation' LIMIT 1),
    NOW() - INTERVAL '28 days'
)
ON CONFLICT (email) DO UPDATE
SET department_id = (SELECT id FROM departments WHERE name = 'Sanitation' LIMIT 1);

-- Worker 3: Electrical / Streetlights
INSERT INTO users (name, email, password, role, department_id, created_at)
VALUES (
    'Amit Singh',
    'worker.electric@civicsense.gov',
    '$2a$12$j53am3Kn7g2C8m3X89WLK.MlnmekBMUo/dk.tklyOX.wkOeFZ6Qce',
    'WORKER',
    (SELECT id FROM departments WHERE name = 'Electrical' LIMIT 1),
    NOW() - INTERVAL '28 days'
)
ON CONFLICT (email) DO UPDATE
SET department_id = (SELECT id FROM departments WHERE name = 'Electrical' LIMIT 1);


-- -----------------------------------------------------------------------------
-- 2. SEED REALISTIC COMPLAINTS ACROSS DELHI & GURGAON
-- -----------------------------------------------------------------------------

-- 1. Connaught Place, New Delhi (ROADS, IN_PROGRESS)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Deep crater on Inner Circle near Block B',
    'Large asphalt crater on Inner Circle road near Connaught Place Block B causing vehicle slowdowns and safety hazards during evening peak hours.',
    'ROADS',
    28.6315,
    77.2167,
    'IN_PROGRESS',
    'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=800&q=80',
    NULL,
    (SELECT id FROM departments WHERE name = 'Public Works' LIMIT 1),
    (SELECT id FROM users WHERE email = 'aarav.sharma@example.com' LIMIT 1),
    NULL,
    'Asphalt patch team dispatched from Okhla batch plant. Roller on site.',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '1 day'
);

-- 2. Cyber City, DLF Phase 2, Gurgaon (WASTE, ASSIGNED)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Garbage accumulation behind Cyber Hub Building 10',
    'Unregulated commercial cardboard and food waste dumping near Cyber Hub back exit lane blocking pedestrian access.',
    'WASTE',
    28.4949,
    77.0895,
    'ASSIGNED',
    'https://images.unsplash.com/photo-1611284446314-60a58ac0deb9?auto=format&fit=crop&w=800&q=80',
    NULL,
    (SELECT id FROM departments WHERE name = 'Sanitation' LIMIT 1),
    (SELECT id FROM users WHERE email = 'pooja.verma@example.com' LIMIT 1),
    NULL,
    NULL,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'
);

-- 3. Golf Course Road, Sector 54, Gurgaon (ROADS, IN_PROGRESS - Parent Issue)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Severe pothole underpass near Rapid Metro Pillar 142',
    'Deep road cavity formed right after the underpass bend on Golf Course Road Sector 54. Causes heavy wheel rim damage.',
    'ROADS',
    28.4357,
    77.1066,
    'IN_PROGRESS',
    'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=800&q=80',
    NULL,
    (SELECT id FROM departments WHERE name = 'Public Works' LIMIT 1),
    (SELECT id FROM users WHERE email = 'aarav.sharma@example.com' LIMIT 1),
    NULL,
    'Lane closed with traffic cones. Hot-mix asphalt application scheduled for 11 PM.',
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '12 hours'
);

-- 4. Golf Course Road (DUPLICATE ISSUE - Child of #3, 20 metres away)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Dangerous sunken road trench near Pillar 142',
    'Sunken asphalt depression near Sector 54 Rapid Metro pillar 142. Cars are braking abruptly.',
    'ROADS',
    28.4358,
    77.1068,
    'REPORTED',
    'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=800&q=80',
    NULL,
    (SELECT id FROM departments WHERE name = 'Public Works' LIMIT 1),
    (SELECT id FROM users WHERE email = 'pooja.verma@example.com' LIMIT 1),
    (SELECT id FROM complaints WHERE title LIKE '%Pillar 142%' AND parent_complaint_id IS NULL ORDER BY id ASC LIMIT 1),
    NULL,
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days'
);

-- 5. MG Road Metro Station, Gurgaon (STREETLIGHTS, RESOLVED - with photo evidence)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Non-functional streetlights along MG Road service lane',
    'Four consecutive streetlight poles completely dead near MG Road Metro Gate 2 creating dangerous dark zone at night.',
    'STREETLIGHTS',
    28.4800,
    77.0804,
    'RESOLVED',
    'https://images.unsplash.com/photo-1509114397022-ed747cca3f65?auto=format&fit=crop&w=800&q=80',
    'https://images.unsplash.com/photo-1517649763962-0c623266ddc0?auto=format&fit=crop&w=800&q=80',
    (SELECT id FROM departments WHERE name = 'Electrical' LIMIT 1),
    (SELECT id FROM users WHERE email = 'pooja.verma@example.com' LIMIT 1),
    NULL,
    'Replaced damaged junction box circuit breaker and fitted new 90W LED luminaires on poles 12-15. Illuminance verified.',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '2 days'
);

-- 6. Lajpat Nagar Central Market, South Delhi (WASTE, RESOLVED - with photo evidence)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Overflowing community dumpster near Feroze Gandhi Road',
    'Large municipal bin overflowing with retail cartons and organic waste, spilling onto main market footpath.',
    'WASTE',
    28.5700,
    77.2370,
    'RESOLVED',
    'https://images.unsplash.com/photo-1530587191325-3db32d826c18?auto=format&fit=crop&w=800&q=80',
    'https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?auto=format&fit=crop&w=800&q=80',
    (SELECT id FROM departments WHERE name = 'Sanitation' LIMIT 1),
    (SELECT id FROM users WHERE email = 'aarav.sharma@example.com' LIMIT 1),
    NULL,
    'Compactor vehicle #DL-1M-4412 cleared 3.5 tonnes of waste. Area sanitized and washed.',
    NOW() - INTERVAL '6 days',
    NOW() - INTERVAL '1 day'
);

-- 7. Dwarka Sector 21, South-West Delhi (STREETLIGHTS, ASSIGNED)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Flickering high-mast junction light at Sector 21 crossing',
    'High mast lighting fixture is erratically strobing on the roundabout opposite Dwarka Sector 21 Metro terminal.',
    'STREETLIGHTS',
    28.5523,
    77.0583,
    'ASSIGNED',
    'https://images.unsplash.com/photo-1509114397022-ed747cca3f65?auto=format&fit=crop&w=800&q=80',
    NULL,
    (SELECT id FROM departments WHERE name = 'Electrical' LIMIT 1),
    (SELECT id FROM users WHERE email = 'aarav.sharma@example.com' LIMIT 1),
    NULL,
    NULL,
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
);

-- 8. Sector 29, HUDA City Centre, Gurgaon (OTHER, REPORTED)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Damaged pedestrian crossing railing with sharp metal edges',
    'Steel safety guard railing crushed by vehicle collision outside Sector 29 market entrance, protruding dangerously onto walking path.',
    'OTHER',
    28.4682,
    77.0632,
    'REPORTED',
    'https://images.unsplash.com/photo-1584463699028-2c26f0766c5a?auto=format&fit=crop&w=800&q=80',
    NULL,
    (SELECT id FROM departments WHERE name = 'General Administration' LIMIT 1),
    (SELECT id FROM users WHERE email = 'pooja.verma@example.com' LIMIT 1),
    NULL,
    NULL,
    NOW() - INTERVAL '8 hours',
    NOW() - INTERVAL '8 hours'
);

-- 9. Chandni Chowk, Old Delhi (WASTE, IN_PROGRESS)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Decaying vegetable residue pile near Town Hall walkway',
    'Heaps of perishable fruit and vegetable waste left on the pedestrian zone behind Chandni Chowk Town Hall.',
    'WASTE',
    28.6562,
    77.2309,
    'IN_PROGRESS',
    'https://images.unsplash.com/photo-1611284446314-60a58ac0deb9?auto=format&fit=crop&w=800&q=80',
    NULL,
    (SELECT id FROM departments WHERE name = 'Sanitation' LIMIT 1),
    (SELECT id FROM users WHERE email = 'aarav.sharma@example.com' LIMIT 1),
    NULL,
    'Special sanitation drive ongoing with 4 sweepers and manual tipper cart.',
    NOW() - INTERVAL '18 hours',
    NOW() - INTERVAL '6 hours'
);

-- 10. Sohna Road, Sector 48, Gurgaon (ROADS, ASSIGNED)
INSERT INTO complaints (
    title, description, category, latitude, longitude, status,
    before_image_url, after_image_url, department_id, reporter_id,
    parent_complaint_id, worker_notes, created_at, updated_at
)
VALUES (
    'Open stormwater drain concrete cover near Subhash Chowk',
    'Heavy concrete drain slab fractured and fallen into drain channel, leaving a dangerous 3-foot pit next to the bus stop.',
    'ROADS',
    28.4189,
    77.0378,
    'ASSIGNED',
    'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=800&q=80',
    NULL,
    (SELECT id FROM departments WHERE name = 'Public Works' LIMIT 1),
    (SELECT id FROM users WHERE email = 'pooja.verma@example.com' LIMIT 1),
    NULL,
    NULL,
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
);
