


-- ===============================
-- 1. Utwórz schemat i ustaw go
-- ===============================
CREATE SCHEMA IF NOT EXISTS meethub_schema;
SET SCHEMA meethub_schema;

-- ===============================
-- 2. Testowe dane
-- ===============================

-- 2.1. Users
INSERT INTO users (email, password, first_name, last_name, role, enabled)
VALUES ('test.admin@example.com', 'pass', 'Admin', 'Test', 'ADMIN', 1);

INSERT INTO users (email, password, first_name, last_name, role, enabled)
VALUES ('test.organizer@example.com', 'pass', 'Organizer', 'Test', 'ORGANIZER', 1);

INSERT INTO users (email, password, first_name, last_name, role, enabled)
VALUES ('test.user@example.com', 'pass', 'User', 'Test', 'PARTICIPANT', 1);

-- 2.2. Locations
INSERT INTO locations (name, type, timezone)
VALUES
('Test Conference Room', 'PHYSICAL', 'Europe/Warsaw');

-- 2.3. Meetings
INSERT INTO meetings (title, description, type, status, visibility, start_date, end_date, organizer_id)
VALUES (
    'Test Meeting',
    'Test Description',
    'PHYSICAL',
    'PLANNED',
    'PUBLIC',
    DATEADD('DAY', 1, CURRENT_TIMESTAMP),
    DATEADD('HOUR', 2, DATEADD('DAY', 1, CURRENT_TIMESTAMP)),
    (SELECT id FROM users WHERE email = 'test.organizer@example.com')
);

-- 2.4. Attendance tokens
-- Najpierw sprawdzamy, że spotkanie istnieje
INSERT INTO attendance_tokens (token, user_id, meeting_id, expires_at, status, created_at)
VALUES
('TOKEN_ACTIVE_123',
 (SELECT id FROM users WHERE email = 'test.user@example.com'),
 (SELECT id FROM meetings WHERE title = 'Test Meeting'),
 DATEADD('HOUR', 24, CURRENT_TIMESTAMP),
 'ACTIVE',
 CURRENT_TIMESTAMP
),
('TOKEN_USED_456',
 (SELECT id FROM users WHERE email = 'test.admin@example.com'),
 (SELECT id FROM meetings WHERE title = 'Test Meeting'),
 DATEADD('HOUR', 24, CURRENT_TIMESTAMP),
 'USED',
 CURRENT_TIMESTAMP
),
('TOKEN_EXPIRED_789',
 (SELECT id FROM users WHERE email = 'test.organizer@example.com'),
 (SELECT id FROM meetings WHERE title = 'Test Meeting'),
 DATEADD('HOUR', -1, CURRENT_TIMESTAMP),
 'EXPIRED',
 DATEADD('DAY', -1, CURRENT_TIMESTAMP)
);

INSERT INTO meeting_participants (meeting_id, user_id, status, permission_level, created_at, updated_at)
VALUES (
  (SELECT id FROM meetings WHERE title = 'Test Meeting'),
  (SELECT id FROM users WHERE email = 'test.user@example.com'),
  'CONFIRMED',  -- lub inny status zgodny z logiką aplikacji
  'PARTICIPANT',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);

-- Zasoby spotkania
INSERT INTO meeting_resources (
    filename, original_filename, file_path, resource_type, uploaded_by, uploaded_at, is_current, meeting_id
)
VALUES
('file1.pdf', 'file1.pdf', '/files/file1.pdf', 'DOCUMENT',
    (SELECT id FROM users WHERE email = 'test.admin@example.com'),
    DATEADD('DAY', -2, CURRENT_TIMESTAMP), TRUE,
    (SELECT id FROM meetings WHERE title = 'Test Meeting')
),
('file2.docx', 'file2.docx', '/files/file2.docx', 'DOCUMENT',
    (SELECT id FROM users WHERE email = 'test.admin@example.com'),
    DATEADD('DAY', -3, CURRENT_TIMESTAMP), TRUE,
    (SELECT id FROM meetings WHERE title = 'Test Meeting')
),
('presentation1.pptx', 'presentation1.pptx', '/files/presentation1.pptx', 'PRESENTATION',
    (SELECT id FROM users WHERE email = 'test.organizer@example.com'),
    DATEADD('DAY', -1, CURRENT_TIMESTAMP), TRUE,
    (SELECT id FROM meetings WHERE title = 'Test Meeting')
);


-- Tagi zasobów
INSERT INTO resource_tags (resource_id, tag) VALUES
(1, 'pdf'),
(1, 'important'),
(2, 'docx'),
(3, 'presentation');

-- ===============================
-- 5. Testowe dane dla głosowań
-- ===============================

-- 5.1. Voting dla Test Meeting
INSERT INTO meeting_votings (
    meeting_id, title, description, type, status, deadline_date, created_at
)
VALUES (
    (SELECT id FROM meetings WHERE title = 'Test Meeting'),
    'Test Voting 1',
    'Opis głosowania 1',
    'SINGLE_CHOICE',
    'ACTIVE',
    DATEADD('DAY', 1, CURRENT_TIMESTAMP),  -- deadline jutro
    CURRENT_TIMESTAMP
);

INSERT INTO meeting_votings (
    meeting_id, title, description, type, status, deadline_date, created_at
)
VALUES (
    (SELECT id FROM meetings WHERE title = 'Test Meeting'),
    'Test Voting 2 Expired',
    'Opis głosowania 2',
    'MULTIPLE_CHOICE',
    'ACTIVE',
    DATEADD('DAY', -1, CURRENT_TIMESTAMP),  -- deadline wczoraj (expired)
    CURRENT_TIMESTAMP
);

-- 5.2. Opcje głosowania dla pierwszego głosowania
INSERT INTO voting_options (
    voting_id, option_date, option_duration_minutes
)
VALUES (
    (SELECT id FROM meeting_votings WHERE title = 'Test Voting 1'),
    DATEADD('DAY', 1, CURRENT_TIMESTAMP),
    60
);

INSERT INTO voting_options (
    voting_id, option_date, option_duration_minutes
)
VALUES (
    (SELECT id FROM meeting_votings WHERE title = 'Test Voting 1'),
    DATEADD('DAY', 2, CURRENT_TIMESTAMP),
    90
);

-- 5.3. Głosy użytkowników
INSERT INTO votes (
    voting_id, option_id, user_id, vote_weight, voted_at
)
VALUES (
    (SELECT id FROM meeting_votings WHERE title = 'Test Voting 1'),
    (SELECT id FROM voting_options WHERE voting_id = (SELECT id FROM meeting_votings WHERE title = 'Test Voting 1') LIMIT 1),
    (SELECT id FROM users WHERE email = 'test.user@example.com'),
    1,
    CURRENT_TIMESTAMP
);

INSERT INTO votes (
    voting_id, option_id, user_id, vote_weight, voted_at
)
VALUES (
    (SELECT id FROM meeting_votings WHERE title = 'Test Voting 1'),
    (SELECT id FROM voting_options WHERE voting_id = (SELECT id FROM meeting_votings WHERE title = 'Test Voting 1') LIMIT 1 OFFSET 1),
    (SELECT id FROM users WHERE email = 'test.admin@example.com'),
    1,
    CURRENT_TIMESTAMP
);


INSERT INTO notifications (user_id, title, message, type, status, channel, reference_id, reference_type, template_key, scheduled_for, delivered_at, retry_count, error_message)
VALUES
(1, 'Welcome', 'Welcome to MeetHub!', 'MEETING_REMINDER', 'SENT', 'EMAIL', NULL, NULL, 'welcome_email', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL),
(1, 'New Message', 'You have a new message from Alice.', 'MEETING_REMINDER', 'PENDING', 'IN_APP', 101, 'MESSAGE', NULL, NULL, NULL, 0, NULL),
(2, 'Reminder', 'Don’t forget your meeting tomorrow.', 'MEETING_REMINDER', 'PENDING', 'IN_APP', 201, 'EVENT', NULL, NULL, NULL, 0, NULL),
(2, 'Weekly Digest', 'Here is your weekly summary.', 'MEETING_REMINDER', 'SENT', 'EMAIL', NULL, NULL, 'weekly_digest', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL);



INSERT INTO notification_schedules (user_id, schedule_type, trigger_time, enabled, custom_settings, created_at, updated_at)
VALUES
(1, 'DAILY_REMINDER', '08:00', true, '{"notify_via":"EMAIL"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'EVENING_SUMMARY', '20:00', true, '{"notify_via":"IN_APP"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'WEEKLY_DIGEST', '09:00', false, '{"notify_via":"EMAIL"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'EVENT_ALERT', '15:00', true, '{"notify_via":"IN_APP"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- Participant status history
INSERT INTO participant_status_history (participant_id, old_status, new_status, comment, changed_by_user_id, changed_at)
VALUES
-- Test User changes
(
  (SELECT id FROM meeting_participants WHERE user_id = (SELECT id FROM users WHERE email = 'test.user@example.com')
   AND meeting_id = (SELECT id FROM meetings WHERE title = 'Test Meeting')),
  'INVITED',
  'CONFIRMED',
  'User accepted invitation',
  (SELECT id FROM users WHERE email = 'test.user@example.com'),
  CURRENT_TIMESTAMP
),
-- Organizer changes participant status
(
  (SELECT id FROM meeting_participants WHERE user_id = (SELECT id FROM users WHERE email = 'test.user@example.com')
   AND meeting_id = (SELECT id FROM meetings WHERE title = 'Test Meeting')),
  'CONFIRMED',
  'PENDING',
  'Status changed by organizer',
  (SELECT id FROM users WHERE email = 'test.organizer@example.com'),
  CURRENT_TIMESTAMP
),
-- Admin manually updates participant status
(
  (SELECT id FROM meeting_participants WHERE user_id = (SELECT id FROM users WHERE email = 'test.user@example.com')
   AND meeting_id = (SELECT id FROM meetings WHERE title = 'Test Meeting')),
  'PENDING',
  'CONFIRMED',
  'Approved by admin',
  (SELECT id FROM users WHERE email = 'test.admin@example.com'),
  CURRENT_TIMESTAMP
);




-- Tasks
INSERT INTO tasks (id, title, description, status, deadline, meeting_id, created_by, created_at, updated_at, allow_self_assignment)
VALUES
(1, 'Prepare slides', 'Prepare presentation slides', 'TODO', DATEADD('DAY', 1, CURRENT_TIMESTAMP),
 (SELECT id FROM meetings WHERE title = 'Test Meeting'),
 (SELECT id FROM users WHERE email = 'test.organizer@example.com'),
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE),
(2, 'Send invitations', 'Send invitations to participants', 'TODO', DATEADD('DAY', 2, CURRENT_TIMESTAMP),
 (SELECT id FROM meetings WHERE title = 'Test Meeting'),
 (SELECT id FROM users WHERE email = 'test.organizer@example.com'),
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE),
(3, 'Setup room', 'Prepare conference room equipment', 'TODO', DATEADD('DAY', 3, CURRENT_TIMESTAMP),
 (SELECT id FROM meetings WHERE title = 'Test Meeting'),
 (SELECT id FROM users WHERE email = 'test.admin@example.com'),
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE);

-- Task Assignments
INSERT INTO task_assignments (id, status, comment, completed_at, assigned_at, task_id, user_id)
VALUES
(1, 'ASSIGNED', 'Assigned by organizer', NULL, CURRENT_TIMESTAMP,
 (SELECT id FROM tasks WHERE title = 'Prepare slides'),
 (SELECT id FROM users WHERE email = 'test.user@example.com')),
(2, 'ASSIGNED', 'Assigned by organizer', NULL, CURRENT_TIMESTAMP,
 (SELECT id FROM tasks WHERE title = 'Send invitations'),
 (SELECT id FROM users WHERE email = 'test.admin@example.com')),
(3, 'IN_PROGRESS', 'User started working', NULL, CURRENT_TIMESTAMP,
 (SELECT id FROM tasks WHERE title = 'Setup room'),
 (SELECT id FROM users WHERE email = 'test.user@example.com'));


-- User Preferences
INSERT INTO user_preferences (user_id, preference_key, preference_value, privacy_level, created_at)
VALUES
-- Test user preferences (user_id = 5)
(3, 'meeting_invitations', 'true', 'PRIVATE', CURRENT_TIMESTAMP),
(3, 'meeting_reminders', 'true', 'PRIVATE', CURRENT_TIMESTAMP),
(3, 'meeting_updates', 'true', 'PRIVATE', CURRENT_TIMESTAMP),
(3, 'task_assignments', 'true', 'PRIVATE', CURRENT_TIMESTAMP);



-- ===============================
-- Waitlist entries
-- ===============================
INSERT INTO waitlist_entries (id, meeting_id, user_id, position, joined_at, notified_at, auto_promote)
VALUES
(1, (SELECT id FROM meetings WHERE title = 'Test Meeting'),
    (SELECT id FROM users WHERE email = 'test.user@example.com'), 1, CURRENT_TIMESTAMP, NULL, TRUE),
(2, (SELECT id FROM meetings WHERE title = 'Test Meeting'),
    (SELECT id FROM users WHERE email = 'test.admin@example.com'), 2, CURRENT_TIMESTAMP, NULL, FALSE);
