// MeetingAnalyticsService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.OrganizerReport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface MeetingAnalyticsService {

    MeetingStatistics generateMeetingStatistics(@NotNull Long meetingId);
    Optional<MeetingStatistics> getMeetingStatistics(@NotNull Long meetingId);
    void deleteMeetingStatistics(@NotNull Long meetingId);

    OrganizerReport generateOrganizerReport(
            @NotNull Long organizerId,
            @Valid ReportFilter filter
    );

    byte[] exportReportToCsv(@NotNull Long organizerId, @Valid ReportFilter filter);
    byte[] exportReportToPdf(@NotNull Long organizerId, @Valid ReportFilter filter);

    List<MeetingStatistics> getMeetingStatisticsByOrganizer(@NotNull Long organizerId);
    byte[] exportMeetingStatisticsToCsv(@NotNull Long meetingId);
    byte[] exportMeetingStatisticsToPdf(@NotNull Long meetingId);
    BigDecimal getAverageResponseTime(@NotNull Long meetingId);

    List<MeetingStatistics> getRecentStatistics(int limit);
    Map<String, Object> getStatisticsOverview(@NotNull Long meetingId);
    void refreshAllStatistics();

}