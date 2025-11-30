---- MeetHub Database Initialization Script
---- Complete setup: extensions, schema, tables, indexes, sample data, and permissions
--
---- =============================================
---- 1. EXTENSIONS
---- =============================================
--CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--CREATE EXTENSION IF NOT EXISTS "cube";
--CREATE EXTENSION IF NOT EXISTS "earthdistance";
--
---- =============================================
---- 2. APPLICATION USER
---- =============================================
--CREATE USER meethub_app WITH PASSWORD 'app_password123';
--GRANT CONNECT ON DATABASE meethub TO meethub_app;
--
---- =============================================
---- 3. SCHEMA SETUP
---- =============================================
--CREATE SCHEMA IF NOT EXISTS meethub_schema;
--GRANT USAGE ON SCHEMA meethub_schema TO meethub_user, meethub_app;
--GRANT CREATE ON SCHEMA meethub_schema TO meethub_user;
--
---- Set default schema
--ALTER ROLE meethub_user SET search_path TO meethub_schema, public;
--ALTER ROLE meethub_app SET search_path TO meethub_schema, public;
--
---- =============================================
---- 4. TABLES
---- =============================================
--SET search_path TO meethub_schema;
--
---- Users table
--CREATE TABLE users (
--    id BIGSERIAL PRIMARY KEY,
--    email VARCHAR(255) UNIQUE NOT NULL,
--    password VARCHAR(255) NOT NULL,
--    first_name VARCHAR(100) NOT NULL,
--    last_name VARCHAR(100) NOT NULL,
--    role VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
--    phone_number VARCHAR(20),
--    enabled BOOLEAN NOT NULL DEFAULT TRUE,
--    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
--    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
--    account_locked_until TIMESTAMP,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- User preferences table
--CREATE TABLE user_preferences (
--    id BIGSERIAL PRIMARY KEY,
--    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
--    preference_key VARCHAR(100) NOT NULL,
--    preference_value VARCHAR(500),
--    privacy_level VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Locations table
--CREATE TABLE locations (
--    id BIGSERIAL PRIMARY KEY,
--    name VARCHAR(200) NOT NULL,
--    address TEXT,
--    city VARCHAR(100),
--    country VARCHAR(100),
--    latitude NUMERIC(10, 6),
--    longitude NUMERIC(10, 6),
--    type VARCHAR(20) NOT NULL,
--    virtual_meeting_url VARCHAR(500),
--    access_code VARCHAR(50),
--    driving_instructions TEXT,
--    timezone VARCHAR(50)
--);
--
---- Meetings table
--CREATE TABLE meetings (
--    id BIGSERIAL PRIMARY KEY,
--    title VARCHAR(200) NOT NULL,
--    description TEXT,
--    agenda TEXT,
--    type VARCHAR(20) NOT NULL,
--    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
--    visibility VARCHAR(20) NOT NULL,
--    start_date TIMESTAMP NOT NULL,
--    end_date TIMESTAMP NOT NULL,
--    max_participants INTEGER,
--    organizer_id BIGINT NOT NULL REFERENCES users(id),
--    location_id BIGINT REFERENCES locations(id),
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Meeting tags table
--CREATE TABLE meeting_tags (
--    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
--    tag VARCHAR(100) NOT NULL,
--    PRIMARY KEY (meeting_id, tag)
--);
--
---- Meeting participants table
--CREATE TABLE meeting_participants (
--    id BIGSERIAL PRIMARY KEY,
--    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
--    user_id BIGINT NOT NULL REFERENCES users(id),
--    status VARCHAR(20) NOT NULL DEFAULT 'INVITED',
--    permission_level VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
--    comment TEXT,
--    invitation_token VARCHAR(100) UNIQUE,
--    token_expires_at TIMESTAMP,
--    response_date TIMESTAMP,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    UNIQUE (meeting_id, user_id)
--);
--
---- Meeting resources table
--CREATE TABLE meeting_resources (
--    id BIGSERIAL PRIMARY KEY,
--    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
--    filename VARCHAR(255) NOT NULL,
--    original_filename VARCHAR(255) NOT NULL,
--    file_path VARCHAR(500) NOT NULL,
--    file_size BIGINT,
--    mime_type VARCHAR(100),
--    resource_type VARCHAR(50) NOT NULL,
--    version INTEGER NOT NULL DEFAULT 1,
--    is_current BOOLEAN NOT NULL DEFAULT TRUE,
--    uploaded_by BIGINT REFERENCES users(id),
--    access_level VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANTS',
--    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Meeting tasks table
--CREATE TABLE meeting_tasks (
--    id BIGSERIAL PRIMARY KEY,
--    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
--    title VARCHAR(200) NOT NULL,
--    description TEXT,
--    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
--    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
--    assigned_to BIGINT REFERENCES users(id),
--    due_date TIMESTAMP,
--    completed_at TIMESTAMP,
--    progress_percentage INTEGER NOT NULL DEFAULT 0,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Notifications table
--CREATE TABLE notifications (
--    id BIGSERIAL PRIMARY KEY,
--    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
--    title VARCHAR(200) NOT NULL,
--    message TEXT,
--    type VARCHAR(20) NOT NULL,
--    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
--    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
--    reference_id BIGINT,
--    reference_type VARCHAR(50),
--    sent_at TIMESTAMP,
--    read_at TIMESTAMP,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- =============================================
---- 5. INDEXES
---- =============================================
--
---- Users indexes
--CREATE INDEX idx_user_email ON users(email);
--CREATE INDEX idx_user_role ON users(role);
--CREATE INDEX idx_user_created ON users(created_at);
--
---- User preferences indexes
--CREATE INDEX idx_user_pref_user ON user_preferences(user_id);
--CREATE INDEX idx_user_pref_key ON user_preferences(preference_key);
--
---- Locations indexes
--CREATE INDEX idx_location_coordinates ON locations(latitude, longitude);
--CREATE INDEX idx_location_type ON locations(type);
--CREATE INDEX idx_location_city ON locations(city);
--
---- Meetings indexes
--CREATE INDEX idx_meeting_organizer ON meetings(organizer_id);
--CREATE INDEX idx_meeting_status ON meetings(status);
--CREATE INDEX idx_meeting_start_date ON meetings(start_date);
--CREATE INDEX idx_meeting_end_date ON meetings(end_date);
--CREATE INDEX idx_meeting_visibility ON meetings(visibility);
--CREATE INDEX idx_meeting_date_range ON meetings(start_date, end_date);
--
---- Meeting participants indexes
--CREATE INDEX idx_participant_meeting ON meeting_participants(meeting_id);
--CREATE INDEX idx_participant_user ON meeting_participants(user_id);
--CREATE INDEX idx_participant_status ON meeting_participants(status);
--
---- Meeting resources indexes
--CREATE INDEX idx_resource_meeting ON meeting_resources(meeting_id);
--CREATE INDEX idx_resource_type ON meeting_resources(resource_type);
--
---- Meeting tasks indexes
--CREATE INDEX idx_task_meeting ON meeting_tasks(meeting_id);
--CREATE INDEX idx_task_assignee ON meeting_tasks(assigned_to);
--CREATE INDEX idx_task_status ON meeting_tasks(status);
--CREATE INDEX idx_task_due_date ON meeting_tasks(due_date);
--
---- Notifications indexes
--CREATE INDEX idx_notification_user ON notifications(user_id);
--CREATE INDEX idx_notification_status ON notifications(status);
--CREATE INDEX idx_notification_created ON notifications(created_at);
--
--CREATE TABLE meethub_schema.resource_tags (
--    resource_id BIGINT NOT NULL REFERENCES meethub_schema.meeting_resources(id) ON DELETE CASCADE,
--    tag VARCHAR(100) NOT NULL,
--    PRIMARY KEY (resource_id, tag)
--);
---- =============================================
---- 6. SAMPLE DATA
---- =============================================
--
---- Insert sample users (passwords are bcrypt encoded "password123")
--INSERT INTO users (email, password, first_name, last_name, role) VALUES
--('admin@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Admin', 'User', 'ADMIN'),
--('organizer@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'John', 'Organizer', 'ORGANIZER'),
--('user1@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Alice', 'Participant', 'PARTICIPANT'),
--('user2@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Bob', 'Attendee', 'PARTICIPANT');
---- Insert sample locations
--INSERT INTO locations (name, address, city, country, latitude, longitude, type) VALUES
--('Conference Room A', '123 Main St', 'Warsaw', 'Poland', 52.2297, 21.0122, 'PHYSICAL'),
--('Virtual Meeting Room', NULL, NULL, NULL, NULL, NULL, 'VIRTUAL'),
--('Tech Hub Office', '456 Tech Ave', 'Krakow', 'Poland', 50.0647, 19.9450, 'PHYSICAL');
--
---- Insert sample meetings
--INSERT INTO meetings (title, description, agenda, type, status, visibility, start_date, end_date, max_participants, organizer_id, location_id) VALUES
--('Spring Boot Workshop', 'Learn Spring Boot with hands-on examples', '1. Introduction 2. Hands-on coding 3. Q&A', 'PHYSICAL', 'CONFIRMED', 'PUBLIC', '2024-02-15 10:00:00', '2024-02-15 12:00:00', 20, 2, 1),
--('Project Planning', 'Quarterly project planning meeting', 'Review goals, assign tasks, set deadlines', 'VIRTUAL', 'PLANNED', 'INVITATION_ONLY', '2024-02-20 14:00:00', '2024-02-20 15:30:00', 10, 2, 2),
--('Team Building', 'Monthly team building activity', 'Fun activities and games for team bonding', 'PHYSICAL', 'CONFIRMED', 'PUBLIC', '2024-02-25 16:00:00', '2024-02-25 18:00:00', 30, 2, 3);
--
---- Insert sample meeting participants
--INSERT INTO meeting_participants (meeting_id, user_id, status, permission_level) VALUES
--(1, 3, 'CONFIRMED', 'PARTICIPANT'),
--(1, 4, 'INVITED', 'PARTICIPANT'),
--(2, 3, 'CONFIRMED', 'CONTRIBUTOR'),
--(3, 4, 'CONFIRMED', 'PARTICIPANT');
--
---- Insert sample meeting tasks
--INSERT INTO meeting_tasks (meeting_id, title, description, status, priority, assigned_to, due_date, progress_percentage) VALUES
--(1, 'Prepare presentation', 'Create slides for Spring Boot workshop', 'IN_PROGRESS', 'HIGH', 2, '2024-02-14 18:00:00', 75),
--(1, 'Book conference room', 'Reserve Conference Room A for the workshop', 'COMPLETED', 'MEDIUM', 2, '2024-02-10 12:00:00', 100),
--(2, 'Create project timeline', 'Develop detailed project timeline with milestones', 'PENDING', 'HIGH', 3, '2024-02-18 17:00:00', 0);
--
---- =============================================
---- 7. PERMISSIONS
---- =============================================
--
---- Grant permissions to application user
--GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA meethub_schema TO meethub_user;
--GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA meethub_schema TO meethub_user;
--GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA meethub_schema TO meethub_app;
--GRANT USAGE ON ALL SEQUENCES IN SCHEMA meethub_schema TO meethub_app;
--
---- =============================================
---- 8. UPDATE SEQUENCES (ensure proper ID generation)
---- =============================================
--SELECT setval('meethub_schema.users_id_seq', (SELECT MAX(id) FROM meethub_schema.users));
--SELECT setval('meethub_schema.user_preferences_id_seq', (SELECT MAX(id) FROM meethub_schema.user_preferences));
--SELECT setval('meethub_schema.locations_id_seq', (SELECT MAX(id) FROM meethub_schema.locations));
--SELECT setval('meethub_schema.meetings_id_seq', (SELECT MAX(id) FROM meethub_schema.meetings));
--SELECT setval('meethub_schema.meeting_participants_id_seq', (SELECT MAX(id) FROM meethub_schema.meeting_participants));
--SELECT setval('meethub_schema.meeting_resources_id_seq', (SELECT MAX(id) FROM meethub_schema.meeting_resources));
--SELECT setval('meethub_schema.meeting_tasks_id_seq', (SELECT MAX(id) FROM meethub_schema.meeting_tasks));
--SELECT setval('meethub_schema.notifications_id_seq', (SELECT MAX(id) FROM meethub_schema.notifications));
--
--
---- Set default schema for user
--ALTER USER meethub_user SET search_path TO meethub_schema, public;
















--
---- MeetHub Database Initialization Script
---- Complete setup: extensions, schema, tables, indexes, sample data, and permissions
--
---- =============================================
---- 1. EXTENSIONS
---- =============================================
--CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--CREATE EXTENSION IF NOT EXISTS "cube";
--CREATE EXTENSION IF NOT EXISTS "earthdistance";
--
---- =============================================
---- 2. APPLICATION USER
---- =============================================
--CREATE USER meethub_app WITH PASSWORD 'app_password123';
--GRANT CONNECT ON DATABASE meethub TO meethub_app;
--
---- =============================================
---- 3. SCHEMA SETUP
---- =============================================
--CREATE SCHEMA IF NOT EXISTS meethub_schema;
--GRANT USAGE ON SCHEMA meethub_schema TO meethub_user, meethub_app;
--GRANT CREATE ON SCHEMA meethub_schema TO meethub_user;
--
---- Set default schema
--ALTER ROLE meethub_user SET search_path TO meethub_schema, public;
--ALTER ROLE meethub_app SET search_path TO meethub_schema, public;
--
---- =============================================
---- 4. TABLES
---- =============================================
--SET search_path TO meethub_schema;
--
---- Users table
--CREATE TABLE users (
--    id BIGSERIAL PRIMARY KEY,
--    email VARCHAR(255) UNIQUE NOT NULL,
--    password VARCHAR(255) NOT NULL,
--    first_name VARCHAR(100) NOT NULL,
--    last_name VARCHAR(100) NOT NULL,
--    role VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
--    phone_number VARCHAR(20),
--    enabled BOOLEAN NOT NULL DEFAULT TRUE,
--    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
--    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
--    account_locked_until TIMESTAMP,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- User preferences table
--CREATE TABLE user_preferences (
--    id BIGSERIAL PRIMARY KEY,
--    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
--    preference_key VARCHAR(100) NOT NULL,
--    preference_value VARCHAR(500),
--    privacy_level VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Locations table
--CREATE TABLE locations (
--    id BIGSERIAL PRIMARY KEY,
--    name VARCHAR(200) NOT NULL,
--    address TEXT,
--    city VARCHAR(100),
--    country VARCHAR(100),
--    latitude NUMERIC(10, 6),
--    longitude NUMERIC(10, 6),
--    type VARCHAR(20) NOT NULL,
--    type VARCHAR(20) NOT NULL,
--    virtual_meeting_url VARCHAR(500),
--    access_code VARCHAR(50),
--    driving_instructions TEXT,
--    timezone VARCHAR(50)
--);
--
---- Meetings table
--CREATE TABLE meetings (
--    id BIGSERIAL PRIMARY KEY,
--    title VARCHAR(200) NOT NULL,
--    description TEXT,
--    agenda TEXT,
--    type VARCHAR(20) NOT NULL,
--    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
--    visibility VARCHAR(20) NOT NULL,
--    start_date TIMESTAMP NOT NULL,
--    end_date TIMESTAMP NOT NULL,
--    max_participants INTEGER,
--    organizer_id BIGINT NOT NULL REFERENCES users(id),
--    location_id BIGINT REFERENCES locations(id),
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Meeting tags table
--CREATE TABLE meeting_tags (
--    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
--    tag VARCHAR(100) NOT NULL,
--    PRIMARY KEY (meeting_id, tag)
--);
--
---- Meeting participants table
--CREATE TABLE meeting_participants (
--    id BIGSERIAL PRIMARY KEY,
--    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
--    user_id BIGINT NOT NULL REFERENCES users(id),
--    status VARCHAR(20) NOT NULL DEFAULT 'INVITED',
--    permission_level VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
--    comment TEXT,
--    invitation_token VARCHAR(100) UNIQUE,
--    token_expires_at TIMESTAMP,
--    response_date TIMESTAMP,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    UNIQUE (meeting_id, user_id)
--);
--
---- BRAKUJĄCE TABELE:
--
---- Participant status history table
--CREATE TABLE participant_status_history (
--    id BIGSERIAL PRIMARY KEY,
--    participant_id BIGINT NOT NULL REFERENCES meeting_participants(id) ON DELETE CASCADE,
--    old_status VARCHAR(20),
--    new_status VARCHAR(20) NOT NULL,
--    comment TEXT,
--    changed_by_user_id BIGINT,
--    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--);
--
---- Waitlist entries table
--CREATE TABLE waitlist_entries (
--    id BIGSERIAL PRIMARY KEY,
--    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
--    user_id BIGINT NOT NULL REFERENCES users(id),
--    position INTEGER NOT NULL,
--    auto_promote BOOLEAN NOT NULL DEFAULT TRUE,  -- Dodane
--    notified_at TIMESTAMP,                       -- Dodane
--    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    UNIQUE (meeting_id, user_id)
--);
--
---- User groups table
--CREATE TABLE user_groups (
--    id BIGSERIAL PRIMARY KEY,
--    name VARCHAR(200) NOT NULL UNIQUE,
--    description TEXT,
--    created_by BIGINT NOT NULL REFERENCES users(id),
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- User group members table
--CREATE TABLE user_group_members (
--    id BIGSERIAL PRIMARY KEY,
--    group_id BIGINT NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
--    user_id BIGINT NOT NULL REFERENCES users(id),
--    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
--    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    UNIQUE (group_id, user_id)
--);
--
---- Meeting resources table
--CREATE TABLE meeting_resources (
--    id BIGSERIAL PRIMARY KEY,
--    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
--    filename VARCHAR(255) NOT NULL,
--    original_filename VARCHAR(255) NOT NULL,
--    file_path VARCHAR(500) NOT NULL,
--    file_size BIGINT,
--    mime_type VARCHAR(100),
--    resource_type VARCHAR(50) NOT NULL,
--    version INTEGER NOT NULL DEFAULT 1,
--    is_current BOOLEAN NOT NULL DEFAULT TRUE,
--    uploaded_by BIGINT REFERENCES users(id),
--    access_level VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANTS',
--    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Resource tags table
--CREATE TABLE resource_tags (
--    resource_id BIGINT NOT NULL REFERENCES meeting_resources(id) ON DELETE CASCADE,
--    tag VARCHAR(100) NOT NULL,
--    PRIMARY KEY (resource_id, tag)
--);
--
---- Meeting tasks table
--CREATE TABLE meeting_tasks (
--    id BIGSERIAL PRIMARY KEY,
--    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
--    title VARCHAR(200) NOT NULL,
--    description TEXT,
--    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
--    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
--    assigned_to BIGINT REFERENCES users(id),
--    due_date TIMESTAMP,
--    completed_at TIMESTAMP,
--    progress_percentage INTEGER NOT NULL DEFAULT 0,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Notifications table
--CREATE TABLE notifications (
--    id BIGSERIAL PRIMARY KEY,
--    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
--    title VARCHAR(200) NOT NULL,
--    message TEXT,
--    type VARCHAR(20) NOT NULL,
--    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
--    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
--    reference_id BIGINT,
--    reference_type VARCHAR(50),
--    sent_at TIMESTAMP,
--    read_at TIMESTAMP,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Audit logs table
--CREATE TABLE audit_logs (
--    id BIGSERIAL PRIMARY KEY,
--    action VARCHAR(100) NOT NULL,
--    entity_type VARCHAR(100) NOT NULL,
--    entity_id BIGINT,
--    old_values JSONB,
--    new_values JSONB,
--    performed_by BIGINT REFERENCES users(id),
--    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    ip_address VARCHAR(45),
--    user_agent TEXT
--);
--
---- Email templates table
--CREATE TABLE email_templates (
--    id BIGSERIAL PRIMARY KEY,
--    template_key VARCHAR(100) UNIQUE NOT NULL,
--    subject VARCHAR(500) NOT NULL,
--    body_template TEXT NOT NULL,
--    language VARCHAR(10) NOT NULL DEFAULT 'pl',
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- =============================================
---- 5. INDEXES
---- =============================================
--
---- Users indexes
--CREATE INDEX idx_user_email ON users(email);
--CREATE INDEX idx_user_role ON users(role);
--CREATE INDEX idx_user_created ON users(created_at);
--
---- User preferences indexes
--CREATE INDEX idx_user_pref_user ON user_preferences(user_id);
--CREATE INDEX idx_user_pref_key ON user_preferences(preference_key);
--
---- Locations indexes
--CREATE INDEX idx_location_coordinates ON locations(latitude, longitude);
--CREATE INDEX idx_location_type ON locations(type);
--CREATE INDEX idx_location_city ON locations(city);
--
---- Meetings indexes
--CREATE INDEX idx_meeting_organizer ON meetings(organizer_id);
--CREATE INDEX idx_meeting_status ON meetings(status);
--CREATE INDEX idx_meeting_start_date ON meetings(start_date);
--CREATE INDEX idx_meeting_end_date ON meetings(end_date);
--CREATE INDEX idx_meeting_visibility ON meetings(visibility);
--CREATE INDEX idx_meeting_date_range ON meetings(start_date, end_date);
--
---- Meeting participants indexes
--CREATE INDEX idx_participant_meeting ON meeting_participants(meeting_id);
--CREATE INDEX idx_participant_user ON meeting_participants(user_id);
--CREATE INDEX idx_participant_status ON meeting_participants(status);
--
---- Participant status history indexes
--CREATE INDEX idx_status_history_participant ON participant_status_history(participant_id);
--CREATE INDEX idx_status_history_changed_by ON participant_status_history(changed_by_user_id);
--CREATE INDEX idx_status_history_changed_at ON participant_status_history(changed_at);
--
---- Waitlist entries indexes
--CREATE INDEX idx_waitlist_meeting ON waitlist_entries(meeting_id);
--CREATE INDEX idx_waitlist_user ON waitlist_entries(user_id);
--CREATE INDEX idx_waitlist_position ON waitlist_entries(meeting_id, position);
--
---- User groups indexes
--CREATE INDEX idx_group_name ON user_groups(name);
--CREATE INDEX idx_group_created_by ON user_groups(created_by);
--
---- User group members indexes
--CREATE INDEX idx_group_member_group ON user_group_members(group_id);
--CREATE INDEX idx_group_member_user ON user_group_members(user_id);
--
---- Meeting resources indexes
--CREATE INDEX idx_resource_meeting ON meeting_resources(meeting_id);
--CREATE INDEX idx_resource_type ON meeting_resources(resource_type);
--
---- Meeting tasks indexes
--CREATE INDEX idx_task_meeting ON meeting_tasks(meeting_id);
--CREATE INDEX idx_task_assignee ON meeting_tasks(assigned_to);
--CREATE INDEX idx_task_status ON meeting_tasks(status);
--CREATE INDEX idx_task_due_date ON meeting_tasks(due_date);
--
---- Notifications indexes
--CREATE INDEX idx_notification_user ON notifications(user_id);
--CREATE INDEX idx_notification_status ON notifications(status);
--CREATE INDEX idx_notification_created ON notifications(created_at);
--
---- Audit logs indexes
--CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
--CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);
--CREATE INDEX idx_audit_performed_at ON audit_logs(performed_at);
--
---- Email templates indexes
--CREATE INDEX idx_email_template_key ON email_templates(template_key);
--
---- =============================================
---- 6. SAMPLE DATA
---- =============================================
--
---- Insert sample users (passwords are bcrypt encoded "password123")
--INSERT INTO users (email, password, first_name, last_name, role) VALUES
--('admin@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Admin', 'User', 'ADMIN'),
--('organizer@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'John', 'Organizer', 'ORGANIZER'),
--('user1@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Alice', 'Participant', 'PARTICIPANT'),
--('user2@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Bob', 'Attendee', 'PARTICIPANT'),
--('a@a', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Bob', 'Attendee', 'PARTICIPANT');
--
--
---- Insert sample locations
--INSERT INTO locations (name, address, city, country, latitude, longitude, type) VALUES
--('Conference Room A', '123 Main St', 'Warsaw', 'Poland', 52.2297, 21.0122, 'PHYSICAL'),
--('Virtual Meeting Room', NULL, NULL, NULL, NULL, NULL, 'VIRTUAL'),
--('Tech Hub Office', '456 Tech Ave', 'Krakow', 'Poland', 50.0647, 19.9450, 'PHYSICAL');
--
---- Insert sample meetings
--INSERT INTO meetings (title, description, agenda, type, status, visibility, start_date, end_date, max_participants, organizer_id, location_id) VALUES
--('Spring Boot Workshop', 'Learn Spring Boot with hands-on examples', '1. Introduction 2. Hands-on coding 3. Q&A', 'PHYSICAL', 'CONFIRMED', 'PUBLIC', '2024-02-15 10:00:00', '2024-02-15 12:00:00', 20, 2, 1),
--('Project Planning', 'Quarterly project planning meeting', 'Review goals, assign tasks, set deadlines', 'VIRTUAL', 'PLANNED', 'INVITATION_ONLY', '2024-02-20 14:00:00', '2024-02-20 15:30:00', 10, 2, 2),
--('Team Building', 'Monthly team building activity', 'Fun activities and games for team bonding', 'PHYSICAL', 'CONFIRMED', 'PUBLIC', '2024-02-25 16:00:00', '2024-02-25 18:00:00', 30, 2, 3);
--
---- Insert sample meeting participants
--INSERT INTO meeting_participants (meeting_id, user_id, status, permission_level) VALUES
--(1, 3, 'CONFIRMED', 'PARTICIPANT'),
--(1, 4, 'INVITED', 'PARTICIPANT'),
--(2, 3, 'CONFIRMED', 'CONTRIBUTOR'),
--(3, 4, 'CONFIRMED', 'PARTICIPANT');
--
---- Insert sample participant status history
--INSERT INTO participant_status_history (participant_id, old_status, new_status, comment, changed_by_user_id) VALUES
--(1, NULL, 'INVITED', 'Initial invitation sent', 2),
--(1, 'INVITED', 'CONFIRMED', 'User accepted invitation', 3),
--(2, NULL, 'INVITED', 'Initial invitation sent', 2);
--
---- Insert sample waitlist entries
--INSERT INTO waitlist_entries (meeting_id, user_id, position) VALUES
--(1, 4, 1);
--
---- Insert sample user groups
--INSERT INTO user_groups (name, description, created_by) VALUES
--('Developers', 'Software development team', 2),
--('Managers', 'Project management team', 2),
--('Design Team', 'UI/UX design team', 3);
--
---- Insert sample user group members
--INSERT INTO user_group_members (group_id, user_id, role) VALUES
--(1, 3, 'MEMBER'),
--(1, 4, 'MEMBER'),
--(2, 2, 'ADMIN'),
--(3, 3, 'MEMBER');
--
---- Insert sample meeting tasks
--INSERT INTO meeting_tasks (meeting_id, title, description, status, priority, assigned_to, due_date, progress_percentage) VALUES
--(1, 'Prepare presentation', 'Create slides for Spring Boot workshop', 'IN_PROGRESS', 'HIGH', 2, '2024-02-14 18:00:00', 75),
--(1, 'Book conference room', 'Reserve Conference Room A for the workshop', 'COMPLETED', 'MEDIUM', 2, '2024-02-10 12:00:00', 100),
--(2, 'Create project timeline', 'Develop detailed project timeline with milestones', 'PENDING', 'HIGH', 3, '2024-02-18 17:00:00', 0);
--
---- Insert sample email templates
--INSERT INTO email_templates (template_key, subject, body_template, language) VALUES
--('meeting-invitation', 'Zaproszenie do spotkania: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Zostałeś zaproszony do spotkania "{{meetingTitle}}" organizowanego przez {{organizerName}}.<br><br>Data: {{meetingDate}}<br><br><a href="{{confirmationLink}}">Potwierdź udział</a>', 'pl'),
--('waitlist-notification', 'Zostałeś dodany do listy oczekujących: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Zostałeś dodany do listy oczekujących na spotkanie "{{meetingTitle}}". Twoja pozycja w kolejce: {{position}}.<br><br>Powiadomimy Cię gdy miejsce się zwolni.', 'pl'),
--('waitlist-promotion', 'Miejsce zwolniło się w spotkaniu: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Miejsce zwolniło się w spotkaniu "{{meetingTitle}}" i zostałeś automatycznie zapisany!<br><br>Zapraszamy do udziału.', 'pl');
--
---- =============================================
---- 7. PERMISSIONS
---- =============================================
--
---- Grant permissions to application user
--GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA meethub_schema TO meethub_user;
--GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA meethub_schema TO meethub_user;
--GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA meethub_schema TO meethub_app;
--GRANT USAGE ON ALL SEQUENCES IN SCHEMA meethub_schema TO meethub_app;
--
---- =============================================
---- 8. UPDATE SEQUENCES (ensure proper ID generation)
---- =============================================
--SELECT setval('meethub_schema.users_id_seq', (SELECT MAX(id) FROM meethub_schema.users));
--SELECT setval('meethub_schema.user_preferences_id_seq', (SELECT MAX(id) FROM meethub_schema.user_preferences));
--SELECT setval('meethub_schema.locations_id_seq', (SELECT MAX(id) FROM meethub_schema.locations));
--SELECT setval('meethub_schema.meetings_id_seq', (SELECT MAX(id) FROM meethub_schema.meetings));
--SELECT setval('meethub_schema.meeting_participants_id_seq', (SELECT MAX(id) FROM meethub_schema.meeting_participants));
--SELECT setval('meethub_schema.participant_status_history_id_seq', (SELECT MAX(id) FROM meethub_schema.participant_status_history));
--SELECT setval('meethub_schema.waitlist_entries_id_seq', (SELECT MAX(id) FROM meethub_schema.waitlist_entries));
--SELECT setval('meethub_schema.user_groups_id_seq', (SELECT MAX(id) FROM meethub_schema.user_groups));
--SELECT setval('meethub_schema.user_group_members_id_seq', (SELECT MAX(id) FROM meethub_schema.user_group_members));
--SELECT setval('meethub_schema.meeting_resources_id_seq', (SELECT MAX(id) FROM meethub_schema.meeting_resources));
--SELECT setval('meethub_schema.meeting_tasks_id_seq', (SELECT MAX(id) FROM meethub_schema.meeting_tasks));
--SELECT setval('meethub_schema.notifications_id_seq', (SELECT MAX(id) FROM meethub_schema.notifications));
--SELECT setval('meethub_schema.audit_logs_id_seq', (SELECT MAX(id) FROM meethub_schema.audit_logs));
--SELECT setval('meethub_schema.email_templates_id_seq', (SELECT MAX(id) FROM meethub_schema.email_templates));
--
---- Set default schema for user
--ALTER USER meethub_user SET search_path TO meethub_schema, public;
--
--
--
---- Rozszerzenie tabeli users
--ALTER TABLE meethub_schema.users
--ADD COLUMN email_notifications_enabled BOOLEAN DEFAULT TRUE,
--ADD COLUMN push_notifications_enabled BOOLEAN DEFAULT TRUE,
--ADD COLUMN sms_notifications_enabled BOOLEAN DEFAULT FALSE,
--ADD COLUMN digest_enabled BOOLEAN DEFAULT TRUE,
--ADD COLUMN digest_frequency VARCHAR(20) DEFAULT 'DAILY',
--ADD COLUMN timezone VARCHAR(50) DEFAULT 'Europe/Warsaw',
--ADD COLUMN language VARCHAR(10) DEFAULT 'pl';
--
---- Tabela kanałów powiadomień użytkownika
--CREATE TABLE meethub_schema.user_notification_channels (
--    user_id BIGINT NOT NULL REFERENCES meethub_schema.users(id) ON DELETE CASCADE,
--    channel VARCHAR(20) NOT NULL,
--    PRIMARY KEY (user_id, channel)
--);
--
---- Rozszerzenie tabeli notifications
--ALTER TABLE meethub_schema.notifications
--ADD COLUMN template_key VARCHAR(100),
--ADD COLUMN scheduled_for TIMESTAMP,
--ADD COLUMN delivered_at TIMESTAMP,
--ADD COLUMN retry_count INTEGER DEFAULT 0,
--ADD COLUMN error_message TEXT;
--
---- Tabela zmiennych szablonów powiadomień
--CREATE TABLE meethub_schema.notification_variables (
--    notification_id BIGINT NOT NULL REFERENCES meethub_schema.notifications(id) ON DELETE CASCADE,
--    variable_key VARCHAR(100) NOT NULL,
--    variable_value TEXT,
--    PRIMARY KEY (notification_id, variable_key)
--);
--
---- Tabela harmonogramów powiadomień
--CREATE TABLE meethub_schema.notification_schedules (
--    id BIGSERIAL PRIMARY KEY,
--    user_id BIGINT NOT NULL REFERENCES meethub_schema.users(id) ON DELETE CASCADE,
--    schedule_type VARCHAR(50) NOT NULL,
--    trigger_time VARCHAR(20),
--    enabled BOOLEAN NOT NULL DEFAULT TRUE,
--    custom_settings TEXT,
--    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--);
--
---- Indeksy dla wydajności
--CREATE INDEX idx_notifications_scheduled ON meethub_schema.notifications(scheduled_for, status);
--CREATE INDEX idx_notifications_user_status ON meethub_schema.notifications(user_id, status);
--CREATE INDEX idx_notifications_created ON meethub_schema.notifications(created_at DESC);
--CREATE INDEX idx_notification_schedules_user ON meethub_schema.notification_schedules(user_id);
--
---- Wstaw domyślne kanały dla istniejących użytkowników
--INSERT INTO meethub_schema.user_notification_channels (user_id, channel)
--SELECT id, 'EMAIL' FROM meethub_schema.users
--WHERE email_notifications_enabled = true;
--
--INSERT INTO meethub_schema.user_notification_channels (user_id, channel)
--SELECT id, 'PUSH' FROM meethub_schema.users
--WHERE push_notifications_enabled = true;
--
--INSERT INTO meethub_schema.user_notification_channels (user_id, channel)
--SELECT id, 'SMS' FROM meethub_schema.users
--WHERE sms_notifications_enabled = true;











-- MeetHub Database Initialization Script
-- Complete setup: extensions, schema, tables, indexes, sample data, and permissions

-- =============================================
-- 1. CREATE DATABASE (jeśli nie istnieje)
-- =============================================
SELECT 'CREATE DATABASE meethub' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'meethub');\gexec

-- =============================================
-- 2. EXTENSIONS
-- =============================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "cube";
CREATE EXTENSION IF NOT EXISTS "earthdistance";

-- =============================================
-- 3. APPLICATION USER
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'meethub_app') THEN
        CREATE USER meethub_app WITH PASSWORD 'app_password123';
    END IF;
END $$;

GRANT CONNECT ON DATABASE meethub TO meethub_app;

-- =============================================
-- 4. SCHEMA SETUP
-- =============================================
CREATE SCHEMA IF NOT EXISTS meethub_schema;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'meethub_user') THEN
        CREATE USER meethub_user WITH PASSWORD 'user_password123';
    END IF;
END $$;

GRANT USAGE ON SCHEMA meethub_schema TO meethub_user, meethub_app;
GRANT CREATE ON SCHEMA meethub_schema TO meethub_user;

-- Set default schema
ALTER ROLE meethub_user SET search_path TO meethub_schema, public;
ALTER ROLE meethub_app SET search_path TO meethub_schema, public;

-- =============================================
-- 5. TABLES
-- =============================================
SET search_path TO meethub_schema;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
    phone_number VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    account_locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preference_key VARCHAR(100) NOT NULL,
    preference_value VARCHAR(500),
    privacy_level VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Locations table (POPRAWIONE - usunięto powtórzoną kolumnę type)
CREATE TABLE IF NOT EXISTS locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100),
    latitude NUMERIC(10, 6),
    longitude NUMERIC(10, 6),
    type VARCHAR(20) NOT NULL,
    virtual_meeting_url VARCHAR(500),
    access_code VARCHAR(50),
    driving_instructions TEXT,
    timezone VARCHAR(50)
);

-- Meetings table
CREATE TABLE IF NOT EXISTS meetings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    agenda TEXT,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    visibility VARCHAR(20) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    max_participants INTEGER,
    organizer_id BIGINT NOT NULL REFERENCES users(id),
    location_id BIGINT REFERENCES locations(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Meeting tags table
CREATE TABLE IF NOT EXISTS meeting_tags (
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (meeting_id, tag)
);

-- Meeting participants table
CREATE TABLE IF NOT EXISTS meeting_participants (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'INVITED',
    permission_level VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT',
    comment TEXT,
    invitation_token VARCHAR(100) UNIQUE,
    token_expires_at TIMESTAMP,
    response_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (meeting_id, user_id)
);

-- BRAKUJĄCE TABELE:

-- Participant status history table
CREATE TABLE IF NOT EXISTS participant_status_history (
    id BIGSERIAL PRIMARY KEY,
    participant_id BIGINT NOT NULL REFERENCES meeting_participants(id) ON DELETE CASCADE,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    comment TEXT,
    changed_by_user_id BIGINT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Waitlist entries table
CREATE TABLE IF NOT EXISTS waitlist_entries (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    position INTEGER NOT NULL,
    auto_promote BOOLEAN NOT NULL DEFAULT TRUE,
    notified_at TIMESTAMP,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (meeting_id, user_id)
);

-- User groups table
CREATE TABLE IF NOT EXISTS user_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User group members table
CREATE TABLE IF NOT EXISTS user_group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (group_id, user_id)
);

-- Meeting resources table
CREATE TABLE IF NOT EXISTS meeting_resources (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    resource_type VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    uploaded_by BIGINT REFERENCES users(id),
    access_level VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANTS',
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Resource tags table
CREATE TABLE IF NOT EXISTS resource_tags (
    resource_id BIGINT NOT NULL REFERENCES meeting_resources(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (resource_id, tag)
);

-- Meeting tasks table
CREATE TABLE IF NOT EXISTS meeting_tasks (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assigned_to BIGINT REFERENCES users(id),
    due_date TIMESTAMP,
    completed_at TIMESTAMP,
    progress_percentage INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    reference_id BIGINT,
    reference_type VARCHAR(50),
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    old_values JSONB,
    new_values JSONB,
    performed_by BIGINT REFERENCES users(id),
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT
);

-- Email templates table
CREATE TABLE IF NOT EXISTS email_templates (
    id BIGSERIAL PRIMARY KEY,
    template_key VARCHAR(100) UNIQUE NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body_template TEXT NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'pl',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- 6. INDEXES
-- =============================================

-- Users indexes
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_user_created ON users(created_at);

-- User preferences indexes
CREATE INDEX IF NOT EXISTS idx_user_pref_user ON user_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_user_pref_key ON user_preferences(preference_key);

-- Locations indexes
CREATE INDEX IF NOT EXISTS idx_location_coordinates ON locations(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_location_type ON locations(type);
CREATE INDEX IF NOT EXISTS idx_location_city ON locations(city);

-- Meetings indexes
CREATE INDEX IF NOT EXISTS idx_meeting_organizer ON meetings(organizer_id);
CREATE INDEX IF NOT EXISTS idx_meeting_status ON meetings(status);
CREATE INDEX IF NOT EXISTS idx_meeting_start_date ON meetings(start_date);
CREATE INDEX IF NOT EXISTS idx_meeting_end_date ON meetings(end_date);
CREATE INDEX IF NOT EXISTS idx_meeting_visibility ON meetings(visibility);
CREATE INDEX IF NOT EXISTS idx_meeting_date_range ON meetings(start_date, end_date);

-- Meeting participants indexes
CREATE INDEX IF NOT EXISTS idx_participant_meeting ON meeting_participants(meeting_id);
CREATE INDEX IF NOT EXISTS idx_participant_user ON meeting_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_participant_status ON meeting_participants(status);

-- Participant status history indexes
CREATE INDEX IF NOT EXISTS idx_status_history_participant ON participant_status_history(participant_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_by ON participant_status_history(changed_by_user_id);
CREATE INDEX IF NOT EXISTS idx_status_history_changed_at ON participant_status_history(changed_at);

-- Waitlist entries indexes
CREATE INDEX IF NOT EXISTS idx_waitlist_meeting ON waitlist_entries(meeting_id);
CREATE INDEX IF NOT EXISTS idx_waitlist_user ON waitlist_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_waitlist_position ON waitlist_entries(meeting_id, position);

-- User groups indexes
CREATE INDEX IF NOT EXISTS idx_group_name ON user_groups(name);
CREATE INDEX IF NOT EXISTS idx_group_created_by ON user_groups(created_by);

-- User group members indexes
CREATE INDEX IF NOT EXISTS idx_group_member_group ON user_group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_member_user ON user_group_members(user_id);

-- Meeting resources indexes
CREATE INDEX IF NOT EXISTS idx_resource_meeting ON meeting_resources(meeting_id);
CREATE INDEX IF NOT EXISTS idx_resource_type ON meeting_resources(resource_type);

-- Meeting tasks indexes
CREATE INDEX IF NOT EXISTS idx_task_meeting ON meeting_tasks(meeting_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee ON meeting_tasks(assigned_to);
CREATE INDEX IF NOT EXISTS idx_task_status ON meeting_tasks(status);
CREATE INDEX IF NOT EXISTS idx_task_due_date ON meeting_tasks(due_date);

-- Notifications indexes
CREATE INDEX IF NOT EXISTS idx_notification_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notification_created ON notifications(created_at);

-- Audit logs indexes
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_performed_by ON audit_logs(performed_by);
CREATE INDEX IF NOT EXISTS idx_audit_performed_at ON audit_logs(performed_at);

-- Email templates indexes
CREATE INDEX IF NOT EXISTS idx_email_template_key ON email_templates(template_key);

-- =============================================
-- 7. SAMPLE DATA
-- =============================================

-- Insert sample users (passwords are bcrypt encoded "password123")
INSERT INTO users (email, password, first_name, last_name, role) VALUES
('admin@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Admin', 'User', 'ADMIN'),
('organizer@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'John', 'Organizer', 'ORGANIZER'),
('user1@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Alice', 'Participant', 'PARTICIPANT'),
('user2@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Bob', 'Attendee', 'PARTICIPANT'),
('a@a', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Bob', 'Attendee', 'PARTICIPANT')
ON CONFLICT (email) DO NOTHING;

-- Insert sample locations
INSERT INTO locations (name, address, city, country, latitude, longitude, type) VALUES
('Conference Room A', '123 Main St', 'Warsaw', 'Poland', 52.2297, 21.0122, 'PHYSICAL'),
('Virtual Meeting Room', NULL, NULL, NULL, NULL, NULL, 'VIRTUAL'),
('Tech Hub Office', '456 Tech Ave', 'Krakow', 'Poland', 50.0647, 19.9450, 'PHYSICAL')
ON CONFLICT DO NOTHING;

-- Insert sample meetings
INSERT INTO meetings (title, description, agenda, type, status, visibility, start_date, end_date, max_participants, organizer_id, location_id) VALUES
('Spring Boot Workshop', 'Learn Spring Boot with hands-on examples', '1. Introduction 2. Hands-on coding 3. Q&A', 'PHYSICAL', 'CONFIRMED', 'PUBLIC', '2024-02-15 10:00:00', '2024-02-15 12:00:00', 20, 2, 1),
('Project Planning', 'Quarterly project planning meeting', 'Review goals, assign tasks, set deadlines', 'VIRTUAL', 'PLANNED', 'INVITATION_ONLY', '2024-02-20 14:00:00', '2024-02-20 15:30:00', 10, 2, 2),
('Team Building', 'Monthly team building activity', 'Fun activities and games for team bonding', 'PHYSICAL', 'CONFIRMED', 'PUBLIC', '2024-02-25 16:00:00', '2024-02-25 18:00:00', 30, 2, 3)
ON CONFLICT DO NOTHING;

-- Insert sample meeting participants
INSERT INTO meeting_participants (meeting_id, user_id, status, permission_level) VALUES
(1, 3, 'CONFIRMED', 'PARTICIPANT'),
(1, 4, 'INVITED', 'PARTICIPANT'),
(2, 3, 'CONFIRMED', 'CONTRIBUTOR'),
(3, 4, 'CONFIRMED', 'PARTICIPANT')
ON CONFLICT DO NOTHING;

-- Insert sample participant status history
INSERT INTO participant_status_history (participant_id, old_status, new_status, comment, changed_by_user_id) VALUES
(1, NULL, 'INVITED', 'Initial invitation sent', 2),
(1, 'INVITED', 'CONFIRMED', 'User accepted invitation', 3),
(2, NULL, 'INVITED', 'Initial invitation sent', 2)
ON CONFLICT DO NOTHING;

-- Insert sample waitlist entries
INSERT INTO waitlist_entries (meeting_id, user_id, position) VALUES
(1, 4, 1)
ON CONFLICT DO NOTHING;

-- Insert sample user groups
INSERT INTO user_groups (name, description, created_by) VALUES
('Developers', 'Software development team', 2),
('Managers', 'Project management team', 2),
('Design Team', 'UI/UX design team', 3)
ON CONFLICT DO NOTHING;

-- Insert sample user group members
INSERT INTO user_group_members (group_id, user_id, role) VALUES
(1, 3, 'MEMBER'),
(1, 4, 'MEMBER'),
(2, 2, 'ADMIN'),
(3, 3, 'MEMBER')
ON CONFLICT DO NOTHING;

-- Insert sample meeting tasks
INSERT INTO meeting_tasks (meeting_id, title, description, status, priority, assigned_to, due_date, progress_percentage) VALUES
(1, 'Prepare presentation', 'Create slides for Spring Boot workshop', 'IN_PROGRESS', 'HIGH', 2, '2024-02-14 18:00:00', 75),
(1, 'Book conference room', 'Reserve Conference Room A for the workshop', 'COMPLETED', 'MEDIUM', 2, '2024-02-10 12:00:00', 100),
(2, 'Create project timeline', 'Develop detailed project timeline with milestones', 'PENDING', 'HIGH', 3, '2024-02-18 17:00:00', 0)
ON CONFLICT DO NOTHING;

-- Insert sample email templates
INSERT INTO email_templates (template_key, subject, body_template, language) VALUES
('meeting-invitation', 'Zaproszenie do spotkania: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Zostałeś zaproszony do spotkania "{{meetingTitle}}" organizowanego przez {{organizerName}}.<br><br>Data: {{meetingDate}}<br><br><a href="{{confirmationLink}}">Potwierdź udział</a>', 'pl'),
('waitlist-notification', 'Zostałeś dodany do listy oczekujących: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Zostałeś dodany do listy oczekujących na spotkanie "{{meetingTitle}}". Twoja pozycja w kolejce: {{position}}.<br><br>Powiadomimy Cię gdy miejsce się zwolni.', 'pl'),
('waitlist-promotion', 'Miejsce zwolniło się w spotkaniu: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Miejsce zwolniło się w spotkaniu "{{meetingTitle}}" i zostałeś automatycznie zapisany!<br><br>Zapraszamy do udziału.', 'pl')
ON CONFLICT DO NOTHING;

-- =============================================
-- 8. ROZSZERZENIE TABEL DLA SYSTEMU POWIADOMIEŃ
-- =============================================

-- Rozszerzenie tabeli users
ALTER TABLE meethub_schema.users
ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS push_notifications_enabled BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS sms_notifications_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS digest_enabled BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS digest_frequency VARCHAR(20) DEFAULT 'DAILY',
ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'Europe/Warsaw',
ADD COLUMN IF NOT EXISTS language VARCHAR(10) DEFAULT 'pl';

-- Tabela kanałów powiadomień użytkownika
CREATE TABLE IF NOT EXISTS meethub_schema.user_notification_channels (
    user_id BIGINT NOT NULL REFERENCES meethub_schema.users(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, channel)
);

-- Rozszerzenie tabeli notifications
ALTER TABLE meethub_schema.notifications
ADD COLUMN IF NOT EXISTS template_key VARCHAR(100),
ADD COLUMN IF NOT EXISTS scheduled_for TIMESTAMP,
ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS error_message TEXT;

-- Tabela zmiennych szablonów powiadomień
CREATE TABLE IF NOT EXISTS meethub_schema.notification_variables (
    notification_id BIGINT NOT NULL REFERENCES meethub_schema.notifications(id) ON DELETE CASCADE,
    variable_key VARCHAR(100) NOT NULL,
    variable_value TEXT,
    PRIMARY KEY (notification_id, variable_key)
);

-- Tabela harmonogramów powiadomień
CREATE TABLE IF NOT EXISTS meethub_schema.notification_schedules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES meethub_schema.users(id) ON DELETE CASCADE,
    schedule_type VARCHAR(50) NOT NULL,
    trigger_time VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    custom_settings TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indeksy dla wydajności
CREATE INDEX IF NOT EXISTS idx_notifications_scheduled ON meethub_schema.notifications(scheduled_for, status);
CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON meethub_schema.notifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON meethub_schema.notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_schedules_user ON meethub_schema.notification_schedules(user_id);

-- Wstaw domyślne kanały dla istniejących użytkowników
INSERT INTO meethub_schema.user_notification_channels (user_id, channel)
SELECT id, 'EMAIL' FROM meethub_schema.users
WHERE email_notifications_enabled = true
ON CONFLICT (user_id, channel) DO NOTHING;

INSERT INTO meethub_schema.user_notification_channels (user_id, channel)
SELECT id, 'PUSH' FROM meethub_schema.users
WHERE push_notifications_enabled = true
ON CONFLICT (user_id, channel) DO NOTHING;

INSERT INTO meethub_schema.user_notification_channels (user_id, channel)
SELECT id, 'SMS' FROM meethub_schema.users
WHERE sms_notifications_enabled = true
ON CONFLICT (user_id, channel) DO NOTHING;

-- =============================================
-- 9. PERMISSIONS
-- =============================================

-- Grant permissions to application user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA meethub_schema TO meethub_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA meethub_schema TO meethub_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA meethub_schema TO meethub_app;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA meethub_schema TO meethub_app;

-- =============================================
-- 10. UPDATE SEQUENCES (ensure proper ID generation)
-- =============================================
SELECT setval('meethub_schema.users_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.users), 1));
SELECT setval('meethub_schema.user_preferences_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.user_preferences), 1));
SELECT setval('meethub_schema.locations_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.locations), 1));
SELECT setval('meethub_schema.meetings_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.meetings), 1));
SELECT setval('meethub_schema.meeting_participants_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.meeting_participants), 1));
SELECT setval('meethub_schema.participant_status_history_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.participant_status_history), 1));
SELECT setval('meethub_schema.waitlist_entries_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.waitlist_entries), 1));
SELECT setval('meethub_schema.user_groups_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.user_groups), 1));
SELECT setval('meethub_schema.user_group_members_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.user_group_members), 1));
SELECT setval('meethub_schema.meeting_resources_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.meeting_resources), 1));
SELECT setval('meethub_schema.meeting_tasks_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.meeting_tasks), 1));
SELECT setval('meethub_schema.notifications_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.notifications), 1));
SELECT setval('meethub_schema.audit_logs_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.audit_logs), 1));
SELECT setval('meethub_schema.email_templates_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.email_templates), 1));
SELECT setval('meethub_schema.notification_schedules_id_seq', COALESCE((SELECT MAX(id) FROM meethub_schema.notification_schedules), 1));

-- Set default schema for user
ALTER USER meethub_user SET search_path TO meethub_schema, public;



-- Dodaj brakujące kolumny do tabeli meeting_resources
ALTER TABLE meethub_schema.meeting_resources
ADD COLUMN IF NOT EXISTS download_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS description TEXT;

-- Dodaj indeksy dla lepszej wydajności
CREATE INDEX IF NOT EXISTS idx_resource_uploaded_by ON meethub_schema.meeting_resources(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_resource_current ON meethub_schema.meeting_resources(is_current);
CREATE INDEX IF NOT EXISTS idx_resource_uploaded_at ON meethub_schema.meeting_resources(uploaded_at DESC);

-- Dodaj WSZYSTKIE brakujące kolumny do email_templates
ALTER TABLE meethub_schema.email_templates
ADD COLUMN IF NOT EXISTS name VARCHAR(200),
ADD COLUMN IF NOT EXISTS category VARCHAR(100),
ADD COLUMN IF NOT EXISTS description TEXT,
ADD COLUMN IF NOT EXISTS variables_help TEXT,
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1,
ADD COLUMN IF NOT EXISTS channel VARCHAR(50),
ADD COLUMN IF NOT EXISTS available_variables TEXT;

-- Ustaw domyślne wartości
UPDATE meethub_schema.email_templates SET
    name = template_key,
    category = 'NOTIFICATION',
    is_active = TRUE,
    version = 1,
    channel = 'EMAIL'
WHERE name IS NULL OR category IS NULL OR is_active IS NULL;



-- =============================================
-- TABELE SYSTEMU GŁOSOWANIA
-- =============================================

-- Tabela głosowań
CREATE TABLE IF NOT EXISTS meeting_votings (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_choices INTEGER,
    allow_suggestions BOOLEAN DEFAULT FALSE,
    deadline_date TIMESTAMP,
    auto_close BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabela opcji głosowania
CREATE TABLE IF NOT EXISTS voting_options (
    id BIGSERIAL PRIMARY KEY,
    voting_id BIGINT NOT NULL REFERENCES meeting_votings(id) ON DELETE CASCADE,
    option_date TIMESTAMP NOT NULL,
    option_duration_minutes INTEGER,
    is_suggested BOOLEAN DEFAULT FALSE,
    suggested_by BIGINT REFERENCES users(id)
);

-- Tabela głosów
CREATE TABLE IF NOT EXISTS votes (
    id BIGSERIAL PRIMARY KEY,
    voting_id BIGINT NOT NULL REFERENCES meeting_votings(id) ON DELETE CASCADE,
    option_id BIGINT NOT NULL REFERENCES voting_options(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vote_weight INTEGER DEFAULT 1,
    preference_order INTEGER,
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- INDEKSY DLA WYDajNOŚCI
-- =============================================

-- Indeksy dla meeting_votings
CREATE INDEX IF NOT EXISTS idx_voting_meeting ON meeting_votings(meeting_id);
CREATE INDEX IF NOT EXISTS idx_voting_status ON meeting_votings(status);
CREATE INDEX IF NOT EXISTS idx_voting_deadline ON meeting_votings(deadline_date);

-- Indeksy dla voting_options
CREATE INDEX IF NOT EXISTS idx_option_voting ON voting_options(voting_id);
CREATE INDEX IF NOT EXISTS idx_option_date ON voting_options(option_date);

-- Indeksy dla votes
CREATE INDEX IF NOT EXISTS idx_vote_voting ON votes(voting_id);
CREATE INDEX IF NOT EXISTS idx_vote_option ON votes(option_id);
CREATE INDEX IF NOT EXISTS idx_vote_user ON votes(user_id);
CREATE INDEX IF NOT EXISTS idx_vote_user_voting ON votes(user_id, voting_id);
CREATE INDEX IF NOT EXISTS idx_vote_preference ON votes(preference_order);

-- Unikalne ograniczenia
ALTER TABLE votes ADD CONSTRAINT unique_user_voting_option UNIQUE (user_id, voting_id, option_id);