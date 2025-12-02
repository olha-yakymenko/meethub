package com.meethub.domain.service;


import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.OrganizerReport;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MeetingAnalyticsService {
    MeetingStatistics generateMeetingStatistics(Long meetingId);

    @Transactional(readOnly = true)
    Optional<MeetingStatistics> getMeetingStatistics(Long meetingId);

    @Transactional
    void deleteMeetingStatistics(Long meetingId);

    OrganizerReport generateOrganizerReport(Long organizerId, ReportFilter filter);
    byte[] exportReportToCsv(Long organizerId, ReportFilter filter);
    byte[] exportReportToPdf(Long organizerId, ReportFilter filter);
    List<MeetingStatistics> getMeetingStatisticsByOrganizer(Long organizerId);

    @Transactional(readOnly = true)
    byte[] exportMeetingStatisticsToCsv(Long meetingId);

    @Transactional(readOnly = true)
    byte[] exportMeetingStatisticsToPdf(Long meetingId);

    @Transactional(readOnly = true)
    BigDecimal getAverageResponseTime(Long meetingId);

    List<MeetingStatistics> getRecentStatistics(int limit);

    Map<String, Object> getStatisticsOverview(Long meetingId);

    void refreshAllStatistics();
}