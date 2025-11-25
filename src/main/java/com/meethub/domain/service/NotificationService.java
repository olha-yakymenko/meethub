package com.meethub.domain.service;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;

import java.util.List;

public interface NotificationService {
    void sendMeetingInvitation(Meeting meeting, User invitedUser, String invitationToken);
    void sendMeetingUpdate(Meeting meeting, List<MeetingParticipant> participants);
    void sendMeetingCancellation(Meeting meeting, List<MeetingParticipant> participants);
    void sendReminder(Meeting meeting, List<MeetingParticipant> participants);
    void sendParticipantStatusUpdate(Meeting meeting, User participant, String status);
}