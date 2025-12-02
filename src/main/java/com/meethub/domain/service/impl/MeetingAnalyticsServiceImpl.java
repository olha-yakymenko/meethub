//package com.meethub.domain.service.impl;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.itextpdf.text.*;
//import com.itextpdf.text.pdf.*;
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.enums.*;
//import com.meethub.domain.model.request.ReportFilter;
//import com.meethub.domain.model.response.*;
//import com.meethub.domain.repository.jpa.*;
//import com.meethub.domain.service.MeetingAnalyticsService;
//import com.meethub.exception.BusinessException;
//import com.meethub.exception.ResourceNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.io.ByteArrayOutputStream;
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.TextStyle;
//import java.util.*;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MeetingAnalyticsServiceImpl implements MeetingAnalyticsService {
//
//    private final MeetingRepository meetingRepository;
//    private final MeetingParticipantRepository participantRepository;
//    private final TaskRepository taskRepository;
//    private final TaskAssignmentRepository assignmentRepository;
//    private final MeetingStatisticsRepository statisticsRepository;
//    private final FeedbackRepository feedbackRepository;
//    private final ObjectMapper objectMapper;
//
//    @Override
//    @Transactional
//    public MeetingStatistics generateMeetingStatistics(Long meetingId) {
//        log.info("Generating statistics for meeting: {}", meetingId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
//        List<Task> tasks = taskRepository.findByMeetingId(meetingId);
//
//        // Oblicz podstawowe statystyki
//        int total = participants.size();
//        int confirmed = (int) participants.stream()
//                .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
//                .count();
//        int attended = (int) participants.stream()
//                .filter(p -> p.getStatus() == ParticipationStatus.ATTENDED)
//                .count();
//
//        // Użyj BigDecimal zamiast double
//        BigDecimal attendanceRate = total > 0
//                ? BigDecimal.valueOf((double) attended / total * 100).setScale(2, RoundingMode.HALF_UP)
//                : BigDecimal.ZERO;
//
//        BigDecimal confirmationRate = total > 0
//                ? BigDecimal.valueOf((double) confirmed / total * 100).setScale(2, RoundingMode.HALF_UP)
//                : BigDecimal.ZERO;
//
//        // Średni czas odpowiedzi - jeśli potrzebujesz
//        // BigDecimal avgResponseTime = ...
//
//        // Zaangażowanie
//        BigDecimal engagementScore = calculateSimpleEngagementScore(meetingId, participants);
//
//        // Wykonanie zadań
//        BigDecimal taskCompletionRate = calculateTaskCompletionRate(tasks);
//
//        // Feedback
//        BigDecimal avgRating = feedbackRepository.findAverageRatingByMeetingId(meetingId) != null
//                ? BigDecimal.valueOf(feedbackRepository.findAverageRatingByMeetingId(meetingId)).setScale(2, RoundingMode.HALF_UP)
//                : BigDecimal.ZERO;
//
//        Long feedbackCount = feedbackRepository.countByMeetingId(meetingId);
//
//        // Zapisz statystyki
//        MeetingStatistics stats = MeetingStatistics.builder()
//                .meeting(meeting)
//                .totalParticipants(total)
//                .confirmedParticipants(confirmed)
//                .attendedParticipants(attended)
//                .attendanceRate(attendanceRate)
//                .confirmationRate(confirmationRate)
//                .noShowCount(confirmed - attended)
//                .engagementScore(engagementScore)
//                .taskCompletionRate(taskCompletionRate)
//                .feedbackCount(feedbackCount.intValue())
//                .avgFeedbackRating(avgRating)
//                .generatedAt(LocalDateTime.now())
//                .build();
//
//        return statisticsRepository.save(stats);
//    }
//
//    private BigDecimal calculateSimpleEngagementScore(Long meetingId, List<MeetingParticipant> participants) {
//        if (participants.isEmpty()) return BigDecimal.ZERO;
//
//        BigDecimal total = BigDecimal.ZERO;
//        for (MeetingParticipant p : participants) {
//            BigDecimal score = BigDecimal.ZERO;
//
//            // Obecność (60%)
//            if (p.getStatus() == ParticipationStatus.ATTENDED) {
//                score = score.add(new BigDecimal("60.00"));
//            }
//
//            // Zadania (30%)
//            BigDecimal taskEngagement = BigDecimal.valueOf(calculateTaskEngagementForUser(meetingId, p.getUser().getId()));
//            score = score.add(taskEngagement.multiply(new BigDecimal("0.30")));
//
//            // Feedback (10%)
//            boolean hasFeedback = feedbackRepository.findByMeetingIdAndUserId(meetingId, p.getUser().getId()).isPresent();
//            if (hasFeedback) {
//                score = score.add(new BigDecimal("10.00"));
//            }
//
//            total = total.add(score);
//        }
//
//        // Dzielenie z zaokrągleniem
//        return total.divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);
//    }
//
//    private double calculateTaskEngagementForUser(Long meetingId, Long userId) {
//        List<Task> tasks = taskRepository.findByMeetingId(meetingId);
//        if (tasks.isEmpty()) return 0.0;
//
//        int userTasks = 0;
//        int completed = 0;
//
//        for (Task task : tasks) {
//            List<TaskAssignment> assignments = assignmentRepository.findByTaskId(task.getId());
//            Optional<TaskAssignment> userAssignment = assignments.stream()
//                    .filter(a -> a.getUser().getId().equals(userId))
//                    .findFirst();
//
//            if (userAssignment.isPresent()) {
//                userTasks++;
//                if (userAssignment.get().getStatus() == AssignmentStatus.COMPLETED) {
//                    completed++;
//                }
//            }
//        }
//
//        return userTasks > 0 ? (double) completed / userTasks * 30 : 0.0;
//    }
//
//    private BigDecimal calculateTaskCompletionRate(List<Task> tasks) {
//        if (tasks.isEmpty()) {
//            return BigDecimal.ZERO;
//        }
//
//        BigDecimal totalCompletion = BigDecimal.ZERO;
//        int tasksWithAssignments = 0; // Licznik zadań które mają przypisania
//
//        for (Task task : tasks) {
//            List<TaskAssignment> assignments = assignmentRepository.findByTaskId(task.getId());
//            if (!assignments.isEmpty()) {
//                tasksWithAssignments++;
//                long completed = assignments.stream()
//                        .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED)
//                        .count();
//
//                // Oblicz procent ukończenia dla tego zadania
//                BigDecimal taskCompletionRate = BigDecimal.valueOf(completed)
//                        .multiply(BigDecimal.valueOf(100))
//                        .divide(BigDecimal.valueOf(assignments.size()), 4, RoundingMode.HALF_UP);
//
//                totalCompletion = totalCompletion.add(taskCompletionRate);
//            }
//        }
//
//        if (tasksWithAssignments == 0) {
//            return BigDecimal.ZERO;
//        }
//
//        // Średnia dla wszystkich zadań które mają przypisania
//        return totalCompletion.divide(BigDecimal.valueOf(tasksWithAssignments), 2, RoundingMode.HALF_UP);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public OrganizerReport generateOrganizerReport(Long organizerId, ReportFilter filter) {
//        log.info("Generating report for organizer: {}", organizerId);
//
//        List<Meeting> meetings = meetingRepository.findByOrganizerId(organizerId);
//
//        // Proste filtrowanie
//        if (filter != null && filter.getDateRange() != null) {
//            meetings = meetings.stream()
//                    .filter(m -> {
//                        if (filter.getDateRange().getFrom() != null &&
//                                m.getStartDate().isBefore(filter.getDateRange().getFrom())) {
//                            return false;
//                        }
//                        if (filter.getDateRange().getTo() != null &&
//                                m.getStartDate().isAfter(filter.getDateRange().getTo())) {
//                            return false;
//                        }
//                        return true;
//                    })
//                    .collect(Collectors.toList());
//        }
//
//        // Oblicz podsumowanie
//        ReportSummary summary = calculateSimpleSummary(meetings);
//        List<MonthlyTrend> trends = calculateSimpleTrends(organizerId);
//
//        return OrganizerReport.builder()
//                .organizerId(organizerId)
//                .summary(summary)
//                .monthlyTrends(trends)
//                .generatedAt(LocalDateTime.now())
//                .build();
//    }
//
////    private ReportSummary calculateSimpleSummary(List<Meeting> meetings) {
////        if (meetings.isEmpty()) {
////            return ReportSummary.empty();
////        }
////
////        int totalParticipants = 0;
////        BigDecimal totalAttendance = BigDecimal.ZERO;
////        BigDecimal totalEngagement = BigDecimal.ZERO;
////        int meetingsWithStats = 0;
////
////        for (Meeting meeting : meetings) {
////            Optional<MeetingStatistics> stats = statisticsRepository.findByMeetingId(meeting.getId());
////            if (stats.isPresent()) {
////                meetingsWithStats++;
////                totalParticipants += stats.get().getTotalParticipants();
////
////                // Dodaj BigDecimal
////                totalAttendance = totalAttendance.add(stats.get().getAttendanceRate());
////                totalEngagement = totalEngagement.add(stats.get().getEngagementScore());
////            }
////        }
////
////        if (meetingsWithStats == 0) {
////            return ReportSummary.empty();
////        }
////
////        // Oblicz średnie z BigDecimal i konwertuj do double
////        BigDecimal avgAttendance = totalAttendance
////                .divide(BigDecimal.valueOf(meetingsWithStats), 2, RoundingMode.HALF_UP);
////
////        BigDecimal avgEngagement = totalEngagement
////                .divide(BigDecimal.valueOf(meetingsWithStats), 2, RoundingMode.HALF_UP);
////
////        return ReportSummary.builder()
////                .totalMeetings(meetings.size())
////                .totalParticipants(totalParticipants)
////                .avgAttendanceRate(avgAttendance.doubleValue()) // konwersja do double
////                .avgEngagementScore(avgEngagement.doubleValue()) // konwersja do double
////                .build();
////    }
//
//    private ReportSummary calculateSimpleSummary(List<Meeting> meetings) {
//        if (meetings.isEmpty()) {
//            return ReportSummary.empty();
//        }
//
//        int totalParticipants = 0;
//        BigDecimal totalAttendance = BigDecimal.ZERO;
//        BigDecimal totalEngagement = BigDecimal.ZERO;
//        int meetingsWithStats = 0;
//
//        for (Meeting meeting : meetings) {
//            Optional<MeetingStatistics> stats = statisticsRepository.findByMeetingId(meeting.getId());
//            if (stats.isPresent()) {
//                meetingsWithStats++;
//                totalParticipants += stats.get().getTotalParticipants();
//
//                // Teraz to będzie działać, bo getAttendanceRate() zwraca BigDecimal
//                BigDecimal attendanceRate = stats.get().getAttendanceRate();
//                BigDecimal engagementScore = stats.get().getEngagementScore();
//
//                if (attendanceRate != null) {
//                    totalAttendance = totalAttendance.add(attendanceRate);
//                }
//
//                if (engagementScore != null) {
//                    totalEngagement = totalEngagement.add(engagementScore);
//                }
//            }
//        }
//
//        if (meetingsWithStats == 0) {
//            return ReportSummary.empty();
//        }
//
//        // Oblicz średnie
//        BigDecimal avgAttendance = totalAttendance
//                .divide(BigDecimal.valueOf(meetingsWithStats), 2, RoundingMode.HALF_UP);
//
//        BigDecimal avgEngagement = totalEngagement
//                .divide(BigDecimal.valueOf(meetingsWithStats), 2, RoundingMode.HALF_UP);
//
//        // Wybierz OPCJĘ A lub B:
//
//        // OPCJA A: Jeśli ReportSummary ma double (bez zmiany ReportSummary)
//        return ReportSummary.builder()
//                .totalMeetings(meetings.size())
//                .totalParticipants(totalParticipants)
//                .avgAttendanceRate(avgAttendance.doubleValue())
//                .avgEngagementScore(avgEngagement.doubleValue())
//                .build();
//
//        // OPCJA B: Jeśli zmienisz ReportSummary na BigDecimal
//        // return ReportSummary.builder()
//        //         .totalMeetings(meetings.size())
//        //         .totalParticipants(totalParticipants)
//        //         .avgAttendanceRate(avgAttendance)
//        //         .avgEngagementScore(avgEngagement)
//        //         .build();
//    }
//
//    private List<MonthlyTrend> calculateSimpleTrends(Long organizerId) {
//        List<MonthlyTrend> trends = new ArrayList<>();
//        LocalDateTime now = LocalDateTime.now();
//
//        // Ostatnie 6 miesięcy
//        for (int i = 5; i >= 0; i--) {
//            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0);
//            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
//
//            List<Meeting> monthlyMeetings = meetingRepository.findByOrganizerIdAndDateRange(
//                    organizerId, monthStart, monthEnd);
//
//            if (!monthlyMeetings.isEmpty()) {
//                // ZMIENIONE: BigDecimal zamiast double
//                BigDecimal totalAttendance = BigDecimal.ZERO;
//                int meetingsWithStats = 0;
//
//                for (Meeting meeting : monthlyMeetings) {
//                    Optional<MeetingStatistics> stats = statisticsRepository.findByMeetingId(meeting.getId());
//                    if (stats.isPresent()) {
//                        meetingsWithStats++;
//                        // ZMIENIONE: dodajemy BigDecimal
//                        if (stats.get().getAttendanceRate() != null) {
//                            totalAttendance = totalAttendance.add(stats.get().getAttendanceRate());
//                        }
//                    }
//                }
//
//                // Oblicz średnią
//                double avgAttendance = 0.0;
//                if (meetingsWithStats > 0) {
//                    // Dzielenie BigDecimal i konwersja do double
//                    BigDecimal avgAttendanceBigDecimal = totalAttendance
//                            .divide(BigDecimal.valueOf(meetingsWithStats), 2, RoundingMode.HALF_UP);
//                    avgAttendance = avgAttendanceBigDecimal.doubleValue();
//                }
//
//                String monthName = monthStart.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pl"));
//
//                trends.add(MonthlyTrend.builder()
//                        .monthName(monthName)
//                        .meetingsCount(monthlyMeetings.size())
//                        .avgAttendance(avgAttendance) // nadal double w MonthlyTrend
//                        .build());
//            }
//        }
//
//        return trends;
//    }
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public byte[] exportReportToCsv(Long organizerId, ReportFilter filter) {
//        OrganizerReport report = generateOrganizerReport(organizerId, filter);
//        return generateCsv(report);
//    }
//
//    private byte[] generateCsv(OrganizerReport report) {
//        StringBuilder csv = new StringBuilder();
//
//        csv.append("=== RAPORT ORGANIZATORA ===\n");
//        csv.append("Organizator ID: ").append(report.getOrganizerId()).append("\n");
//        csv.append("Wygenerowano: ").append(report.getGeneratedAt().format(
//                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
//
//        csv.append("=== PODSUMOWANIE ===\n");
//        csv.append("Liczba spotkań,").append(report.getSummary().getTotalMeetings()).append("\n");
//        csv.append("Łączna liczba uczestników,").append(report.getSummary().getTotalParticipants()).append("\n");
//        csv.append("Średnia frekwencja,").append(String.format("%.2f", report.getSummary().getAvgAttendanceRate())).append("%\n");
//        csv.append("Średnie zaangażowanie,").append(String.format("%.2f", report.getSummary().getAvgEngagementScore())).append(" pkt\n\n");
//
//        csv.append("=== TRENDY MIESIĘCZNE ===\n");
//        csv.append("Miesiąc,Liczba spotkań,Średnia frekwencja\n");
//
//        for (MonthlyTrend trend : report.getMonthlyTrends()) {
//            csv.append(trend.getMonthName()).append(",")
//                    .append(trend.getMeetingsCount()).append(",")
//                    .append(String.format("%.2f", trend.getAvgAttendance())).append("%\n");
//        }
//
//        return csv.toString().getBytes(StandardCharsets.UTF_8);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public byte[] exportReportToPdf(Long organizerId, ReportFilter filter) {
//        OrganizerReport report = generateOrganizerReport(organizerId, filter);
//        return generatePdf(report);
//    }
//
//    private byte[] generatePdf(OrganizerReport report) {
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            Document document = new Document(PageSize.A4);
//            PdfWriter.getInstance(document, baos);
//            document.open();
//
//            // Tytuł
//            Paragraph title = new Paragraph("RAPORT ORGANIZATORA SPOTKAŃ",
//                    new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD));
//            title.setAlignment(Element.ALIGN_CENTER);
//            title.setSpacingAfter(20);
//            document.add(title);
//
//            // Informacje
//            document.add(new Paragraph("Organizator ID: " + report.getOrganizerId()));
//            document.add(new Paragraph("Wygenerowano: " + report.getGeneratedAt().format(
//                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
//            document.add(Chunk.NEWLINE);
//
//            // Podsumowanie
//            document.add(new Paragraph("PODSUMOWANIE",
//                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
//
//            PdfPTable table = new PdfPTable(2);
//            table.setWidthPercentage(100);
//
//            addTableRow(table, "Liczba spotkań", String.valueOf(report.getSummary().getTotalMeetings()));
//            addTableRow(table, "Łączna liczba uczestników", String.valueOf(report.getSummary().getTotalParticipants()));
//            addTableRow(table, "Średnia frekwencja", String.format("%.2f", report.getSummary().getAvgAttendanceRate()) + "%");
//            addTableRow(table, "Średnie zaangażowanie", String.format("%.2f", report.getSummary().getAvgEngagementScore()) + " pkt");
//
//            document.add(table);
//            document.close();
//
//            return baos.toByteArray();
//        } catch (Exception e) {
//            log.error("Error generating PDF", e);
//            throw new BusinessException("Błąd podczas generowania PDF");
//        }
//    }
//
//    private void addTableRow(PdfPTable table, String label, String value) {
//        table.addCell(new PdfPCell(new Phrase(label)));
//        table.addCell(new PdfPCell(new Phrase(value)));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingStatistics> getMeetingStatisticsByOrganizer(Long organizerId) {
//        List<Meeting> meetings = meetingRepository.findByOrganizerId(organizerId);
//        return meetings.stream()
//                .map(m -> statisticsRepository.findByMeetingId(m.getId()).orElse(null))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    // ========== NOWE METODY DLA EKSPORTU STATYSTYK SPOTKANIA ==========
//
//    @Transactional(readOnly = true)
//    @Override
//    public byte[] exportMeetingStatisticsToCsv(Long meetingId) {
//        MeetingStatistics stats = generateMeetingStatistics(meetingId);
//        return generateMeetingCsv(stats);
//    }
//
//    @Transactional(readOnly = true)
//    @Override
//    public byte[] exportMeetingStatisticsToPdf(Long meetingId) {
//        MeetingStatistics stats = generateMeetingStatistics(meetingId);
//        return generateMeetingPdf(stats);
//    }
//
//    private byte[] generateMeetingCsv(MeetingStatistics stats) {
//        StringBuilder csv = new StringBuilder();
//
//        csv.append("=== RAPORT STATYSTYK SPOTKANIA ===\n");
//        csv.append("Spotkanie ID: ").append(stats.getMeeting().getId()).append("\n");
//        csv.append("Tytuł: ").append(stats.getMeeting().getTitle()).append("\n");
//        csv.append("Data spotkania: ").append(stats.getMeeting().getStartDate().format(
//                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
//        csv.append("Organizator: ").append(stats.getMeeting().getOrganizer().getFirstName())
//                .append(" ").append(stats.getMeeting().getOrganizer().getLastName()).append("\n");
//        csv.append("Wygenerowano: ").append(stats.getGeneratedAt().format(
//                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
//
//        csv.append("=== PODSTAWOWE STATYSTYKI ===\n");
//        csv.append("Kategoria,Wartość\n");
//        csv.append("Łączna liczba uczestników,").append(stats.getTotalParticipants()).append("\n");
//        csv.append("Potwierdzeni uczestnicy,").append(stats.getConfirmedParticipants()).append("\n");
//        csv.append("Uczestnicy obecni,").append(stats.getAttendedParticipants()).append("\n");
//        csv.append("Frekwencja,").append(String.format("%.2f", stats.getAttendanceRate())).append("%\n");
//        csv.append("Wskaźnik potwierdzeń,").append(String.format("%.2f", stats.getConfirmationRate())).append("%\n");
//        csv.append("Liczba nieobecnych,").append(stats.getNoShowCount()).append("\n");
//        csv.append("Wskaźnik zaangażowania,").append(String.format("%.2f", stats.getEngagementScore())).append("\n");
//        csv.append("Wykonanie zadań,").append(String.format("%.2f", stats.getTaskCompletionRate())).append("%\n");
//        csv.append("Liczba ocen,").append(stats.getFeedbackCount()).append("\n");
//
//        if (stats.getAvgFeedbackRating() != null && stats.getAvgFeedbackRating().compareTo(BigDecimal.ZERO) > 0) {
//            csv.append("Średnia ocena,").append(String.format("%.2f", stats.getAvgFeedbackRating())).append("/5\n");
//        } else {
//            csv.append("Średnia ocena,Brak ocen\n");
//        }
//
//        return csv.toString().getBytes(StandardCharsets.UTF_8);
//    }
//
//    private byte[] generateMeetingPdf(MeetingStatistics stats) {
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            Document document = new Document(PageSize.A4);
//            PdfWriter.getInstance(document, baos);
//            document.open();
//
//            // Tytuł
//            Paragraph title = new Paragraph("RAPORT STATYSTYK SPOTKANIA",
//                    new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD));
//            title.setAlignment(Element.ALIGN_CENTER);
//            title.setSpacingAfter(20);
//            document.add(title);
//
//            // Informacje o spotkaniu
//            document.add(new Paragraph("Tytuł: " + stats.getMeeting().getTitle()));
//            document.add(new Paragraph("Data spotkania: " +
//                    stats.getMeeting().getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
//            document.add(new Paragraph("Organizator: " +
//                    stats.getMeeting().getOrganizer().getFirstName() + " " +
//                    stats.getMeeting().getOrganizer().getLastName()));
//            document.add(new Paragraph("Wygenerowano: " + stats.getGeneratedAt().format(
//                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
//            document.add(Chunk.NEWLINE);
//
//            // Statystyki
//            document.add(new Paragraph("STATYSTYKI",
//                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
//
//            PdfPTable table = new PdfPTable(2);
//            table.setWidthPercentage(100);
//
//            addTableRow(table, "Łączna liczba uczestników", String.valueOf(stats.getTotalParticipants()));
//            addTableRow(table, "Potwierdzeni uczestnicy", String.valueOf(stats.getConfirmedParticipants()));
//            addTableRow(table, "Uczestnicy obecni", String.valueOf(stats.getAttendedParticipants()));
//            addTableRow(table, "Frekwencja", String.format("%.2f", stats.getAttendanceRate()) + "%");
//            addTableRow(table, "Wskaźnik potwierdzeń", String.format("%.2f", stats.getConfirmationRate()) + "%");
//            addTableRow(table, "Nieobecni", String.valueOf(stats.getNoShowCount()));
//            addTableRow(table, "Wskaźnik zaangażowania", String.format("%.2f", stats.getEngagementScore()));
//            addTableRow(table, "Wykonanie zadań", String.format("%.2f", stats.getTaskCompletionRate()) + "%");
//            addTableRow(table, "Liczba ocen", String.valueOf(stats.getFeedbackCount()));
//
//            if (stats.getAvgFeedbackRating() != null && stats.getAvgFeedbackRating().compareTo(BigDecimal.ZERO) > 0) {
//                addTableRow(table, "Średnia ocena", String.format("%.2f", stats.getAvgFeedbackRating()) + "/5");
//            } else {
//                addTableRow(table, "Średnia ocena", "Brak ocen");
//            }
//
//            document.add(table);
//            document.close();
//
//            return baos.toByteArray();
//        } catch (Exception e) {
//            log.error("Error generating meeting PDF", e);
//            throw new BusinessException("Błąd podczas generowania PDF dla spotkania");
//        }
//    }
//
//    // ========== METODY POMOCNICZE DO PRZELICZEŃ ==========
//
//    @Transactional(readOnly = true)
//    @Override
//    public BigDecimal getAverageResponseTime(Long meetingId) {
//        // Ta metoda może być używana do obliczania średniego czasu odpowiedzi
//        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
//
//        long totalResponseTime = 0;
//        int participantsWithResponse = 0;
//
//        // Przykład obliczania średniego czasu odpowiedzi
//        for (MeetingParticipant participant : participants) {
//            // Zakładając, że masz pola invitedAt i responseAt
////            if (participant.getInvitedAt() != null && participant.getResponseAt() != null) {
////                long responseTime = java.time.Duration.between(
////                        participant.getInvitedAt(), participant.getResponseAt()).toHours();
////                totalResponseTime += responseTime;
////                participantsWithResponse++;
////            }
//        }
//
//        if (participantsWithResponse > 0) {
//            return BigDecimal.valueOf((double) totalResponseTime / participantsWithResponse)
//                    .setScale(1, RoundingMode.HALF_UP);
//        }
//
//        return BigDecimal.ZERO;
//    }
//}








package com.meethub.domain.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.*;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.MeetingAnalyticsService;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingAnalyticsServiceImpl implements MeetingAnalyticsService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final MeetingStatisticsRepository statisticsRepository;
    private final FeedbackRepository feedbackRepository;

    // ========== INTERFACE IMPLEMENTATIONS ==========

    @Override
    @Transactional
    public MeetingStatistics generateMeetingStatistics(Long meetingId) {
        log.info("Generating/updating statistics for meeting: {}", meetingId);

        // Znajdź istniejące statystyki lub utwórz nowe
        MeetingStatistics stats = statisticsRepository.findByMeetingId(meetingId)
                .orElseGet(() -> {
                    Meeting meeting = meetingRepository.findById(meetingId)
                            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

                    MeetingStatistics newStats = new MeetingStatistics();
                    newStats.setMeeting(meeting);
                    newStats.setCreatedAt(LocalDateTime.now());
                    log.info("Creating new statistics for meeting: {}", meetingId);
                    return newStats;
                });

        // Pobierz dane do obliczeń
        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
        List<Task> tasks = taskRepository.findByMeetingId(meetingId);

        // Oblicz podstawowe statystyki
        int total = participants.size();
        int confirmed = (int) participants.stream()
                .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
                .count();
        int attended = (int) participants.stream()
                .filter(p -> p.getStatus() == ParticipationStatus.ATTENDED)
                .count();

        // Oblicz stawki
        BigDecimal attendanceRate = total > 0
                ? BigDecimal.valueOf((double) attended / total * 100)
                .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal confirmationRate = total > 0
                ? BigDecimal.valueOf((double) confirmed / total * 100)
                .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Oblicz zaangażowanie
        BigDecimal engagementScore = calculateEngagementScore(meetingId, participants);

        // Oblicz wykonanie zadań
        BigDecimal taskCompletionRate = calculateTaskCompletionRate(tasks);

        // Oblicz feedback
        Long feedbackCount = feedbackRepository.countByMeetingId(meetingId);
        BigDecimal avgRating = calculateAverageRating(meetingId);

        // Oblicz średni czas odpowiedzi - jeśli pole istnieje w MeetingParticipant
        BigDecimal avgResponseTime = calculateAverageResponseTime(meetingId, participants);

        // Aktualizuj statystyki
        stats.setTotalParticipants(total);
        stats.setConfirmedParticipants(confirmed);
        stats.setAttendedParticipants(attended);
        stats.setAttendanceRate(attendanceRate);
        stats.setConfirmationRate(confirmationRate);
        stats.setNoShowCount(Math.max(0, confirmed - attended));
        stats.setEngagementScore(engagementScore);
        stats.setTaskCompletionRate(taskCompletionRate);
        stats.setFeedbackCount(feedbackCount.intValue());
        stats.setAvgFeedbackRating(avgRating);
        stats.setAvgResponseTimeHours(avgResponseTime);
        stats.setGeneratedAt(LocalDateTime.now());
        stats.setUpdatedAt(LocalDateTime.now());

        log.info("Statistics saved for meeting {}: attendance={}%, engagement={}",
                meetingId, attendanceRate, engagementScore);

        return statisticsRepository.save(stats);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<MeetingStatistics> getMeetingStatistics(Long meetingId) {
        return statisticsRepository.findByMeetingId(meetingId);
    }

    @Transactional
    @Override
    public void deleteMeetingStatistics(Long meetingId) {
        statisticsRepository.findByMeetingId(meetingId)
                .ifPresent(stats -> {
                    statisticsRepository.delete(stats);
                    log.info("Deleted statistics for meeting: {}", meetingId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizerReport generateOrganizerReport(Long organizerId, ReportFilter filter) {
        log.info("Generating organizer report for organizer: {}", organizerId);

        List<Meeting> meetings = meetingRepository.findByOrganizerId(organizerId);

        // Filtrowanie według daty - UŻYJ DateRange
        if (filter != null && filter.getDateRange() != null) {
            LocalDateTime startDate = filter.getDateRange().getFrom();
            LocalDateTime endDate = filter.getDateRange().getTo();
            meetings = filterMeetingsByDateRange(meetings, startDate, endDate);
        }

        ReportSummary summary = calculateOrganizerSummary(meetings);
        List<MonthlyTrend> trends = calculateMonthlyTrends(organizerId);

        return OrganizerReport.builder()
                .organizerId(organizerId)
                .summary(summary)
                .monthlyTrends(trends)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingStatistics> getMeetingStatisticsByOrganizer(Long organizerId) {
        List<Meeting> meetings = meetingRepository.findByOrganizerId(organizerId);
        return meetings.stream()
                .map(meeting -> statisticsRepository.findByMeetingId(meeting.getId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportToCsv(Long organizerId, ReportFilter filter) {
        OrganizerReport report = generateOrganizerReport(organizerId, filter);
        return generateOrganizerCsv(report);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportToPdf(Long organizerId, ReportFilter filter) {
        OrganizerReport report = generateOrganizerReport(organizerId, filter);
        return generateOrganizerPdf(report);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportMeetingStatisticsToCsv(Long meetingId) {
        MeetingStatistics stats = statisticsRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statistics not found for meeting: " + meetingId));
        return generateMeetingCsv(stats);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportMeetingStatisticsToPdf(Long meetingId) {
        MeetingStatistics stats = statisticsRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statistics not found for meeting: " + meetingId));
        return generateMeetingPdf(stats);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAverageResponseTime(Long meetingId) {
        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
        return calculateAverageResponseTime(meetingId, participants);
    }

    // ========== NOWE METODY Z INTERFEJSU ==========

    @Override
    @Transactional(readOnly = true)
    public List<MeetingStatistics> getRecentStatistics(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return statisticsRepository.findTopNByOrderByGeneratedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatisticsOverview(Long meetingId) {
        Optional<MeetingStatistics> statsOpt = statisticsRepository.findByMeetingId(meetingId);

        Map<String, Object> overview = new HashMap<>();

        if (statsOpt.isPresent()) {
            MeetingStatistics stats = statsOpt.get();
            overview.put("meetingId", meetingId);
            overview.put("meetingTitle", stats.getMeeting().getTitle());
            overview.put("attendanceRate", stats.getAttendanceRate());
            overview.put("engagementScore", stats.getEngagementScore());
            overview.put("feedbackCount", stats.getFeedbackCount());
            overview.put("generatedAt", stats.getGeneratedAt());

            // Dodaj ocenę jakości
            String grade = calculateGrade(stats);
            overview.put("grade", grade);
        }

        return overview;
    }

    @Override
    @Transactional
    public void refreshAllStatistics() {
        log.info("Refreshing statistics for all meetings...");

        List<Meeting> meetings = meetingRepository.findAll();
        int updated = 0;

        for (Meeting meeting : meetings) {
            try {
                generateMeetingStatistics(meeting.getId());
                updated++;
            } catch (Exception e) {
                log.error("Error refreshing statistics for meeting {}: {}",
                        meeting.getId(), e.getMessage());
            }
        }

        log.info("Statistics refreshed for {}/{} meetings", updated, meetings.size());
    }

    // ========== METODY POMOCNICZE DO OBLICZEŃ ==========

    private BigDecimal calculateEngagementScore(Long meetingId, List<MeetingParticipant> participants) {
        if (participants.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        int participantsWithData = 0;

        for (MeetingParticipant participant : participants) {
            BigDecimal participantScore = BigDecimal.ZERO;

            // Obecność (50%)
            if (participant.getStatus() == ParticipationStatus.ATTENDED) {
                participantScore = participantScore.add(new BigDecimal("50.0"));
            }

            // Zadania (30%)
            BigDecimal taskEngagement = calculateTaskEngagementForUser(meetingId, participant.getUser().getId());
            participantScore = participantScore.add(taskEngagement.multiply(new BigDecimal("0.30")));

            // Feedback (20%)
            boolean hasFeedback = feedbackRepository.findByMeetingIdAndUserId(meetingId,
                    participant.getUser().getId()).isPresent();
            if (hasFeedback) {
                participantScore = participantScore.add(new BigDecimal("20.0"));
            }

            totalScore = totalScore.add(participantScore);
            participantsWithData++;
        }

        if (participantsWithData == 0) {
            return BigDecimal.ZERO;
        }

        return totalScore.divide(BigDecimal.valueOf(participantsWithData), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTaskEngagementForUser(Long meetingId, Long userId) {
        List<Task> tasks = taskRepository.findByMeetingId(meetingId);
        if (tasks.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int userTasks = 0;
        int completedTasks = 0;

        for (Task task : tasks) {
            List<TaskAssignment> assignments = assignmentRepository.findByTaskId(task.getId());
            Optional<TaskAssignment> userAssignment = assignments.stream()
                    .filter(a -> a.getUser().getId().equals(userId))
                    .findFirst();

            if (userAssignment.isPresent()) {
                userTasks++;
                if (userAssignment.get().getStatus() == AssignmentStatus.COMPLETED) {
                    completedTasks++;
                }
            }
        }

        if (userTasks == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf((double) completedTasks / userTasks * 30)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTaskCompletionRate(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalCompletion = BigDecimal.ZERO;
        int tasksWithAssignments = 0;

        for (Task task : tasks) {
            List<TaskAssignment> assignments = assignmentRepository.findByTaskId(task.getId());
            if (!assignments.isEmpty()) {
                tasksWithAssignments++;
                long completed = assignments.stream()
                        .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED)
                        .count();

                BigDecimal taskCompletion = BigDecimal.valueOf(completed)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(assignments.size()), 4, RoundingMode.HALF_UP);

                totalCompletion = totalCompletion.add(taskCompletion);
            }
        }

        if (tasksWithAssignments == 0) {
            return BigDecimal.ZERO;
        }

        return totalCompletion.divide(BigDecimal.valueOf(tasksWithAssignments), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageRating(Long meetingId) {
        Double avgRating = feedbackRepository.findAverageRatingByMeetingId(meetingId);
        if (avgRating == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageResponseTime(Long meetingId, List<MeetingParticipant> participants) {
        // Jeśli MeetingParticipant nie ma pól invitedAt i respondedAt, zwróć 0
        // Możesz dodać te pola później lub obliczyć na podstawie innych danych

        // Tymczasowe rozwiązanie - zwróć 0
        return BigDecimal.ZERO;

        /*
        // Kiedy dodasz pola do MeetingParticipant:
        if (participants == null || participants.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalResponseTimeHours = 0;
        int participantsWithResponse = 0;

        for (MeetingParticipant participant : participants) {
            LocalDateTime invitedAt = participant.getInvitedAt();
            LocalDateTime respondedAt = participant.getRespondedAt();

            if (invitedAt != null && respondedAt != null) {
                long responseTimeHours = java.time.Duration.between(invitedAt, respondedAt).toHours();
                totalResponseTimeHours += responseTimeHours;
                participantsWithResponse++;
            }
        }

        if (participantsWithResponse == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf((double) totalResponseTimeHours / participantsWithResponse)
                .setScale(1, RoundingMode.HALF_UP);
        */
    }

    private List<Meeting> filterMeetingsByDateRange(List<Meeting> meetings,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate) {
        return meetings.stream()
                .filter(meeting -> {
                    boolean afterStart = startDate == null ||
                            !meeting.getStartDate().isBefore(startDate);
                    boolean beforeEnd = endDate == null ||
                            !meeting.getStartDate().isAfter(endDate);
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
    }

    private ReportSummary calculateOrganizerSummary(List<Meeting> meetings) {
        if (meetings.isEmpty()) {
            return ReportSummary.empty();
        }

        int totalParticipants = 0;
        BigDecimal totalAttendance = BigDecimal.ZERO;
        BigDecimal totalEngagement = BigDecimal.ZERO;
        int meetingsWithStats = 0;

        for (Meeting meeting : meetings) {
            Optional<MeetingStatistics> stats = statisticsRepository.findByMeetingId(meeting.getId());
            if (stats.isPresent()) {
                meetingsWithStats++;
                totalParticipants += stats.get().getTotalParticipants();

                if (stats.get().getAttendanceRate() != null) {
                    totalAttendance = totalAttendance.add(stats.get().getAttendanceRate());
                }

                if (stats.get().getEngagementScore() != null) {
                    totalEngagement = totalEngagement.add(stats.get().getEngagementScore());
                }
            }
        }

        if (meetingsWithStats == 0) {
            return ReportSummary.empty();
        }

        BigDecimal avgAttendance = totalAttendance
                .divide(BigDecimal.valueOf(meetingsWithStats), 2, RoundingMode.HALF_UP);

        BigDecimal avgEngagement = totalEngagement
                .divide(BigDecimal.valueOf(meetingsWithStats), 2, RoundingMode.HALF_UP);

        return ReportSummary.builder()
                .totalMeetings(meetings.size())
                .totalParticipants(totalParticipants)
                .avgAttendanceRate(avgAttendance.doubleValue())
                .avgEngagementScore(avgEngagement.doubleValue())
                .build();
    }

    private List<MonthlyTrend> calculateMonthlyTrends(Long organizerId) {
        List<MonthlyTrend> trends = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Ostatnie 6 miesięcy
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i)
                    .withDayOfMonth(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

            List<Meeting> monthlyMeetings = meetingRepository.findByOrganizerIdAndDateRange(
                    organizerId, monthStart, monthEnd);

            if (!monthlyMeetings.isEmpty()) {
                BigDecimal totalAttendance = BigDecimal.ZERO;
                int meetingsWithStats = 0;

                for (Meeting meeting : monthlyMeetings) {
                    Optional<MeetingStatistics> stats = statisticsRepository.findByMeetingId(meeting.getId());
                    if (stats.isPresent() && stats.get().getAttendanceRate() != null) {
                        meetingsWithStats++;
                        totalAttendance = totalAttendance.add(stats.get().getAttendanceRate());
                    }
                }

                double avgAttendance = 0.0;
                if (meetingsWithStats > 0) {
                    BigDecimal avgAttendanceBigDecimal = totalAttendance
                            .divide(BigDecimal.valueOf(meetingsWithStats), 2, RoundingMode.HALF_UP);
                    avgAttendance = avgAttendanceBigDecimal.doubleValue();
                }

                String monthName = monthStart.getMonth()
                        .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pl"))
                        + " " + monthStart.getYear();

                trends.add(MonthlyTrend.builder()
                        .monthName(monthName)
                        .meetingsCount(monthlyMeetings.size())
                        .avgAttendance(avgAttendance)
                        .build());
            }
        }

        return trends;
    }

    // ========== METODY DO GENEROWANIA CSV ==========

    private byte[] generateOrganizerCsv(OrganizerReport report) {
        StringBuilder csv = new StringBuilder();

        csv.append("=== RAPORT ORGANIZATORA ===\n");
        csv.append("Organizator ID: ").append(report.getOrganizerId()).append("\n");
        csv.append("Wygenerowano: ").append(report.getGeneratedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        csv.append("=== PODSUMOWANIE ===\n");
        csv.append("Kategoria,Wartość\n");
        csv.append("Liczba spotkań,").append(report.getSummary().getTotalMeetings()).append("\n");
        csv.append("Łączna liczba uczestników,").append(report.getSummary().getTotalParticipants()).append("\n");
        csv.append("Średnia frekwencja,").append(String.format("%.2f", report.getSummary().getAvgAttendanceRate())).append("%\n");
        csv.append("Średnie zaangażowanie,").append(String.format("%.2f", report.getSummary().getAvgEngagementScore())).append("\n\n");

        csv.append("=== TRENDY MIESIĘCZNE ===\n");
        csv.append("Miesiąc,Liczba spotkań,Średnia frekwencja\n");

        for (MonthlyTrend trend : report.getMonthlyTrends()) {
            csv.append(trend.getMonthName()).append(",")
                    .append(trend.getMeetingsCount()).append(",")
                    .append(String.format("%.2f", trend.getAvgAttendance())).append("%\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateMeetingCsv(MeetingStatistics stats) {
        StringBuilder csv = new StringBuilder();

        csv.append("=== RAPORT STATYSTYK SPOTKANIA ===\n");
        csv.append("Spotkanie ID: ").append(stats.getMeeting().getId()).append("\n");
        csv.append("Tytuł: ").append(stats.getMeeting().getTitle()).append("\n");
        csv.append("Data spotkania: ").append(stats.getMeeting().getStartDate().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        csv.append("Organizator: ").append(stats.getMeeting().getOrganizer().getFirstName())
                .append(" ").append(stats.getMeeting().getOrganizer().getLastName()).append("\n");
        csv.append("Wygenerowano: ").append(stats.getGeneratedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        csv.append("=== STATYSTYKI ===\n");
        csv.append("Kategoria,Wartość\n");
        csv.append("Łączna liczba uczestników,").append(stats.getTotalParticipants()).append("\n");
        csv.append("Potwierdzeni uczestnicy,").append(stats.getConfirmedParticipants()).append("\n");
        csv.append("Uczestnicy obecni,").append(stats.getAttendedParticipants()).append("\n");
        csv.append("Frekwencja,").append(String.format("%.2f", stats.getAttendanceRate())).append("%\n");
        csv.append("Wskaźnik potwierdzeń,").append(String.format("%.2f", stats.getConfirmationRate())).append("%\n");
        csv.append("Nieobecni,").append(stats.getNoShowCount()).append("\n");
        csv.append("Wskaźnik zaangażowania,").append(String.format("%.2f", stats.getEngagementScore())).append("\n");
        csv.append("Wykonanie zadań,").append(String.format("%.2f", stats.getTaskCompletionRate())).append("%\n");
        csv.append("Liczba ocen,").append(stats.getFeedbackCount()).append("\n");

        if (stats.getAvgFeedbackRating() != null && stats.getAvgFeedbackRating().compareTo(BigDecimal.ZERO) > 0) {
            csv.append("Średnia ocena,").append(String.format("%.2f", stats.getAvgFeedbackRating())).append("/5\n");
        } else {
            csv.append("Średnia ocena,Brak ocen\n");
        }

        if (stats.getAvgResponseTimeHours() != null && stats.getAvgResponseTimeHours().compareTo(BigDecimal.ZERO) > 0) {
            csv.append("Średni czas odpowiedzi,").append(String.format("%.1f", stats.getAvgResponseTimeHours())).append(" godzin\n");
        } else {
            csv.append("Średni czas odpowiedzi,Brak danych\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ========== METODY DO GENEROWANIA PDF ==========

    private byte[] generateOrganizerPdf(OrganizerReport report) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            addOrganizerPdfContent(document, report);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating organizer PDF", e);
            throw new BusinessException("Błąd podczas generowania raportu PDF");
        }
    }

    private void addOrganizerPdfContent(Document document, OrganizerReport report) throws DocumentException {
        // Tytuł
        Paragraph title = new Paragraph("RAPORT ORGANIZATORA SPOTKAŃ",
                new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Informacje
        document.add(new Paragraph("Organizator ID: " + report.getOrganizerId()));
        document.add(new Paragraph("Wygenerowano: " + report.getGeneratedAt().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        document.add(Chunk.NEWLINE);

        // Podsumowanie
        document.add(new Paragraph("PODSUMOWANIE",
                new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);
        summaryTable.setSpacingAfter(20);

        addTableRow(summaryTable, "Liczba spotkań", String.valueOf(report.getSummary().getTotalMeetings()));
        addTableRow(summaryTable, "Łączna liczba uczestników", String.valueOf(report.getSummary().getTotalParticipants()));
        addTableRow(summaryTable, "Średnia frekwencja", String.format("%.2f", report.getSummary().getAvgAttendanceRate()) + "%");
        addTableRow(summaryTable, "Średnie zaangażowanie", String.format("%.2f", report.getSummary().getAvgEngagementScore()) + " pkt");

        document.add(summaryTable);

        // Trendy
        if (!report.getMonthlyTrends().isEmpty()) {
            document.add(new Paragraph("TRENDY MIESIĘCZNE",
                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));

            PdfPTable trendsTable = new PdfPTable(3);
            trendsTable.setWidthPercentage(100);
            trendsTable.setSpacingBefore(10);

            addTableHeader(trendsTable, "Miesiąc");
            addTableHeader(trendsTable, "Liczba spotkań");
            addTableHeader(trendsTable, "Średnia frekwencja");

            for (MonthlyTrend trend : report.getMonthlyTrends()) {
                trendsTable.addCell(trend.getMonthName());
                trendsTable.addCell(String.valueOf(trend.getMeetingsCount()));
                trendsTable.addCell(String.format("%.2f", trend.getAvgAttendance()) + "%");
            }

            document.add(trendsTable);
        }
    }

    private byte[] generateMeetingPdf(MeetingStatistics stats) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            addMeetingPdfContent(document, stats);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating meeting PDF", e);
            throw new BusinessException("Błąd podczas generowania raportu PDF dla spotkania");
        }
    }

    private void addMeetingPdfContent(Document document, MeetingStatistics stats) throws DocumentException {
        // Tytuł
        Paragraph title = new Paragraph("RAPORT STATYSTYK SPOTKANIA",
                new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Informacje o spotkaniu
        document.add(new Paragraph("Tytuł: " + stats.getMeeting().getTitle()));
        document.add(new Paragraph("Data: " +
                stats.getMeeting().getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        document.add(new Paragraph("Organizator: " +
                stats.getMeeting().getOrganizer().getFirstName() + " " +
                stats.getMeeting().getOrganizer().getLastName()));
        document.add(new Paragraph("Wygenerowano: " + stats.getGeneratedAt().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        document.add(Chunk.NEWLINE);

        // Statystyki
        document.add(new Paragraph("STATYSTYKI",
                new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        addTableRow(table, "Łączna liczba uczestników", String.valueOf(stats.getTotalParticipants()));
        addTableRow(table, "Potwierdzeni uczestnicy", String.valueOf(stats.getConfirmedParticipants()));
        addTableRow(table, "Uczestnicy obecni", String.valueOf(stats.getAttendedParticipants()));
        addTableRow(table, "Frekwencja", String.format("%.2f", stats.getAttendanceRate()) + "%");
        addTableRow(table, "Wskaźnik potwierdzeń", String.format("%.2f", stats.getConfirmationRate()) + "%");
        addTableRow(table, "Nieobecni", String.valueOf(stats.getNoShowCount()));
        addTableRow(table, "Wskaźnik zaangażowania", String.format("%.2f", stats.getEngagementScore()) + " pkt");
        addTableRow(table, "Wykonanie zadań", String.format("%.2f", stats.getTaskCompletionRate()) + "%");
        addTableRow(table, "Liczba ocen", String.valueOf(stats.getFeedbackCount()));

        if (stats.getAvgFeedbackRating() != null && stats.getAvgFeedbackRating().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Średnia ocena", String.format("%.2f", stats.getAvgFeedbackRating()) + "/5");
        } else {
            addTableRow(table, "Średnia ocena", "Brak ocen");
        }

        if (stats.getAvgResponseTimeHours() != null && stats.getAvgResponseTimeHours().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Średni czas odpowiedzi",
                    String.format("%.1f", stats.getAvgResponseTimeHours()) + " godzin");
        } else {
            addTableRow(table, "Średni czas odpowiedzi", "Brak danych");
        }

        document.add(table);
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        table.addCell(new PdfPCell(new Phrase(label,
                new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL))));
        table.addCell(new PdfPCell(new Phrase(value,
                new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL))));
    }

    private void addTableHeader(PdfPTable table, String header) {
        PdfPCell cell = new PdfPCell(new Phrase(header,
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    // ========== METODY POMOCNICZE ==========

    private String calculateGrade(MeetingStatistics stats) {
        BigDecimal attendance = stats.getAttendanceRate();
        BigDecimal engagement = stats.getEngagementScore();

        if (attendance == null || engagement == null) {
            return "N/A";
        }

        double score = (attendance.doubleValue() * 0.6) + (engagement.doubleValue() * 0.4);

        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}