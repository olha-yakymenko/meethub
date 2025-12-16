package com.meethub.domain.service.impl;


import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingMark;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.repository.jpa.MeetingMarkRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.service.MeetingMarkService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingMarkServiceImpl implements MeetingMarkService {

    private final MeetingMarkRepository meetingMarkRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;

    @Transactional
    @Override
    public void markAsImportant(Long userId, Long meetingId) {
        if (!meetingMarkRepository.existsByUserIdAndMeetingId(userId, meetingId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Meeting not found"));

            MeetingMark mark = new MeetingMark(user, meeting);
            meetingMarkRepository.save(mark);
        }
    }

    @Transactional
    @Override
    public void unmarkAsImportant(Long userId, Long meetingId) {
        meetingMarkRepository.deleteByUserIdAndMeetingId(userId, meetingId);
    }

@Override
public boolean isMeetingImportantForUser(Long userId, Long meetingId) {
        return meetingMarkRepository.existsByUserIdAndMeetingId(userId, meetingId);
    }

    public List<Long> getImportantMeetingIdsForUser(Long userId) {
        return meetingMarkRepository.findImportantMeetingIdsByUserId(userId);
    }

@Transactional
@Override
public boolean toggleImportant(Long userId, Long meetingId) {
        if (isMeetingImportantForUser(userId, meetingId)) {
            unmarkAsImportant(userId, meetingId);
            return false;
        } else {
            markAsImportant(userId, meetingId);
            return true;
        }
    }

}