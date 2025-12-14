-- =============================================
-- MeetHub Sample Data
-- Insert sample data for testing and development
-- =============================================

SET search_path TO meethub_schema;

-- =============================================
-- 1. SAMPLE USERS
-- =============================================
-- Passwords are bcrypt encoded "password123"
INSERT INTO users (email, password, first_name, last_name, role) VALUES
('admin@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Admin', 'User', 'ADMIN'),
('organizer@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'John', 'Organizer', 'ORGANIZER'),
('user1@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Alice', 'Participant', 'PARTICIPANT'),
('user2@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Bob', 'Attendee', 'PARTICIPANT'),
('user3@meethub.com', '$2a$10$k9bUAugra.AHtbPWKwdcxu9v5a2YC/6D/msdYhXQKNnm8eTw.g4Uy', 'Charlie', 'Developer', 'PARTICIPANT')
ON CONFLICT (email) DO NOTHING;

-- =============================================
-- 2. SAMPLE LOCATIONS
-- =============================================
INSERT INTO locations (name, address, city, country, latitude, longitude, type) VALUES
('Conference Room A', '123 Main St', 'Warsaw', 'Poland', 52.2297, 21.0122, 'PHYSICAL'),
('Virtual Meeting Room', NULL, NULL, NULL, NULL, NULL, 'VIRTUAL'),
('Tech Hub Office', '456 Tech Ave', 'Krakow', 'Poland', 50.0647, 19.9450, 'PHYSICAL'),
('Creative Space', '789 Design St', 'Wroclaw', 'Poland', 51.1079, 17.0385, 'PHYSICAL')
ON CONFLICT DO NOTHING;

-- =============================================
-- 3. SAMPLE MEETINGS
-- =============================================
INSERT INTO meetings (title, description, agenda, type, status, visibility, start_date, end_date, max_participants, organizer_id, location_id) VALUES
('Spring Boot Workshop', 'Learn Spring Boot with hands-on examples', '1. Introduction 2. Hands-on coding 3. Q&A', 'PHYSICAL', 'CONFIRMED', 'PUBLIC', '2024-02-15 10:00:00', '2024-02-15 12:00:00', 20, 2, 1),
('Project Planning', 'Quarterly project planning meeting', 'Review goals, assign tasks, set deadlines', 'VIRTUAL', 'PLANNED', 'INVITATION_ONLY', '2024-02-20 14:00:00', '2024-02-20 15:30:00', 10, 2, 2),
('Team Building', 'Monthly team building activity', 'Fun activities and games for team bonding', 'PHYSICAL', 'CONFIRMED', 'PUBLIC', '2024-02-25 16:00:00', '2024-02-25 18:00:00', 30, 2, 3),
('Code Review Session', 'Weekly code review and pair programming', '1. Code walkthrough 2. Feedback session 3. Improvements', 'VIRTUAL', 'CONFIRMED', 'PUBLIC', '2024-02-28 09:00:00', '2024-02-28 10:30:00', 15, 3, 2)
ON CONFLICT DO NOTHING;

-- =============================================
-- 4. SAMPLE MEETING PARTICIPANTS
-- =============================================
INSERT INTO meeting_participants (meeting_id, user_id, status, permission_level) VALUES
(1, 3, 'CONFIRMED', 'PARTICIPANT'),
(1, 4, 'INVITED', 'PARTICIPANT'),
(1, 5, 'CONFIRMED', 'CONTRIBUTOR'),
(2, 3, 'CONFIRMED', 'CONTRIBUTOR'),
(2, 5, 'INVITED', 'PARTICIPANT'),
(3, 4, 'CONFIRMED', 'PARTICIPANT'),
(3, 5, 'CONFIRMED', 'PARTICIPANT'),
(4, 2, 'CONFIRMED', 'ORGANIZER'),
(4, 3, 'CONFIRMED', 'PARTICIPANT')
ON CONFLICT DO NOTHING;

-- =============================================
-- 5. SAMPLE PARTICIPANT STATUS HISTORY
-- =============================================
INSERT INTO participant_status_history (participant_id, old_status, new_status, comment, changed_by_user_id) VALUES
(1, NULL, 'INVITED', 'Initial invitation sent', 2),
(1, 'INVITED', 'CONFIRMED', 'User accepted invitation', 3),
(2, NULL, 'INVITED', 'Initial invitation sent', 2),
(3, NULL, 'INVITED', 'Initial invitation sent', 2),
(3, 'INVITED', 'CONFIRMED', 'User accepted invitation', 5)
ON CONFLICT DO NOTHING;

-- =============================================
-- 6. SAMPLE WAITLIST ENTRIES
-- =============================================
INSERT INTO waitlist_entries (meeting_id, user_id, position) VALUES
(1, 4, 1),
(3, 2, 1)
ON CONFLICT DO NOTHING;

-- =============================================
-- 7. SAMPLE USER GROUPS
-- =============================================
INSERT INTO user_groups (name, description, created_by) VALUES
('Developers', 'Software development team', 2),
('Managers', 'Project management team', 2),
('Design Team', 'UI/UX design team', 3),
('QA Team', 'Quality Assurance team', 4)
ON CONFLICT DO NOTHING;

-- =============================================
-- 8. SAMPLE USER GROUP MEMBERS
-- =============================================
INSERT INTO user_group_members (group_id, user_id, role) VALUES
(1, 3, 'MEMBER'),
(1, 4, 'MEMBER'),
(1, 5, 'ADMIN'),
(2, 2, 'ADMIN'),
(2, 3, 'MEMBER'),
(3, 3, 'MEMBER'),
(3, 5, 'MEMBER'),
(4, 4, 'ADMIN')
ON CONFLICT DO NOTHING;

-- =============================================
-- 9. SAMPLE MEETING TASKS
-- =============================================
INSERT INTO meeting_tasks (meeting_id, title, description, status, priority, assigned_to, due_date, progress_percentage) VALUES
(1, 'Prepare presentation', 'Create slides for Spring Boot workshop', 'IN_PROGRESS', 'HIGH', 2, '2024-02-14 18:00:00', 75),
(1, 'Book conference room', 'Reserve Conference Room A for the workshop', 'COMPLETED', 'MEDIUM', 2, '2024-02-10 12:00:00', 100),
(2, 'Create project timeline', 'Develop detailed project timeline with milestones', 'PENDING', 'HIGH', 3, '2024-02-18 17:00:00', 0),
(3, 'Prepare team building activities', 'Organize games and activities', 'IN_PROGRESS', 'MEDIUM', 4, '2024-02-23 12:00:00', 50),
(4, 'Review pull requests', 'Review pending pull requests before meeting', 'PENDING', 'HIGH', 5, '2024-02-27 18:00:00', 0)
ON CONFLICT DO NOTHING;

-- =============================================
-- 10. SAMPLE EMAIL TEMPLATES
-- =============================================
INSERT INTO email_templates (template_key, subject, body_template, language) VALUES
('meeting-invitation', 'Zaproszenie do spotkania: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Zostałeś zaproszony do spotkania "{{meetingTitle}}" organizowanego przez {{organizerName}}.<br><br>Data: {{meetingDate}}<br><br><a href="{{confirmationLink}}">Potwierdź udział</a>', 'pl'),
('waitlist-notification', 'Zostałeś dodany do listy oczekujących: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Zostałeś dodany do listy oczekujących na spotkanie "{{meetingTitle}}". Twoja pozycja w kolejce: {{position}}.<br><br>Powiadomimy Cię gdy miejsce się zwolni.', 'pl'),
('waitlist-promotion', 'Miejsce zwolniło się w spotkaniu: {{meetingTitle}}', 'Witaj {{userName}}!<br><br>Miejsce zwolniło się w spotkaniu "{{meetingTitle}}" i zostałeś automatycznie zapisany!<br><br>Zapraszamy do udziału.', 'pl'),
('meeting_started', '🎉 Spotkanie {{meetingTitle}} się rozpoczęło!', '<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Spotkanie się rozpoczęło</title></head><body><h1>🎉 Spotkanie się rozpoczęło!</h1><p>Cześć {{userName}}!</p><p>Twoje spotkanie <strong>{{meetingTitle}}</strong> właśnie się rozpoczęło.</p><p><strong>Czas:</strong> {{meetingTime}}</p>{{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}{{#meetingLink}}<p><strong>Link do spotkania:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}<br><p>Pozdrawiamy,<br>Zespół MeetHub</p></body></html>', 'pl'),
('meeting_reminder', '🔔 Przypomnienie: Spotkanie {{meetingTitle}} za {{minutesBefore}} minut', '<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Przypomnienie o spotkaniu</title></head><body><h1>🔔 Przypomnienie o spotkaniu</h1><p>Cześć {{userName}}!</p><p>Przypominamy o spotkaniu <strong>{{meetingTitle}}</strong>.</p><p><strong>Rozpoczyna się za:</strong> {{minutesBefore}} minut</p><p><strong>Godzina:</strong> {{meetingTime}}</p>{{#location}}<p><strong>Miejsce:</strong> {{location}}</p>{{/location}}{{#meetingLink}}<p><strong>Link:</strong> <a href="{{meetingLink}}">{{meetingLink}}</a></p>{{/meetingLink}}<br><p>Pozdrawiamy,<br>Zespół MeetHub</p></body></html>', 'pl')
ON CONFLICT (template_key, language) DO UPDATE
SET subject = EXCLUDED.subject,
    body_template = EXCLUDED.body_template,
    updated_at = CURRENT_TIMESTAMP;

-- =============================================
-- 11. SAMPLE CATEGORIES
-- =============================================
INSERT INTO categories (name, description, color_code, created_by) VALUES
('Szkolenia', 'Spotkania szkoleniowe i warsztaty', '#3498db', 2),
('Spotkania biznesowe', 'Spotkania związane z biznesem', '#2ecc71', 2),
('Społecznościowe', 'Spotkania społeczności i networking', '#e74c3c', 2),
('Techniczne', 'Spotkania techniczne i programistyczne', '#f39c12', 2),
('Planowanie', 'Spotkania planistyczne i strategiczne', '#9b59b6', 2),
('Review', 'Spotkania code review i feedback', '#1abc9c', 3),
('Team Building', 'Integracja i budowanie zespołu', '#34495e', 4)
ON CONFLICT DO NOTHING;

-- =============================================
-- 12. SAMPLE MEETING CATEGORIES
-- =============================================
INSERT INTO meeting_categories (meeting_id, category_id) VALUES
(1, 1),
(1, 4),
(2, 5),
(2, 2),
(3, 7),
(3, 3),
(4, 6),
(4, 4)
ON CONFLICT DO NOTHING;

-- =============================================
-- 13. SAMPLE MEETING TAGS
-- =============================================
INSERT INTO meeting_tags (meeting_id, tag) VALUES
(1, 'spring-boot'),
(1, 'workshop'),
(1, 'java'),
(2, 'project'),
(2, 'planning'),
(2, 'quarterly'),
(3, 'team'),
(3, 'fun'),
(3, 'integration'),
(4, 'code-review'),
(4, 'programming'),
(4, 'feedback')
ON CONFLICT DO NOTHING;

-- =============================================
-- 14. SAMPLE MEETING STATUS CHANGES
-- =============================================
INSERT INTO meeting_status_changes (meeting_id, old_status, new_status, changed_by_user_id, reason) VALUES
(1, 'PLANNED', 'CONFIRMED', 2, 'Venue confirmed and all speakers available'),
(2, 'DRAFT', 'PLANNED', 2, 'Initial planning completed'),
(3, 'PLANNED', 'CONFIRMED', 2, 'Activities and venue confirmed'),
(4, 'PLANNED', 'CONFIRMED', 3, 'Team availability confirmed')
ON CONFLICT DO NOTHING;

-- =============================================
-- 15. SAMPLE TASKS AND ASSIGNMENTS
-- =============================================
INSERT INTO tasks (title, description, status, deadline, meeting_id, created_by) VALUES
('Prepare agenda', 'Create detailed agenda for the workshop', 'TODO', '2024-02-14 18:00:00', 1, 2),
('Send invitations', 'Send meeting invitations to participants', 'IN_PROGRESS', '2024-02-13 12:00:00', 1, 2),
('Prepare materials', 'Prepare training materials and handouts', 'TODO', '2024-02-14 15:00:00', 1, 3),
('Review code', 'Review project code before meeting', 'DONE', '2024-02-27 10:00:00', 4, 5)
ON CONFLICT DO NOTHING;

INSERT INTO task_assignments (task_id, user_id, status) VALUES
(1, 2, 'ASSIGNED'),
(2, 3, 'IN_PROGRESS'),
(3, 4, 'ASSIGNED'),
(4, 5, 'COMPLETED')
ON CONFLICT DO NOTHING;

-- =============================================
-- 16. SAMPLE FEEDBACK
-- =============================================
INSERT INTO feedbacks (meeting_id, user_id, rating, comment) VALUES
(1, 3, 5, 'Excellent workshop! Very informative and well-organized.'),
(1, 5, 4, 'Great content, but could use more hands-on exercises.'),
(4, 3, 5, 'Very productive code review session.'),
(4, 2, 4, 'Good discussion, but ran a bit over time.')
ON CONFLICT DO NOTHING;

-- =============================================
-- 17. SAMPLE MEETING VOTINGS
-- =============================================
INSERT INTO meeting_votings (meeting_id, title, description, type, status, max_choices, deadline_date) VALUES
(1, 'Choose next workshop topic', 'Vote for the topic of our next workshop', 'MULTIPLE_CHOICE', 'ACTIVE', 2, '2024-02-28 23:59:59'),
(2, 'Meeting time preference', 'Select your preferred meeting time', 'SINGLE_CHOICE', 'ACTIVE', 1, '2024-02-25 12:00:00'),
(4, 'Code review frequency', 'How often should we have code reviews?', 'MULTIPLE_CHOICE', 'CLOSED', 1, '2024-02-20 18:00:00')
ON CONFLICT DO NOTHING;

INSERT INTO voting_options (voting_id, option_date, option_duration_minutes) VALUES
(1, '2024-03-10 10:00:00', 120),
(1, '2024-03-17 10:00:00', 120),
(1, '2024-03-24 14:00:00', 120),
(2, '2024-02-27 09:00:00', 90),
(2, '2024-02-27 14:00:00', 90),
(2, '2024-02-28 10:00:00', 90)
ON CONFLICT DO NOTHING;

INSERT INTO votes (voting_id, option_id, user_id, vote_weight, preference_order) VALUES
(1, 1, 3, 1, 1),
(1, 2, 3, 1, 2),
(1, 1, 5, 1, 1),
(1, 3, 5, 1, 2),
(2, 4, 3, 1, 1),
(2, 5, 5, 1, 1)
ON CONFLICT DO NOTHING;

-- =============================================
-- 18. USER NOTIFICATION CHANNELS
-- =============================================
INSERT INTO user_notification_channels (user_id, channel)
SELECT id, 'EMAIL' FROM users
WHERE email_notifications_enabled = true
ON CONFLICT (user_id, channel) DO NOTHING;

INSERT INTO user_notification_channels (user_id, channel)
SELECT id, 'PUSH' FROM users
WHERE push_notifications_enabled = true
ON CONFLICT (user_id, channel) DO NOTHING;

-- =============================================
-- 19. SAMPLE NOTIFICATIONS
-- =============================================
INSERT INTO notifications (user_id, title, message, type, status, channel, reference_id, reference_type) VALUES
(2, 'Nowy uczestnik', 'Alice dołączyła do spotkania Spring Boot Workshop', 'MEETING', 'SENT', 'EMAIL', 1, 'MEETING'),
(3, 'Przypomnienie o spotkaniu', 'Spotkanie Spring Boot Workshop zaczyna się za 30 minut', 'REMINDER', 'PENDING', 'PUSH', 1, 'MEETING'),
(4, 'Zaakceptowano prośbę', 'Twoja prośba o dołączenie do spotkania Team Building została zaakceptowana', 'MEETING', 'SENT', 'EMAIL', 3, 'MEETING'),
(5, 'Nowe zadanie', 'Przypisano Ci zadanie: Prepare materials', 'TASK', 'SENT', 'EMAIL', 3, 'TASK')
ON CONFLICT DO NOTHING;

-- =============================================
-- 20. SAMPLE MEETING STATISTICS
-- =============================================
INSERT INTO meeting_statistics (meeting_id, total_participants, confirmed_participants, attended_participants, attendance_rate, confirmation_rate, feedback_count, avg_feedback_rating, generated_at) VALUES
(1, 3, 2, 2, 66.67, 66.67, 2, 4.5, '2024-02-16 10:00:00'),
(4, 3, 3, 3, 100.0, 100.0, 2, 4.5, '2024-02-28 11:00:00')
ON CONFLICT DO NOTHING;

-- =============================================
-- 21. UPDATE SEQUENCES
-- =============================================
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval('user_preferences_id_seq', COALESCE((SELECT MAX(id) FROM user_preferences), 1));
SELECT setval('locations_id_seq', COALESCE((SELECT MAX(id) FROM locations), 1));
SELECT setval('meetings_id_seq', COALESCE((SELECT MAX(id) FROM meetings), 1));
SELECT setval('meeting_participants_id_seq', COALESCE((SELECT MAX(id) FROM meeting_participants), 1));
SELECT setval('participant_status_history_id_seq', COALESCE((SELECT MAX(id) FROM participant_status_history), 1));
SELECT setval('waitlist_entries_id_seq', COALESCE((SELECT MAX(id) FROM waitlist_entries), 1));
SELECT setval('user_groups_id_seq', COALESCE((SELECT MAX(id) FROM user_groups), 1));
SELECT setval('user_group_members_id_seq', COALESCE((SELECT MAX(id) FROM user_group_members), 1));
SELECT setval('meeting_resources_id_seq', COALESCE((SELECT MAX(id) FROM meeting_resources), 1));
SELECT setval('meeting_tasks_id_seq', COALESCE((SELECT MAX(id) FROM meeting_tasks), 1));
SELECT setval('notifications_id_seq', COALESCE((SELECT MAX(id) FROM notifications), 1));
SELECT setval('email_templates_id_seq', COALESCE((SELECT MAX(id) FROM email_templates), 1));
SELECT setval('tasks_id_seq', COALESCE((SELECT MAX(id) FROM tasks), 1));
SELECT setval('task_assignments_id_seq', COALESCE((SELECT MAX(id) FROM task_assignments), 1));
SELECT setval('task_files_id_seq', COALESCE((SELECT MAX(id) FROM task_files), 1));
SELECT setval('feedbacks_id_seq', COALESCE((SELECT MAX(id) FROM feedbacks), 1));
SELECT setval('meeting_statistics_id_seq', COALESCE((SELECT MAX(id) FROM meeting_statistics), 1));
SELECT setval('meeting_votings_id_seq', COALESCE((SELECT MAX(id) FROM meeting_votings), 1));
SELECT setval('voting_options_id_seq', COALESCE((SELECT MAX(id) FROM voting_options), 1));
SELECT setval('votes_id_seq', COALESCE((SELECT MAX(id) FROM votes), 1));
SELECT setval('categories_id_seq', COALESCE((SELECT MAX(id) FROM categories), 1));
SELECT setval('meeting_status_changes_id_seq', COALESCE((SELECT MAX(id) FROM meeting_status_changes), 1));
SELECT setval('attendance_tokens_id_seq', COALESCE((SELECT MAX(id) FROM attendance_tokens), 1));
SELECT setval('notification_schedules_id_seq', COALESCE((SELECT MAX(id) FROM notification_schedules), 1));

-- =============================================
-- 22. FINAL CONFIGURATION
-- =============================================
-- Set default values for email_templates
UPDATE email_templates SET
    name = template_key,
    category = 'NOTIFICATION',
    is_active = TRUE,
    version = 1,
    channel = 'EMAIL'
WHERE name IS NULL OR category IS NULL OR is_active IS NULL;

-- Update updated_at for all tables with that column
UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE updated_at < created_at;
UPDATE meetings SET updated_at = CURRENT_TIMESTAMP WHERE updated_at < created_at;
UPDATE categories SET updated_at = CURRENT_TIMESTAMP WHERE updated_at < created_at;

COMMIT;