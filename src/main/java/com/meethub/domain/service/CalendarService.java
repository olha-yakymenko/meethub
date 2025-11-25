package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;

public interface CalendarService {
    String generateGoogleCalendarLink(Meeting meeting);
    String generateOutlookCalendarLink(Meeting meeting);
    void syncWithUserCalendar(Meeting meeting, User user);
}