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



-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'TODO',
    deadline TIMESTAMP,
    meeting_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Task assignments table
CREATE TABLE IF NOT EXISTS task_assignments (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(50) NOT NULL DEFAULT 'ASSIGNED',
    comment TEXT,
    completed_at TIMESTAMP,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(task_id, user_id)
);

-- Task files table
CREATE TABLE IF NOT EXISTS task_files (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assignment_id BIGINT NOT NULL,
    FOREIGN KEY (assignment_id) REFERENCES task_assignments(id) ON DELETE CASCADE
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_tasks_meeting_id ON tasks(meeting_id);
CREATE INDEX IF NOT EXISTS idx_tasks_created_by ON tasks(created_by);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_assignments_task_id ON task_assignments(task_id);
CREATE INDEX IF NOT EXISTS idx_assignments_user_id ON task_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_assignments_status ON task_assignments(status);
CREATE INDEX IF NOT EXISTS idx_files_assignment_id ON task_files(assignment_id);

ALTER TABLE tasks ADD COLUMN allow_self_assignment BOOLEAN;
ALTER TABLE tasks ADD COLUMN allowed_file_types TEXT DEFAULT 'pdf,doc,docx,jpg,png';








-- Tabela feedbacks
CREATE TABLE feedbacks (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(meeting_id, user_id)
);

CREATE INDEX idx_feedbacks_meeting ON feedbacks(meeting_id);
CREATE INDEX idx_feedbacks_user ON feedbacks(user_id);

-- Tabela meeting_statistics
--CREATE TABLE meeting_statistics (
--    id BIGSERIAL PRIMARY KEY,
--    meeting_id BIGINT NOT NULL UNIQUE REFERENCES meetings(id) ON DELETE CASCADE,
--    total_participants INTEGER DEFAULT 0,
--    confirmed_participants INTEGER DEFAULT 0,
--    attended_participants INTEGER DEFAULT 0,
--    attendance_rate DECIMAL(5,2) DEFAULT 0.00,
--    confirmation_rate DECIMAL(5,2) DEFAULT 0.00,
--    avg_response_time_hours DECIMAL(8,2) DEFAULT 0.00,
--    no_show_count INTEGER DEFAULT 0,
--    engagement_score DECIMAL(5,2) DEFAULT 0.00,
--    task_completion_rate DECIMAL(5,2) DEFAULT 0.00,
--    feedback_count INTEGER DEFAULT 0,
--    avg_feedback_rating DECIMAL(3,2) DEFAULT 0.00,
--    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--);
--
--CREATE INDEX idx_meeting_stats_meeting ON meeting_statistics(meeting_id);

-- Migration: Create meeting_statistics table
CREATE TABLE meeting_statistics (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL UNIQUE,

    -- Podstawowe metryki
    total_participants INTEGER NOT NULL DEFAULT 0,
    attended_participants INTEGER NOT NULL DEFAULT 0,
    confirmed_participants INTEGER DEFAULT 0,
    declined_participants INTEGER DEFAULT 0,
    pending_participants INTEGER DEFAULT 0,

    -- Frekwencja
    attendance_rate DECIMAL(5,2),
    confirmation_rate DECIMAL(5,2),

    -- Czasy odpowiedzi
    avg_response_time_minutes DECIMAL(10,2),
    min_response_time_minutes DECIMAL(10,2),
    max_response_time_minutes DECIMAL(10,2),

    -- Czas trwania
    avg_join_delay_minutes DECIMAL(10,2),
    avg_participation_duration_minutes DECIMAL(10,2),
    total_meeting_duration_minutes INTEGER,

    -- Statystyki uczestnictwa
    max_concurrent_participants INTEGER,
    current_participants INTEGER DEFAULT 0,
    peak_participants_today INTEGER DEFAULT 0,

    -- Feedback
    average_rating DECIMAL(3,2),
    feedback_count INTEGER DEFAULT 0,
    positive_feedback_count INTEGER DEFAULT 0,
    negative_feedback_count INTEGER DEFAULT 0,

    -- Koszty
    total_cost DECIMAL(15,2),
    cost_per_participant DECIMAL(10,2),

    -- Statusy
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_finalized BOOLEAN NOT NULL DEFAULT FALSE,
    data_quality_score DECIMAL(3,2),

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    generated_at TIMESTAMP NOT NULL,
    last_calculated_at TIMESTAMP,
    valid_until TIMESTAMP,

    -- JSON metryki
    additional_metrics JSONB,

    -- Wersja
    version INTEGER DEFAULT 0,

    -- Klucze obce
    CONSTRAINT fk_meeting_statistics_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(id) ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_attendance_rate_range
        CHECK (attendance_rate >= 0 AND attendance_rate <= 100),
    CONSTRAINT chk_confirmation_rate_range
        CHECK (confirmation_rate >= 0 AND confirmation_rate <= 100),
    CONSTRAINT chk_average_rating_range
        CHECK (average_rating >= 0 AND average_rating <= 5)
);

-- Indeksy dla wydajności
CREATE INDEX idx_meeting_statistics_meeting_id ON meeting_statistics(meeting_id);
CREATE INDEX idx_meeting_statistics_status ON meeting_statistics(status);
CREATE INDEX idx_meeting_statistics_generated_at ON meeting_statistics(generated_at DESC);
CREATE INDEX idx_meeting_statistics_organizer ON meeting_statistics(meeting_id)
    INCLUDE (attendance_rate, total_participants, generated_at);

-- Trigger do aktualizacji updated_at
CREATE OR REPLACE FUNCTION update_meeting_statistics_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_meeting_statistics_updated_at
BEFORE UPDATE ON meeting_statistics
FOR EACH ROW
EXECUTE FUNCTION update_meeting_statistics_updated_at();


ALTER TABLE feedbacks ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();


CREATE TABLE meeting_predictions (
    id BIGSERIAL PRIMARY KEY,
    organizer_id BIGINT NOT NULL,
    prediction_date DATE NOT NULL,
    predicted_meetings INTEGER,
    predicted_participants INTEGER,
    predicted_attendance_rate DOUBLE PRECISION,
    best_day VARCHAR(255),
    best_time VARCHAR(255),
    confidence_score DOUBLE PRECISION,
    factors_json TEXT,
    CONSTRAINT fk_meeting_prediction_organizer
        FOREIGN KEY (organizer_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Sprawdź czy tabela istnieje
SELECT EXISTS (
    SELECT FROM information_schema.tables
    WHERE table_schema = 'meethub_schema'
    AND table_name = 'meeting_statistics'
);

-- Jeśli tabela nie istnieje, utwórz ją
CREATE TABLE IF NOT EXISTS meeting_statistics (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_at TIMESTAMP,
    updated_at TIMESTAMP,
    total_participants INTEGER DEFAULT 0,
    confirmed_participants INTEGER DEFAULT 0,
    attended_participants INTEGER DEFAULT 0,
    attendance_rate DOUBLE PRECISION DEFAULT 0.0,
    confirmation_rate DOUBLE PRECISION DEFAULT 0.0,
    no_show_count INTEGER DEFAULT 0,
    feedback_count INTEGER DEFAULT 0,
    avg_feedback_rating DOUBLE PRECISION DEFAULT 0.0,
    engagement_score DOUBLE PRECISION DEFAULT 0.0,
    task_completion_rate DOUBLE PRECISION DEFAULT 0.0,
    avg_response_time_hours DOUBLE PRECISION DEFAULT 0.0,
    CONSTRAINT fk_meeting_statistics_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(id)
        ON DELETE CASCADE
);

-- Dodaj indeksy
CREATE INDEX IF NOT EXISTS idx_meeting_statistics_meeting
    ON meeting_statistics(meeting_id);
CREATE INDEX IF NOT EXISTS idx_meeting_statistics_created
    ON meeting_statistics(created_at);

ALTER TABLE meeting_statistics
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;



INSERT INTO meethub_schema.email_templates (
    template_key,
    subject,
    body_template,
    language,
    name,
    category,
    description,
    variables_help,
    is_active,
    version,
    channel,
    available_variables
) VALUES (
    'participant_joined',
    'Nowy uczestnik w spotkaniu',
    'Użytkownik {{participantName}} dołączył do spotkania "{{meetingTitle}}" dnia {{meetingDate}}.
Aktualna liczba uczestników: {{currentParticipants}}.',
    'pl',
    'Uczestnik dołączył',
    'MEETING',
    'Powiadomienie o nowym uczestniku',
    '{{participantName}}, {{meetingTitle}}, {{meetingDate}}, {{currentParticipants}}',
    true,
    1,
    'EMAIL',
    '{{participantName}},{{meetingTitle}},{{meetingDate}},{{currentParticipants}}'
);


INSERT INTO meethub_schema.email_templates (
    template_key, subject, body_template, language,
    name, category, description, variables_help,
    is_active, version, channel, available_variables
) VALUES (
    'join_request',
    'Nowa prośba o dołączenie do spotkania',
    'Użytkownik {{requesterName}} ({{requesterEmail}}) wysłał prośbę o dołączenie do spotkania "{{meetingTitle}}" zaplanowanego na {{meetingDate}}.',
    'pl',
    'Prośba o dołączenie',
    'MEETING',
    'Powiadomienie o nowej prośbie o dołączenie',
    '{{requesterName}}, {{requesterEmail}}, {{meetingTitle}}, {{meetingDate}}',
    true, 1, 'EMAIL',
    '{{requesterName}},{{requesterEmail}},{{meetingTitle}},{{meetingDate}}'
);


INSERT INTO meethub_schema.email_templates (
    template_key, subject, body_template, language,
    name, category, description, variables_help,
    is_active, version, channel, available_variables
) VALUES (
    'request_approved',
    'Twoja prośba została zaakceptowana',
    'Cześć {{userName}}, Twoja prośba o dołączenie do spotkania "{{meetingTitle}}" w dniu {{meetingDate}} została zaakceptowana przez {{organizerName}}.
Lokalizacja: {{meetingLocation}}.',
    'pl',
    'Prośba zaakceptowana',
    'MEETING',
    'Powiadomienie o zaakceptowaniu prośby',
    '{{userName}}, {{meetingTitle}}, {{meetingDate}}, {{organizerName}}, {{meetingLocation}}',
    true, 1, 'EMAIL',
    '{{userName}},{{meetingTitle}},{{meetingDate}},{{organizerName}},{{meetingLocation}}'
);


INSERT INTO meethub_schema.email_templates (
    template_key, subject, body_template, language,
    name, category, description, variables_help,
    is_active, version, channel, available_variables
) VALUES (
    'request_rejected',
    'Twoja prośba została odrzucona',
    'Cześć {{userName}}, Twoja prośba o dołączenie do spotkania "{{meetingTitle}}" w dniu {{meetingDate}} została odrzucona przez {{organizerName}}.',
    'pl',
    'Prośba odrzucona',
    'MEETING',
    'Powiadomienie o odrzuceniu prośby',
    '{{userName}}, {{meetingTitle}}, {{meetingDate}}, {{organizerName}}',
    true, 1, 'EMAIL',
    '{{userName}},{{meetingTitle}},{{meetingDate}},{{organizerName}}'
);



-- ✅ DODAJ TE KOLUMNY DO TABELI meetings
ALTER TABLE meethub_schema.meetings
ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS recurrence_pattern VARCHAR(100),
ADD COLUMN IF NOT EXISTS recurrence_end_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS recurrence_exceptions_json TEXT,
ADD COLUMN IF NOT EXISTS is_template BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS original_meeting_id BIGINT,
ADD COLUMN IF NOT EXISTS workflow_stage VARCHAR(50) DEFAULT 'DRAFT';

-- ✅ DODAJ REFERENCJĘ DO ORYGINALNEGO SPOTKANIA
ALTER TABLE meethub_schema.meetings
ADD CONSTRAINT fk_meeting_original
    FOREIGN KEY (original_meeting_id)
    REFERENCES meethub_schema.meetings(id)
    ON DELETE SET NULL;

-- ✅ DODAJ INDEKSY DLA WYDAJNOŚCI
CREATE INDEX IF NOT EXISTS idx_meetings_recurring
    ON meethub_schema.meetings(is_recurring, recurrence_end_date);

CREATE INDEX IF NOT EXISTS idx_meetings_template
    ON meethub_schema.meetings(is_template, organizer_id);

CREATE INDEX IF NOT EXISTS idx_meetings_original
    ON meethub_schema.meetings(original_meeting_id);



-- ✅ STWÓRZ TABELĘ CATEGORIES
CREATE TABLE IF NOT EXISTS meethub_schema.categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color_code VARCHAR(7) DEFAULT '#3498db',
    created_by BIGINT NOT NULL REFERENCES meethub_schema.users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ✅ STWÓRZ TABELĘ MANY-TO-MANY meetings_categories
CREATE TABLE IF NOT EXISTS meethub_schema.meeting_categories (
    meeting_id BIGINT NOT NULL REFERENCES meethub_schema.meetings(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES meethub_schema.categories(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (meeting_id, category_id)
);

-- ✅ DODAJ INDEKSY DLA CATEGORIES
CREATE INDEX IF NOT EXISTS idx_categories_created_by
    ON meethub_schema.categories(created_by);

CREATE INDEX IF NOT EXISTS idx_categories_name
    ON meethub_schema.categories(name);

CREATE INDEX IF NOT EXISTS idx_meeting_categories_meeting
    ON meethub_schema.meeting_categories(meeting_id);

CREATE INDEX IF NOT EXISTS idx_meeting_categories_category
    ON meethub_schema.meeting_categories(category_id);


-- ✅ STWÓRZ TABELĘ HISTORII ZMIAN STATUSU
CREATE TABLE IF NOT EXISTS meethub_schema.meeting_status_changes (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meethub_schema.meetings(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by_user_id BIGINT REFERENCES meethub_schema.users(id),
    reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ✅ DODAJ INDEKSY DLA STATUS CHANGES
CREATE INDEX IF NOT EXISTS idx_status_changes_meeting
    ON meethub_schema.meeting_status_changes(meeting_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_status_changes_changed_by
    ON meethub_schema.meeting_status_changes(changed_by_user_id);

CREATE INDEX IF NOT EXISTS idx_status_changes_changed_at
    ON meethub_schema.meeting_status_changes(changed_at);


-- ✅ UPEWNIJ SIĘ ŻE meeting_tags MA ODPOWIEDNIE OGRANICZENIA
ALTER TABLE meethub_schema.meeting_tags
ADD COLUMN IF NOT EXISTS added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS added_by BIGINT REFERENCES meethub_schema.users(id);

-- ✅ DODAJ INDEKS DLA TAGÓW
CREATE INDEX IF NOT EXISTS idx_meeting_tags_tag
    ON meethub_schema.meeting_tags(tag);


-- ✅ DODAJ METADATA JAKO JSON (opcjonalnie)
ALTER TABLE meethub_schema.meetings
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- ✅ DODAJ INDEKS DLA METADATA
CREATE INDEX IF NOT EXISTS idx_meetings_metadata
    ON meethub_schema.meetings USING GIN (metadata);


-- ✅ ROZSZERZ meeting_participants O DODATKOWE POLA
ALTER TABLE meethub_schema.meeting_participants
ADD COLUMN IF NOT EXISTS join_method VARCHAR(50), -- 'INVITED', 'REQUESTED', 'DIRECT'
ADD COLUMN IF NOT EXISTS rating INTEGER CHECK (rating >= 1 AND rating <= 5),
ADD COLUMN IF NOT EXISTS feedback TEXT,
ADD COLUMN IF NOT EXISTS attendance_duration_minutes INTEGER;

-- ✅ DODAJ DODATKOWE INDEKSY
CREATE INDEX IF NOT EXISTS idx_participants_join_method
    ON meethub_schema.meeting_participants(join_method);

CREATE INDEX IF NOT EXISTS idx_participants_response_date
    ON meethub_schema.meeting_participants(response_date);


    -- ✅ STWÓRZ TABELĘ DLA KONKRETNYCH WYSTĄPIEŃ POWTARZAJĄCYCH SIĘ SPOTKAŃ
    CREATE TABLE IF NOT EXISTS meethub_schema.recurrence_occurrences (
        id BIGSERIAL PRIMARY KEY,
        meeting_series_id BIGINT NOT NULL REFERENCES meethub_schema.meetings(id) ON DELETE CASCADE,
        occurrence_date TIMESTAMP NOT NULL,
        status VARCHAR(20) DEFAULT 'SCHEDULED',
        is_exception BOOLEAN DEFAULT FALSE,
        modified_start_date TIMESTAMP,
        modified_end_date TIMESTAMP,
        cancelled BOOLEAN DEFAULT FALSE,
        notes TEXT,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

    -- ✅ DODAJ INDEKSY
    CREATE INDEX IF NOT EXISTS idx_occurrences_series
        ON meethub_schema.recurrence_occurrences(meeting_series_id);

    CREATE INDEX IF NOT EXISTS idx_occurrences_date
        ON meethub_schema.recurrence_occurrences(occurrence_date);

    CREATE INDEX IF NOT EXISTS idx_occurrences_status
        ON meethub_schema.recurrence_occurrences(status);



-- ✅ ZAKTUALIZUJ SEKWENCJE DLA NOWYCH TABEL
SELECT setval('meethub_schema.categories_id_seq',
    COALESCE((SELECT MAX(id) FROM meethub_schema.categories), 1));

SELECT setval('meethub_schema.meeting_status_changes_id_seq',
    COALESCE((SELECT MAX(id) FROM meethub_schema.meeting_status_changes), 1));

SELECT setval('meethub_schema.recurrence_occurrences_id_seq',
    COALESCE((SELECT MAX(id) FROM meethub_schema.recurrence_occurrences), 1));

-- ✅ DODAJ PRZYKŁADOWE KATEGORIE
INSERT INTO meethub_schema.categories (name, description, color_code, created_by) VALUES
('Szkolenia', 'Spotkania szkoleniowe i warsztaty', '#3498db', 2),
('Spotkania biznesowe', 'Spotkania związane z biznesem', '#2ecc71', 2),
('Społecznościowe', 'Spotkania społeczności i networking', '#e74c3c', 2),
('Techniczne', 'Spotkania techniczne i programistyczne', '#f39c12', 2),
('Planowanie', 'Spotkania planistyczne i strategiczne', '#9b59b6', 2)
ON CONFLICT DO NOTHING;

-- ✅ STWÓRZ WIDOK DLA POWTARZAJĄCYCH SIĘ SPOTKAŃ
CREATE OR REPLACE VIEW meethub_schema.recurring_meetings_view AS
SELECT
    m.id,
    m.title,
    m.start_date,
    m.recurrence_pattern,
    m.recurrence_end_date,
    m.is_template,
    u.email as organizer_email,
    COUNT(DISTINCT mp.id) as participant_count
FROM meethub_schema.meetings m
JOIN meethub_schema.users u ON m.organizer_id = u.id
LEFT JOIN meethub_schema.meeting_participants mp ON m.id = mp.meeting_id AND mp.status = 'CONFIRMED'
WHERE m.is_recurring = TRUE
GROUP BY m.id, m.title, m.start_date, m.recurrence_pattern, m.recurrence_end_date, m.is_template, u.email;

-- ✅ STWÓRZ WIDOK DLA SZABLONÓW
CREATE OR REPLACE VIEW meethub_schema.meeting_templates_view AS
SELECT
    m.id,
    m.title,
    m.description,
    m.type,
    m.visibility,
    u.email as created_by_email,
    COUNT(DISTINCT mc.category_id) as category_count,
    ARRAY_AGG(DISTINCT mt.tag) as tags
FROM meethub_schema.meetings m
JOIN meethub_schema.users u ON m.organizer_id = u.id
LEFT JOIN meethub_schema.meeting_categories mc ON m.id = mc.meeting_id
LEFT JOIN meethub_schema.meeting_tags mt ON m.id = mt.meeting_id
WHERE m.is_template = TRUE
GROUP BY m.id, m.title, m.description, m.type, m.visibility, u.email;



-- ✅ FUNKCJA DO GENEROWANIA NASTĘPNYCH WYSTĄPIEŃ
CREATE OR REPLACE FUNCTION meethub_schema.generate_next_occurrences(
    meeting_id BIGINT,
    count INTEGER DEFAULT 5
)
RETURNS TABLE(
    occurrence_date TIMESTAMP,
    status VARCHAR
) AS $$
DECLARE
    rec RECORD;
    current_date TIMESTAMP;
    pattern_parts TEXT[];
    frequency TEXT;
    interval_val INTEGER;
    i INTEGER := 0;
BEGIN
    -- Pobierz dane spotkania
    SELECT
        m.start_date,
        m.recurrence_pattern,
        m.recurrence_end_date,
        m.recurrence_exceptions_json
    INTO rec
    FROM meethub_schema.meetings m
    WHERE m.id = meeting_id AND m.is_recurring = TRUE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Meeting not found or not recurring';
    END IF;

    current_date := rec.start_date;
    pattern_parts := string_to_array(rec.recurrence_pattern, ':');
    frequency := pattern_parts[1];

    IF array_length(pattern_parts, 1) > 1 THEN
        interval_val := pattern_parts[2]::INTEGER;
    ELSE
        interval_val := 1;
    END IF;

    -- Generuj daty
    WHILE i < count LOOP
        CASE frequency
            WHEN 'DAILY' THEN
                current_date := current_date + (interval_val || ' days')::INTERVAL;
            WHEN 'WEEKLY' THEN
                current_date := current_date + (interval_val || ' weeks')::INTERVAL;
            WHEN 'MONTHLY' THEN
                current_date := current_date + (interval_val || ' months')::INTERVAL;
            WHEN 'YEARLY' THEN
                current_date := current_date + (interval_val || ' years')::INTERVAL;
            ELSE
                current_date := current_date + '1 day'::INTERVAL;
        END CASE;

        -- Sprawdź czy nie przekroczono końca powtarzania
        IF rec.recurrence_end_date IS NOT NULL AND current_date > rec.recurrence_end_date THEN
            EXIT;
        END IF;

        occurrence_date := current_date;
        status := 'SCHEDULED';
        RETURN NEXT;

        i := i + 1;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ✅ FUNKCJA DO POBRANIA CAŁEJ SERII SPOTKAŃ
CREATE OR REPLACE FUNCTION meethub_schema.get_meeting_series(original_id BIGINT)
RETURNS TABLE(
    meeting_id BIGINT,
    title VARCHAR,
    start_date TIMESTAMP,
    status VARCHAR,
    is_template BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        m.id as meeting_id,
        m.title,
        m.start_date,
        m.status,
        m.is_template
    FROM meethub_schema.meetings m
    WHERE m.original_meeting_id = original_id
       OR m.id = original_id
    ORDER BY m.start_date;
END;
$$ LANGUAGE plpgsql;




-- ✅ UDOSTĘPNIJ UPRAWNIENIA DLA NOWYCH TABEL
GRANT ALL PRIVILEGES ON meethub_schema.categories TO meethub_user, meethub_app;
GRANT ALL PRIVILEGES ON meethub_schema.meeting_categories TO meethub_user, meethub_app;
GRANT ALL PRIVILEGES ON meethub_schema.meeting_status_changes TO meethub_user, meethub_app;
GRANT ALL PRIVILEGES ON meethub_schema.recurrence_occurrences TO meethub_user, meethub_app;

GRANT USAGE ON SEQUENCE meethub_schema.categories_id_seq TO meethub_user, meethub_app;
GRANT USAGE ON SEQUENCE meethub_schema.meeting_status_changes_id_seq TO meethub_user, meethub_app;
GRANT USAGE ON SEQUENCE meethub_schema.recurrence_occurrences_id_seq TO meethub_user, meethub_app;

-- ✅ UDOSTĘPNIJ WIDOKI
GRANT SELECT ON meethub_schema.recurring_meetings_view TO meethub_user, meethub_app;
GRANT SELECT ON meethub_schema.meeting_templates_view TO meethub_user, meethub_app;

-- ✅ UDOSTĘPNIJ FUNKCJE
GRANT EXECUTE ON FUNCTION meethub_schema.generate_next_occurrences TO meethub_user, meethub_app;
GRANT EXECUTE ON FUNCTION meethub_schema.get_meeting_series TO meethub_user, meethub_app;

-- ✅ TRIGGER DLA meetings
CREATE OR REPLACE FUNCTION meethub_schema.update_meetings_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_meetings_updated_at
BEFORE UPDATE ON meethub_schema.meetings
FOR EACH ROW
EXECUTE FUNCTION meethub_schema.update_meetings_updated_at();

-- ✅ TRIGGER DLA categories
CREATE OR REPLACE FUNCTION meethub_schema.update_categories_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_categories_updated_at
BEFORE UPDATE ON meethub_schema.categories
FOR EACH ROW
EXECUTE FUNCTION meethub_schema.update_categories_updated_at();


-- ✅ INDEKSY PEŁNOTEKSTOWE DLA LEPSZEGO WYSZUKIWANIA
CREATE INDEX IF NOT EXISTS idx_meetings_search_title
    ON meethub_schema.meetings USING GIN (to_tsvector('english', title));

CREATE INDEX IF NOT EXISTS idx_meetings_search_description
    ON meethub_schema.meetings USING GIN (to_tsvector('english', description));

CREATE INDEX IF NOT EXISTS idx_categories_search
    ON meethub_schema.categories USING GIN (to_tsvector('english', name || ' ' || description));


--troche poprawic
ALTER TABLE meethub_schema.meetings
ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS recurrence_pattern VARCHAR(100),
ADD COLUMN IF NOT EXISTS recurrence_end_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS recurrence_exceptions JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS is_template BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS original_meeting_id BIGINT,
ADD COLUMN IF NOT EXISTS workflow_stage VARCHAR(50) DEFAULT 'DRAFT',
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;


ALTER TABLE meetings
ALTER COLUMN recurrence_exceptions TYPE TEXT
USING recurrence_exceptions::text;





-- Wykonaj te komendy w PostgreSQL
INSERT INTO email_templates (template_key, language, subject, body_template, created_at, updated_at)
VALUES
-- Szablon dla organizatora gdy spotkanie się rozpoczęło
('meeting_started', 'pl',
 '🎉 Spotkanie {{meetingTitle}} się rozpoczęło!',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Spotkanie się rozpoczęło</title>
</head>
<body>
    <h1>🎉 Spotkanie się rozpoczęło!</h1>
    <p>Cześć {{userName}}!</p>
    <p>Twoje spotkanie <strong>{{meetingTitle}}</strong> właśnie się rozpoczęło.</p>
    <p><strong>Czas:</strong> {{meetingTime}}</p>
    {{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}
    {{#meetingLink}}<p><strong>Link do spotkania:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}
    <br>
    <p>Pozdrawiamy,<br>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW()),

-- Szablon przypomnienia o spotkaniu
('meeting_reminder', 'pl',
 '🔔 Przypomnienie: Spotkanie {{meetingTitle}} za {{minutesBefore}} minut',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Przypomnienie o spotkaniu</title>
</head>
<body>
    <h1>🔔 Przypomnienie o spotkaniu</h1>
    <p>Cześć {{userName}}!</p>
    <p>Przypominamy o spotkaniu <strong>{{meetingTitle}}</strong>.</p>
    <p><strong>Rozpoczyna się za:</strong> {{minutesBefore}} minut</p>
    <p><strong>Godzina:</strong> {{meetingTime}}</p>
    {{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}
    {{#meetingLink}}<p><strong>Link:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}
    <br>
    <p>Pozdrawiamy,<br>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW()),

-- Szablon dla uczestnika gdy spotkanie się rozpoczęło
('meeting_started_participant', 'pl',
 '🎉 Spotkanie {{meetingTitle}} się rozpoczęło',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Spotkanie się rozpoczęło</title>
</head>
<body>
    <h1>🎉 Spotkanie się rozpoczęło!</h1>
    <p>Cześć {{userName}}!</p>
    <p>Spotkanie <strong>{{meetingTitle}}</strong> właśnie się rozpoczęło.</p>
    <p><strong>Organizator:</strong> {{organizerName}}</p>
    <p><strong>Czas:</strong> {{meetingTime}}</p>
    {{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}
    {{#meetingLink}}<p><strong>Link do spotkania:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}
    <br>
    <p>Do zobaczenia na spotkaniu!<br>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW()),

-- Szablon przypomnienia dla uczestnika
('meeting_reminder_participant', 'pl',
 '🔔 Przypomnienie: Spotkanie {{meetingTitle}} za {{minutesBefore}} minut',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Przypomnienie o spotkaniu</title>
</head>
<body>
    <h1>🔔 Przypomnienie o spotkaniu</h1>
    <p>Cześć {{userName}}!</p>
    <p>Przypominamy o spotkaniu <strong>{{meetingTitle}}</strong>.</p>
    <p><strong>Rozpoczyna się za:</strong> {{minutesBefore}} minut</p>
    <p><strong>Godzina:</strong> {{meetingTime}}</p>
    <p><strong>Organizator:</strong> {{organizerName}}</p>
    {{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}
    {{#meetingLink}}<p><strong>Link:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}
    <br>
    <p>Do zobaczenia!<br>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW()),

-- Szablon gdy spotkanie nie zostało rozpoczęte
('meeting_not_started', 'pl',
 '⚠️ Spotkanie {{meetingTitle}} nie zostało rozpoczęte',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Spotkanie nie rozpoczęte</title>
</head>
<body>
    <h1>⚠️ Uwaga!</h1>
    <p>Cześć {{organizerName}},</p>
    <p>Twoje spotkanie <strong>{{meetingTitle}}</strong> miało rozpocząć się o {{scheduledTime}},</p>
    <p>ale <strong>nie zostało jeszcze oznaczone jako rozpoczęte</strong>.</p>
    <p><strong>Opóźnienie:</strong> {{minutesLate}} minut</p>
    <br>
    <p>Proszę sprawdzić:</p>
    <ul>
        <li>Czy spotkanie faktycznie się odbywa?</li>
        <li>Czy trzeba je anulować?</li>
        <li>Czy uczestnicy są poinformowani?</li>
    </ul>
    <br>
    <p>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW()),

-- Szablon testowy
('test_email', 'pl',
 '📧 Testowy email z MeetHub',
 '<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Test Email</title>
</head>
<body>
    <h1>✅ Test systemu powiadomień</h1>
    <p>Cześć {{userName}}!</p>
    <p>To jest testowy email wysłany o {{testTime}}.</p>
    <p>Jeśli go otrzymujesz, oznacza to że system powiadomień działa poprawnie! 🎉</p>
    <br>
    <p>Pozdrawiamy,<br>Zespół MeetHub</p>
</body>
</html>',
 NOW(), NOW());



 -- Zaktualizuj szablon meeting_started na prostszy
 UPDATE meethub_schema.email_templates
 SET body_template = '<!DOCTYPE html>
 <html>
 <head>
     <meta charset="UTF-8">
     <title>Spotkanie się rozpoczęło</title>
 </head>
 <body>
     <h1>🎉 Spotkanie się rozpoczęło!</h1>
     <p>Cześć {{userName}}!</p>
     <p>Twoje spotkanie <strong>{{meetingTitle}}</strong> właśnie się rozpoczęło.</p>
     <p><strong>Czas:</strong> {{meetingDate}}</p>
     <p>Dołącz do spotkania w aplikacji MeetHub.</p>
     <br>
     <p>Pozdrawiamy,<br>Zespół MeetHub</p>
 </body>
 </html>',
     subject = '🎉 Spotkanie {{meetingTitle}} się rozpoczęło!',
     updated_at = NOW()
 WHERE template_key = 'meeting_started' AND language = 'pl';