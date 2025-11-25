package com.meethub.domain.service;

import com.meethub.domain.model.response.StatisticsResponse;
import java.time.LocalDate;

public interface StatisticsService {
    StatisticsResponse getUserStatistics(Long userId);
    StatisticsResponse getMeetingStatistics(Long meetingId);
    StatisticsResponse getPlatformStatistics(LocalDate from, LocalDate to);
}