CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    permission VARCHAR(255) NOT NULL,
    operation VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT REFERENCES roles(id),
    permission_id BIGINT REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO permissions (permission, operation) VALUES
('user:read', 'READ_USER'),
('user:create', 'CREATE_USER'),
('user:update', 'UPDATE_USER'),
('user:delete', 'DELETE_USER'),
('role:read', 'READ_ROLE'),
('role:create', 'CREATE_ROLE'),
('role:update', 'UPDATE_ROLE'),
('role:delete', 'DELETE_ROLE'),
('permission:read', 'READ_PERMISSION'),
('permission:create', 'CREATE_PERMISSION'),
('permission:delete', 'DELETE_PERMISSION'),
('sensor:read', 'READ_SENSOR'),
('sensor:create', 'CREATE_SENSOR'),
('alert:read', 'READ_ALERT'),
('report:export', 'EXPORT_REPORT'),
('config:update', 'UPDATE_CONFIG')
ON CONFLICT (id) DO NOTHING;

INSERT INTO roles (title) VALUES
('ADMIN'),
('MANAGER'),
('OPERATOR'),
('kkm1337')
ON CONFLICT (title) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.title = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.title = 'MANAGER'
AND p.permission IN ('sensor:read', 'alert:read', 'report:export')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.title = 'OPERATOR' 
AND p.permission IN ('sensor:read', 'alert:read')
ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, enabled) VALUES
('admin', '$2b$12$KtvMzYkklj8HMprsCcws5OxhwCSYE8JBmtzaMyT.seIltI8b3fmLW', true),
('manager', '$2b$12$KtvMzYkklj8HMprsCcws5OxhwCSYE8JBmtzaMyT.seIltI8b3fmLW', true),
('operator', '$2b$12$idkezEtxInFRGdXFdWxuMOVPG6a7hj7T1wct4E9erc622wBthFEme', true) // nonath CAPS
('kkm1337', '$2b$12$KtvMzYkklj8HMprsCcws5OxhwCSYE8JBmtzaMyT.seIltI8b3fmLW', true)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.title = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'kkm1337' AND r.title = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'manager' AND r.title = 'MANAGER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'operator' AND r.title = 'OPERATOR'
ON CONFLICT DO NOTHING;

