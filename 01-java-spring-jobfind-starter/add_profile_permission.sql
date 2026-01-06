-- Add permissions for user self-service endpoints
-- Run this SQL script in your MySQL database

-- 1. Insert permission for updating user profile
INSERT INTO permissions (id, api_path, method, module, name, created_at, created_by)
VALUES 
(NULL, '/api/v1/users/profile', 'PUT', 'USERS', 'Update user profile (self)', NOW(), 'admin');

SET @profile_permission_id = LAST_INSERT_ID();

-- Assign to USER role (role_id = 2)
INSERT INTO permission_role (permission_id, role_id)
VALUES (@profile_permission_id, 2);

-- 2. Insert permission for changing password
INSERT INTO permissions (id, api_path, method, module, name, created_at, created_by)
VALUES 
(NULL, '/api/v1/users/change-password', 'PUT', 'USERS', 'Change own password', NOW(), 'admin');

SET @password_permission_id = LAST_INSERT_ID();

-- Assign to USER role (role_id = 2)
INSERT INTO permission_role (permission_id, role_id)
VALUES (@password_permission_id, 2);

-- Optional: Also assign to ADMIN and HR roles
-- INSERT INTO permission_role (permission_id, role_id) VALUES (@profile_permission_id, 1); -- ADMIN
-- INSERT INTO permission_role (permission_id, role_id) VALUES (@password_permission_id, 1); -- ADMIN

-- Verify the permissions were added
SELECT p.*, GROUP_CONCAT(r.name) as assigned_roles
FROM permissions p
LEFT JOIN permission_role pr ON p.id = pr.permission_id
LEFT JOIN roles r ON pr.role_id = r.id
WHERE p.api_path IN ('/api/v1/users/profile', '/api/v1/users/change-password')
GROUP BY p.id;
