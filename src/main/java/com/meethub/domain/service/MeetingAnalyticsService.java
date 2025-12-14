package com.meethub.domain.service;

import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.OrganizerReport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Validated
public interface MeetingAnalyticsService {

    // Walidacja tylko parametrów wejściowych
    MeetingStatistics generateMeetingStatistics(
            @NotNull(message = "ID spotkania nie może być puste")
            @Positive(message = "ID spotkania musi być liczbą dodatnią")
            Long meetingId
    );

    @Transactional(readOnly = true)
    Optional<MeetingStatistics> getMeetingStatistics(
            @NotNull(message = "ID spotkania nie może być puste")
            @Positive(message = "ID spotkania musi być liczbą dodatnią")
            Long meetingId
    );

    @Transactional
    void deleteMeetingStatistics(
            @NotNull(message = "ID spotkania nie może być puste")
            @Positive(message = "ID spotkania musi być liczbą dodatnią")
            Long meetingId
    );

    OrganizerReport generateOrganizerReport(
            @NotNull(message = "ID organizatora nie może być puste")
            @Positive(message = "ID organizatora musi być liczbą dodatnią")
            Long organizerId,
            @Valid ReportFilter filter
    );

    byte[] exportReportToCsv(
            @NotNull(message = "ID organizatora nie może być puste")
            @Positive(message = "ID organizatora musi być liczbą dodatnią")
            Long organizerId,
            @Valid ReportFilter filter
    );

    byte[] exportReportToPdf(
            @NotNull(message = "ID organizatora nie może być puste")
            @Positive(message = "ID organizatora musi być liczbą dodatnią")
            Long organizerId,
            @Valid ReportFilter filter
    );

    List<MeetingStatistics> getMeetingStatisticsByOrganizer(
            @NotNull @Positive Long organizerId
    );

    @Transactional(readOnly = true)
    byte[] exportMeetingStatisticsToCsv(
            @NotNull @Positive Long meetingId
    );

    @Transactional(readOnly = true)
    byte[] exportMeetingStatisticsToPdf(
            @NotNull @Positive Long meetingId
    );

    @Transactional(readOnly = true)
    BigDecimal getAverageResponseTime(
            @NotNull @Positive Long meetingId
    );

    @Min(value = 1, message = "Limit musi być co najmniej 1")
    List<MeetingStatistics> getRecentStatistics(int limit);

    // Map zwracana nie może mieć walidacji liczbowej
    Map<String, Object> getStatisticsOverview(
            @NotNull @Positive Long meetingId
    );

    void refreshAllStatistics();
}






//package com.meethub.domain.service;
//
//
//import com.meethub.domain.model.entity.MeetingStatistics;
//import com.meethub.domain.model.request.ReportFilter;
//import com.meethub.domain.model.response.OrganizerReport;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//public interface MeetingAnalyticsService {
//    MeetingStatistics generateMeetingStatistics(Long meetingId);
//
//    @Transactional(readOnly = true)
//    Optional<MeetingStatistics> getMeetingStatistics(Long meetingId);
//
//    @Transactional
//    void deleteMeetingStatistics(Long meetingId);
//
//    OrganizerReport generateOrganizerReport(Long organizerId, ReportFilter filter);
//    byte[] exportReportToCsv(Long organizerId, ReportFilter filter);
//    byte[] exportReportToPdf(Long organizerId, ReportFilter filter);
//    List<MeetingStatistics> getMeetingStatisticsByOrganizer(Long organizerId);
//
//    @Transactional(readOnly = true)
//    byte[] exportMeetingStatisticsToCsv(Long meetingId);
//
//    @Transactional(readOnly = true)
//    byte[] exportMeetingStatisticsToPdf(Long meetingId);
//
//    @Transactional(readOnly = true)
//    BigDecimal getAverageResponseTime(Long meetingId);
//
//    List<MeetingStatistics> getRecentStatistics(int limit);
//
//    Map<String, Object> getStatisticsOverview(Long meetingId);
//
//    void refreshAllStatistics();
//}