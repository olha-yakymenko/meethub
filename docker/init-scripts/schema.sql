--CREATE SCHEMA IF NOT EXISTS meethub_schema;


-- =============================================
-- MeetHub Database Schema Script
-- Complete database structure setup
-- =============================================

-- 1. CREATE DATABASE (if not exists)
SELECT 'CREATE DATABASE meethub' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'meethub');\gexec

-- 2. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "cube";
CREATE EXTENSION IF NOT EXISTS "earthdistance";

-- 3. APPLICATION USER
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'meethub_app') THEN
        CREATE USER meethub_app WITH PASSWORD 'app_password123';
    END IF;
END $$;

GRANT CONNECT ON DATABASE meethub TO meethub_app;

-- 4. SCHEMA SETUP
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

-- Locations table
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

-- Feedback table
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(meeting_id, user_id)
);

-- Meeting statistics table
CREATE TABLE IF NOT EXISTS meeting_statistics (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    total_participants INTEGER DEFAULT 0,
    confirmed_participants INTEGER DEFAULT 0,
    attended_participants INTEGER DEFAULT 0,
    attendance_rate DOUBLE PRECISION DEFAULT 0.0,
    confirmation_rate DOUBLE PRECISION DEFAULT 0.0,
    avg_response_time_hours DOUBLE PRECISION DEFAULT 0.0,
    no_show_count INTEGER DEFAULT 0,
    engagement_score DOUBLE PRECISION DEFAULT 0.0,
    task_completion_rate DOUBLE PRECISION DEFAULT 0.0,
    feedback_count INTEGER DEFAULT 0,
    avg_feedback_rating DOUBLE PRECISION DEFAULT 0.0,
    generated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_meeting_statistics_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(id)
        ON DELETE CASCADE
);

-- Meeting votings table
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

-- Voting options table
CREATE TABLE IF NOT EXISTS voting_options (
    id BIGSERIAL PRIMARY KEY,
    voting_id BIGINT NOT NULL REFERENCES meeting_votings(id) ON DELETE CASCADE,
    option_date TIMESTAMP NOT NULL,
    option_duration_minutes INTEGER,
    is_suggested BOOLEAN DEFAULT FALSE,
    suggested_by BIGINT REFERENCES users(id)
);

-- Votes table
CREATE TABLE IF NOT EXISTS votes (
    id BIGSERIAL PRIMARY KEY,
    voting_id BIGINT NOT NULL REFERENCES meeting_votings(id) ON DELETE CASCADE,
    option_id BIGINT NOT NULL REFERENCES voting_options(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vote_weight INTEGER DEFAULT 1,
    preference_order INTEGER,
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_voting_option UNIQUE (user_id, voting_id, option_id)
);

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color_code VARCHAR(7) DEFAULT '#3498db',
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Meeting categories table
CREATE TABLE IF NOT EXISTS meeting_categories (
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (meeting_id, category_id)
);

-- Meeting status changes table
CREATE TABLE IF NOT EXISTS meeting_status_changes (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by_user_id BIGINT REFERENCES users(id),
    reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Attendance tokens table
CREATE TABLE IF NOT EXISTS attendance_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    meeting_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_attendance_token_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_token_meeting
        FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE
);

-- User notification channels table
CREATE TABLE IF NOT EXISTS user_notification_channels (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, channel)
);

-- Notification variables table
CREATE TABLE IF NOT EXISTS notification_variables (
    notification_id BIGINT NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    variable_key VARCHAR(100) NOT NULL,
    variable_value TEXT,
    PRIMARY KEY (notification_id, variable_key)
);

-- Notification schedules table
CREATE TABLE IF NOT EXISTS notification_schedules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    schedule_type VARCHAR(50) NOT NULL,
    trigger_time VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    custom_settings TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- 6. ADDITIONAL COLUMNS
-- =============================================

-- Additional columns for users
ALTER TABLE users
ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS push_notifications_enabled BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS sms_notifications_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS digest_enabled BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS digest_frequency VARCHAR(20) DEFAULT 'DAILY',
ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'Europe/Warsaw',
ADD COLUMN IF NOT EXISTS language VARCHAR(10) DEFAULT 'pl';

-- Additional columns for meetings
ALTER TABLE meetings
ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS recurrence_pattern VARCHAR(100),
ADD COLUMN IF NOT EXISTS recurrence_end_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS recurrence_exceptions TEXT,
ADD COLUMN IF NOT EXISTS is_template BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS original_meeting_id BIGINT,
ADD COLUMN IF NOT EXISTS workflow_stage VARCHAR(50) DEFAULT 'DRAFT',
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

-- Additional columns for meeting_resources
ALTER TABLE meeting_resources
ADD COLUMN IF NOT EXISTS download_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS description TEXT;

-- Additional columns for meeting_participants
ALTER TABLE meeting_participants
ADD COLUMN IF NOT EXISTS join_method VARCHAR(50),
ADD COLUMN IF NOT EXISTS rating INTEGER CHECK (rating >= 1 AND rating <= 5),
ADD COLUMN IF NOT EXISTS feedback TEXT,
ADD COLUMN IF NOT EXISTS attendance_duration_minutes INTEGER,
ADD COLUMN IF NOT EXISTS attendance_confirmed_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS attendance_token_used VARCHAR(255);

-- Additional columns for notifications
ALTER TABLE notifications
ADD COLUMN IF NOT EXISTS template_key VARCHAR(100),
ADD COLUMN IF NOT EXISTS scheduled_for TIMESTAMP,
ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS error_message TEXT;

-- Additional columns for meeting_tags
ALTER TABLE meeting_tags
ADD COLUMN IF NOT EXISTS added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS added_by BIGINT REFERENCES users(id);

-- Additional columns for tasks
ALTER TABLE tasks
ADD COLUMN IF NOT EXISTS allow_self_assignment BOOLEAN,
ADD COLUMN IF NOT EXISTS allowed_file_types TEXT DEFAULT 'pdf,doc,docx,jpg,png';

-- Additional columns for email_templates
ALTER TABLE email_templates
ADD COLUMN IF NOT EXISTS name VARCHAR(200),
ADD COLUMN IF NOT EXISTS category VARCHAR(100),
ADD COLUMN IF NOT EXISTS description TEXT,
ADD COLUMN IF NOT EXISTS variables_help TEXT,
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1,
ADD COLUMN IF NOT EXISTS channel VARCHAR(50),
ADD COLUMN IF NOT EXISTS available_variables TEXT;

-- =============================================
-- 7. INDEXES
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
CREATE INDEX IF NOT EXISTS idx_participants_join_method ON meeting_participants(join_method);
CREATE INDEX IF NOT EXISTS idx_participants_response_date ON meeting_participants(response_date);

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
CREATE INDEX IF NOT EXISTS idx_resource_uploaded_by ON meeting_resources(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_resource_current ON meeting_resources(is_current);
CREATE INDEX IF NOT EXISTS idx_resource_uploaded_at ON meeting_resources(uploaded_at DESC);

-- Meeting tasks indexes
CREATE INDEX IF NOT EXISTS idx_task_meeting ON meeting_tasks(meeting_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee ON meeting_tasks(assigned_to);
CREATE INDEX IF NOT EXISTS idx_task_status ON meeting_tasks(status);
CREATE INDEX IF NOT EXISTS idx_task_due_date ON meeting_tasks(due_date);

-- Notifications indexes
CREATE INDEX IF NOT EXISTS idx_notification_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notification_created ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_scheduled ON notifications(scheduled_for, status);
CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_desc ON notifications(created_at DESC);

-- Email templates indexes
CREATE INDEX IF NOT EXISTS idx_email_template_key ON email_templates(template_key);

-- Tasks indexes
CREATE INDEX IF NOT EXISTS idx_tasks_meeting_id ON tasks(meeting_id);
CREATE INDEX IF NOT EXISTS idx_tasks_created_by ON tasks(created_by);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);

-- Task assignments indexes
CREATE INDEX IF NOT EXISTS idx_assignments_task_id ON task_assignments(task_id);
CREATE INDEX IF NOT EXISTS idx_assignments_user_id ON task_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_assignments_status ON task_assignments(status);

-- Task files indexes
CREATE INDEX IF NOT EXISTS idx_files_assignment_id ON task_files(assignment_id);

-- Feedback indexes
CREATE INDEX IF NOT EXISTS idx_feedbacks_meeting ON feedbacks(meeting_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_user ON feedbacks(user_id);

-- Meeting statistics indexes
CREATE INDEX IF NOT EXISTS idx_meeting_statistics_meeting ON meeting_statistics(meeting_id);
CREATE INDEX IF NOT EXISTS idx_meeting_statistics_created ON meeting_statistics(created_at);

-- Voting indexes
CREATE INDEX IF NOT EXISTS idx_voting_meeting ON meeting_votings(meeting_id);
CREATE INDEX IF NOT EXISTS idx_voting_status ON meeting_votings(status);
CREATE INDEX IF NOT EXISTS idx_voting_deadline ON meeting_votings(deadline_date);
CREATE INDEX IF NOT EXISTS idx_option_voting ON voting_options(voting_id);
CREATE INDEX IF NOT EXISTS idx_option_date ON voting_options(option_date);
CREATE INDEX IF NOT EXISTS idx_vote_voting ON votes(voting_id);
CREATE INDEX IF NOT EXISTS idx_vote_option ON votes(option_id);
CREATE INDEX IF NOT EXISTS idx_vote_user ON votes(user_id);
CREATE INDEX IF NOT EXISTS idx_vote_user_voting ON votes(user_id, voting_id);
CREATE INDEX IF NOT EXISTS idx_vote_preference ON votes(preference_order);

-- Categories indexes
CREATE INDEX IF NOT EXISTS idx_categories_created_by ON categories(created_by);
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name);
CREATE INDEX IF NOT EXISTS idx_categories_search ON categories USING GIN (to_tsvector('english', name || ' ' || description));

-- Meeting categories indexes
CREATE INDEX IF NOT EXISTS idx_meeting_categories_meeting ON meeting_categories(meeting_id);
CREATE INDEX IF NOT EXISTS idx_meeting_categories_category ON meeting_categories(category_id);

-- Meeting status changes indexes
CREATE INDEX IF NOT EXISTS idx_status_changes_meeting ON meeting_status_changes(meeting_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_status_changes_changed_by ON meeting_status_changes(changed_by_user_id);
CREATE INDEX IF NOT EXISTS idx_status_changes_changed_at ON meeting_status_changes(changed_at);

-- Attendance tokens indexes
CREATE INDEX IF NOT EXISTS idx_attendance_tokens_token ON attendance_tokens(token);
CREATE INDEX IF NOT EXISTS idx_attendance_tokens_user_meeting ON attendance_tokens(user_id, meeting_id);
CREATE INDEX IF NOT EXISTS idx_attendance_tokens_status ON attendance_tokens(status);
CREATE INDEX IF NOT EXISTS idx_attendance_tokens_expires ON attendance_tokens(expires_at);

-- Notification schedules indexes
CREATE INDEX IF NOT EXISTS idx_notification_schedules_user ON notification_schedules(user_id);

-- Meeting tags indexes
CREATE INDEX IF NOT EXISTS idx_meeting_tags_tag ON meeting_tags(tag);

-- Meeting search indexes
CREATE INDEX IF NOT EXISTS idx_meetings_search_title ON meetings USING GIN (to_tsvector('english', title));
CREATE INDEX IF NOT EXISTS idx_meetings_search_description ON meetings USING GIN (to_tsvector('english', description));

-- Recurring meetings indexes
CREATE INDEX IF NOT EXISTS idx_meetings_recurring ON meetings(is_recurring, recurrence_end_date);
CREATE INDEX IF NOT EXISTS idx_meetings_template ON meetings(is_template, organizer_id);
CREATE INDEX IF NOT EXISTS idx_meetings_original ON meetings(original_meeting_id);

-- =============================================
-- 8. VIEWS
-- =============================================

-- Recurring meetings view
CREATE OR REPLACE VIEW recurring_meetings_view AS
SELECT
    m.id,
    m.title,
    m.start_date,
    m.recurrence_pattern,
    m.recurrence_end_date,
    m.is_template,
    u.email as organizer_email,
    COUNT(DISTINCT mp.id) as participant_count
FROM meetings m
JOIN users u ON m.organizer_id = u.id
LEFT JOIN meeting_participants mp ON m.id = mp.meeting_id AND mp.status = 'CONFIRMED'
WHERE m.is_recurring = TRUE
GROUP BY m.id, m.title, m.start_date, m.recurrence_pattern, m.recurrence_end_date, m.is_template, u.email;

-- Meeting templates view
CREATE OR REPLACE VIEW meeting_templates_view AS
SELECT
    m.id,
    m.title,
    m.description,
    m.type,
    m.visibility,
    u.email as created_by_email,
    COUNT(DISTINCT mc.category_id) as category_count,
    ARRAY_AGG(DISTINCT mt.tag) as tags
FROM meetings m
JOIN users u ON m.organizer_id = u.id
LEFT JOIN meeting_categories mc ON m.id = mc.meeting_id
LEFT JOIN meeting_tags mt ON m.id = mt.meeting_id
WHERE m.is_template = TRUE
GROUP BY m.id, m.title, m.description, m.type, m.visibility, u.email;

-- =============================================
-- 9. FUNCTIONS AND TRIGGERS
-- =============================================

-- Function to update meeting_statistics updated_at
CREATE OR REPLACE FUNCTION update_meeting_statistics_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for meeting_statistics
CREATE TRIGGER trg_meeting_statistics_updated_at
BEFORE UPDATE ON meeting_statistics
FOR EACH ROW
EXECUTE FUNCTION update_meeting_statistics_updated_at();

-- Function to update meetings updated_at
CREATE OR REPLACE FUNCTION update_meetings_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for meetings
CREATE TRIGGER trg_update_meetings_updated_at
BEFORE UPDATE ON meetings
FOR EACH ROW
EXECUTE FUNCTION update_meetings_updated_at();

-- Function to update categories updated_at
CREATE OR REPLACE FUNCTION update_categories_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for categories
CREATE TRIGGER trg_update_categories_updated_at
BEFORE UPDATE ON categories
FOR EACH ROW
EXECUTE FUNCTION update_categories_updated_at();

-- Function to generate next occurrences for recurring meetings
CREATE OR REPLACE FUNCTION generate_next_occurrences(
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
    -- Get meeting data
    SELECT
        m.start_date,
        m.recurrence_pattern,
        m.recurrence_end_date,
        m.recurrence_exceptions
    INTO rec
    FROM meetings m
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

    -- Generate dates
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

        -- Check if recurrence end date exceeded
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

-- Function to get meeting series
CREATE OR REPLACE FUNCTION get_meeting_series(original_id BIGINT)
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
    FROM meetings m
    WHERE m.original_meeting_id = original_id
       OR m.id = original_id
    ORDER BY m.start_date;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- 10. PERMISSIONS
-- =============================================

-- Grant permissions to application user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA meethub_schema TO meethub_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA meethub_schema TO meethub_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA meethub_schema TO meethub_app;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA meethub_schema TO meethub_app;

-- Grant permissions on views
GRANT SELECT ON recurring_meetings_view TO meethub_user, meethub_app;
GRANT SELECT ON meeting_templates_view TO meethub_user, meethub_app;

-- Grant execute permissions on functions
GRANT EXECUTE ON FUNCTION generate_next_occurrences TO meethub_user, meethub_app;
GRANT EXECUTE ON FUNCTION get_meeting_series TO meethub_user, meethub_app;



