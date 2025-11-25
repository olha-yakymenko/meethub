package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public abstract class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendMeetingInvitation(Meeting meeting, User invitedUser, String invitationToken) {
        // TODO: Implement email/SMS notifications
        log.info("Sending invitation to {} for meeting: {}", invitedUser.getEmail(), meeting.getTitle());
    }

    @Override
    public void sendMeetingUpdate(Meeting meeting, List<MeetingParticipant> participants) {
        log.info("Sending update notification for meeting: {} to {} participants",
                meeting.getTitle(), participants.size());
    }

    @Override
    public void sendMeetingCancellation(Meeting meeting, List<MeetingParticipant> participants) {
        log.info("Sending cancellation notification for meeting: {} to {} participants",
                meeting.getTitle(), participants.size());
    }

    @Override
    public void sendReminder(Meeting meeting, List<MeetingParticipant> participants) {
        log.info("Sending reminder for meeting: {} to {} participants",
                meeting.getTitle(), participants.size());
    }

    @Override
    public void sendParticipantStatusUpdate(Meeting meeting, User participant, String status) {
        log.info("Participant {} status updated to {} for meeting: {}",
                participant.getEmail(), status, meeting.getTitle());
    }
}