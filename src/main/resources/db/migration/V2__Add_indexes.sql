-- Users indexes
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_role ON users(role);
CREATE INDEX idx_user_created ON users(created_at);

-- User preferences indexes
CREATE INDEX idx_user_pref_user ON user_preferences(user_id);
CREATE INDEX idx_user_pref_key ON user_preferences(preference_key);

-- Locations indexes
CREATE INDEX idx_location_coordinates ON locations(latitude, longitude);
CREATE INDEX idx_location_type ON locations(type);
CREATE INDEX idx_location_city ON locations(city);

-- Meetings indexes
CREATE INDEX idx_meeting_organizer ON meetings(organizer_id);
CREATE INDEX idx_meeting_status ON meetings(status);
CREATE INDEX idx_meeting_start_date ON meetings(start_date);
CREATE INDEX idx_meeting_end_date ON meetings(end_date);
CREATE INDEX idx_meeting_visibility ON meetings(visibility);
CREATE INDEX idx_meeting_date_range ON meetings(start_date, end_date);
CREATE INDEX idx_meeting_created ON meetings(created_at);

-- Meeting participants indexes
CREATE INDEX idx_participant_meeting ON meeting_participants(meeting_id);
CREATE INDEX idx_participant_user ON meeting_participants(user_id);
CREATE INDEX idx_participant_status ON meeting_participants(status);
CREATE INDEX idx_participant_response ON meeting_participants(response_date) WHERE response_date IS NOT NULL;

-- Meeting date proposals indexes
CREATE INDEX idx_proposal_meeting ON meeting_date_proposals(meeting_id);
CREATE INDEX idx_proposal_date ON meeting_date_proposals(proposed_date);

-- Date votes indexes
CREATE INDEX idx_vote_proposal ON date_votes(date_proposal_id);
CREATE INDEX idx_vote_user ON date_votes(user_id);
CREATE INDEX idx_vote_preference ON date_votes(preference_level);

-- Meeting resources indexes
CREATE INDEX idx_resource_meeting ON meeting_resources(meeting_id);
CREATE INDEX idx_resource_type ON meeting_resources(resource_type);
CREATE INDEX idx_resource_uploaded ON meeting_resources(uploaded_at);

-- Meeting tasks indexes
CREATE INDEX idx_task_meeting ON meeting_tasks(meeting_id);
CREATE INDEX idx_task_assignee ON meeting_tasks(assigned_to);
CREATE INDEX idx_task_status ON meeting_tasks(status);
CREATE INDEX idx_task_due_date ON meeting_tasks(due_date);
CREATE INDEX idx_task_priority ON meeting_tasks(priority);

-- Notifications indexes
CREATE INDEX idx_notification_user ON notifications(user_id);
CREATE INDEX idx_notification_status ON notifications(status);
CREATE INDEX idx_notification_created ON notifications(created_at);
CREATE INDEX idx_notification_type ON notifications(type);

-- Full-text search indexes (for future use)
CREATE INDEX idx_meeting_title_trgm ON meetings USING gin (title gin_trgm_ops);
CREATE INDEX idx_meeting_description_trgm ON meetings USING gin (description gin_trgm_ops);