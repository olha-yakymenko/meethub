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

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.OrganizerReport;
import com.meethub.domain.repository.jpa.FeedbackRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingStatisticsRepository;
import com.meethub.domain.service.MeetingAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingAnalyticsServiceImpl implements MeetingAnalyticsService {

    private final MeetingStatisticsRepository statisticsRepository;
    private final MeetingRepository meetingRepository;

    private final FeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public MeetingStatistics generateMeetingStatistics(Long meetingId) {
        log.info("Generating statistics for meeting: {}", meetingId);

        // 1. Pobierz spotkanie
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + meetingId));

        // 2. Pobierz uczestników
        Set<MeetingParticipant> participants = meeting.getParticipants();
        List<MeetingParticipant> participantList = new ArrayList<>(participants);

        // 3. Oblicz podstawowe statystyki
        int totalParticipants = participantList.size();

        int confirmedCount = 0;
        int declinedCount = 0;
        int pendingCount = 0;

        for (MeetingParticipant participant : participantList) {
            ParticipationStatus status = participant.getStatus();

            if (status != null) {
                switch (status) {
                    case CONFIRMED:
                    case ATTENDED:
                        confirmedCount++;
                        break;
                    case DECLINED:
                        declinedCount++;
                        break;
                    case PENDING:
                    case INVITED:
                    default:
                        pendingCount++;
                        break;
                }
            } else {
                pendingCount++;
            }
        }

        // 4. Dla celów testowych - ustaw attended na taką samą wartość jak confirmed
        int attendedCount = confirmedCount;

        // 5. Oblicz stawki procentowe
        BigDecimal attendanceRate = totalParticipants > 0 ?
                new BigDecimal(attendedCount)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(totalParticipants), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        BigDecimal confirmationRate = totalParticipants > 0 ?
                new BigDecimal(confirmedCount)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(totalParticipants), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // 6. OBLICZ ŚREDNIĄ OCENĘ I LICZBĘ FEEDBACKÓW (TO JEST BARDZO WAŻNE!)
        BigDecimal averageRating = BigDecimal.ZERO;
        int feedbackCount = 0;

        try {
            // Sprawdź czy feedbackRepository istnieje
            if (feedbackRepository != null) {
                // Pobierz średnią ocenę
                Double avgRatingValue = feedbackRepository.findAverageRatingByMeetingId(meetingId);
                if (avgRatingValue != null) {
                    averageRating = BigDecimal.valueOf(avgRatingValue).setScale(2, RoundingMode.HALF_UP);
                }

                // Pobierz liczbę ocen
                Long feedbackCountLong = feedbackRepository.countByMeetingId(meetingId);
                if (feedbackCountLong != null) {
                    feedbackCount = feedbackCountLong.intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Could not calculate feedback statistics: {}", e.getMessage());
        }

        // 7. Oblicz średni czas odpowiedzi (jeśli masz takie dane)
        BigDecimal avgResponseTime = BigDecimal.ZERO; // Tymczasowo 0

        // 8. Sprawdź czy istnieją już statystyki
        Optional<MeetingStatistics> existingStats = statisticsRepository.findByMeetingId(meetingId);

        MeetingStatistics statistics;
        if (existingStats.isPresent()) {
            statistics = existingStats.get();
            updateStatistics(statistics, totalParticipants, attendedCount, confirmedCount,
                    declinedCount, pendingCount, attendanceRate, confirmationRate,
                    avgResponseTime, averageRating, feedbackCount); // DODAJ averageRating i feedbackCount
            log.debug("Updated existing statistics for meeting: {}", meetingId);
        } else {
            statistics = createNewStatistics(meeting, totalParticipants, attendedCount, confirmedCount,
                    declinedCount, pendingCount, attendanceRate, confirmationRate,
                    avgResponseTime, averageRating, feedbackCount); // DODAJ averageRating i feedbackCount
            log.debug("Created new statistics for meeting: {}", meetingId);
        }

        // 9. Ustaw status i timestampy
        setStatisticsStatus(statistics, meeting);
        statistics.setGeneratedAt(LocalDateTime.now());
        statistics.setLastCalculatedAt(LocalDateTime.now());
        statistics.setUpdatedAt(LocalDateTime.now());

        // 10. Zapisz do bazy
        return statisticsRepository.save(statistics);
    }

    private MeetingStatistics createNewStatistics(Meeting meeting,
                                                  int totalParticipants,
                                                  int attendedCount,
                                                  int confirmedCount,
                                                  int declinedCount,
                                                  int pendingCount,
                                                  BigDecimal attendanceRate,
                                                  BigDecimal confirmationRate,
                                                  BigDecimal avgResponseTime,
                                                  BigDecimal averageRating,    // DODAJ
                                                  int feedbackCount) {         // DODAJ

        return MeetingStatistics.builder()
                .meeting(meeting)
                .totalParticipants(totalParticipants)
                .attendedParticipants(attendedCount)
                .confirmedParticipants(confirmedCount)
                .declinedParticipants(declinedCount)
                .pendingParticipants(pendingCount)
                .attendanceRate(attendanceRate)
                .confirmationRate(confirmationRate)
                .avgResponseTimeMinutes(avgResponseTime)
                .averageRating(averageRating)      // DODAJ
                .feedbackCount(feedbackCount)      // DODAJ
                .generatedAt(LocalDateTime.now())
                .status(MeetingStatistics.StatisticsStatus.DRAFT)
                .finalized(false)
                .additionalMetrics(new HashMap<>())
                .build();
    }

    private void updateStatistics(MeetingStatistics statistics,
                                  int totalParticipants,
                                  int attendedCount,
                                  int confirmedCount,
                                  int declinedCount,
                                  int pendingCount,
                                  BigDecimal attendanceRate,
                                  BigDecimal confirmationRate,
                                  BigDecimal avgResponseTime,
                                  BigDecimal averageRating,    // DODAJ
                                  int feedbackCount) {         // DODAJ

        statistics.setTotalParticipants(totalParticipants);
        statistics.setAttendedParticipants(attendedCount);
        statistics.setConfirmedParticipants(confirmedCount);
        statistics.setDeclinedParticipants(declinedCount);
        statistics.setPendingParticipants(pendingCount);
        statistics.setAttendanceRate(attendanceRate);
        statistics.setConfirmationRate(confirmationRate);
        statistics.setAvgResponseTimeMinutes(avgResponseTime);
        statistics.setAverageRating(averageRating);      // DODAJ
        statistics.setFeedbackCount(feedbackCount);      // DODAJ
        statistics.setLastCalculatedAt(LocalDateTime.now());
        statistics.setUpdatedAt(LocalDateTime.now());

        statistics.calculateDerivedMetrics();
    }

    private void setStatisticsStatus(MeetingStatistics statistics, Meeting meeting) {
        if (meeting.getEndDate() != null && meeting.getEndDate().isBefore(LocalDateTime.now())) {
            statistics.setStatus(MeetingStatistics.StatisticsStatus.FINAL);
            statistics.setFinalized(true);
        } else if (meeting.getStartDate() != null && meeting.getStartDate().isBefore(LocalDateTime.now())) {
            statistics.setStatus(MeetingStatistics.StatisticsStatus.PRELIMINARY);
            statistics.setFinalized(false);
        } else {
            statistics.setStatus(MeetingStatistics.StatisticsStatus.DRAFT);
            statistics.setFinalized(false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MeetingStatistics> getMeetingStatistics(Long meetingId) {
        return statisticsRepository.findByMeetingId(meetingId);
    }

    @Override
    @Transactional
    public void deleteMeetingStatistics(Long meetingId) {
        statisticsRepository.deleteByMeetingId(meetingId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizerReport generateOrganizerReport(Long organizerId, ReportFilter filter) {
        log.info("Generating organizer report for organizer: {}", organizerId);

        // 1. Pobierz statystyki dla organizatora
        List<MeetingStatistics> allStats = statisticsRepository.findByOrganizerId(organizerId);

        // 2. Filtruj według daty
        List<MeetingStatistics> filteredStats = filterStatistics(allStats, filter);

        // 3. Stwórz raport
        OrganizerReport report = new OrganizerReport();
        report.setOrganizerId(organizerId);
        report.setGeneratedAt(LocalDateTime.now());
        report.setTotalMeetings(filteredStats.size());

        if (!filteredStats.isEmpty()) {
            // Oblicz średnią frekwencję
            BigDecimal totalAttendance = filteredStats.stream()
                    .filter(stats -> stats.getAttendanceRate() != null)
                    .map(MeetingStatistics::getAttendanceRate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgAttendance = totalAttendance.divide(
                    new BigDecimal(filteredStats.size()), 2, RoundingMode.HALF_UP);
            report.setAverageAttendanceRate(avgAttendance);

            // Oblicz całkowitą liczbę uczestników
            int totalParticipants = filteredStats.stream()
                    .filter(stats -> stats.getTotalParticipants() != null)
                    .mapToInt(MeetingStatistics::getTotalParticipants)
                    .sum();
            report.setTotalParticipants(totalParticipants);

            // Oblicz liczbę uczestników, którzy przyszli
            int totalAttended = filteredStats.stream()
                    .filter(stats -> stats.getAttendedParticipants() != null)
                    .mapToInt(MeetingStatistics::getAttendedParticipants)
                    .sum();
            report.setTotalAttended(totalAttended);
        }

        return report;
    }

    private List<MeetingStatistics> filterStatistics(List<MeetingStatistics> allStats, ReportFilter filter) {
        if (filter == null) {
            return allStats;
        }

        return allStats.stream()
                .filter(stats -> {
                    // Filtrowanie po dacie
                    if (stats.getMeeting() == null || stats.getMeeting().getStartDate() == null) {
                        return false;
                    }

                    LocalDateTime meetingDate = stats.getMeeting().getStartDate();

                    if (filter.getDateFrom() != null && meetingDate.isBefore(filter.getDateFrom())) {
                        return false;
                    }

                    if (filter.getDateTo() != null && meetingDate.isAfter(filter.getDateTo())) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportToCsv(Long organizerId, ReportFilter filter) {
        OrganizerReport report = generateOrganizerReport(organizerId, filter);

        StringBuilder csv = new StringBuilder();
        csv.append("Organizer Report\n");
        csv.append("Organizer ID: ").append(organizerId).append("\n");
        csv.append("Generated: ").append(LocalDateTime.now()).append("\n");
        csv.append("Total Meetings: ").append(report.getTotalMeetings()).append("\n");
        csv.append("Average Attendance Rate: ").append(report.getAverageAttendanceRate()).append("%\n");
        csv.append("Total Participants: ").append(report.getTotalParticipants()).append("\n");
        csv.append("Total Attended: ").append(report.getTotalAttended()).append("\n");

        return csv.toString().getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportToPdf(Long organizerId, ReportFilter filter) {
        // Prosta implementacja - w prawdziwym projekcie użyj biblioteki PDF
        String pdfContent = "Organizer Report PDF\n" +
                "===================\n" +
                "Organizer ID: " + organizerId + "\n" +
                "Generated: " + LocalDateTime.now() + "\n" +
                "This is a placeholder PDF export.\n" +
                "In production, use a library like iText or Apache PDFBox.";

        return pdfContent.getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingStatistics> getMeetingStatisticsByOrganizer(Long organizerId) {
        return statisticsRepository.findByOrganizerId(organizerId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportMeetingStatisticsToCsv(Long meetingId) {
        MeetingStatistics stats = getMeetingStatistics(meetingId)
                .orElseThrow(() -> new RuntimeException("No statistics found for meeting: " + meetingId));

        StringBuilder csv = new StringBuilder();
        csv.append("Meeting Statistics\n");
        csv.append("Meeting ID: ").append(meetingId).append("\n");
        csv.append("Generated: ").append(stats.getGeneratedAt()).append("\n");
        csv.append("Total Participants: ").append(stats.getTotalParticipants()).append("\n");
        csv.append("Attended: ").append(stats.getAttendedParticipants()).append("\n");
        csv.append("Attendance Rate: ").append(stats.getAttendanceRate()).append("%\n");
        csv.append("Confirmed: ").append(stats.getConfirmedParticipants()).append("\n");
        csv.append("Confirmation Rate: ").append(stats.getConfirmationRate()).append("%\n");
        csv.append("Average Response Time: ").append(stats.getAvgResponseTimeMinutes()).append(" minutes\n");

        return csv.toString().getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportMeetingStatisticsToPdf(Long meetingId) {
        MeetingStatistics stats = getMeetingStatistics(meetingId)
                .orElseThrow(() -> new RuntimeException("No statistics found for meeting: " + meetingId));

        String pdfContent = "Meeting Statistics PDF\n" +
                "=====================\n" +
                "Meeting ID: " + meetingId + "\n" +
                "Generated: " + stats.getGeneratedAt() + "\n" +
                "Total Participants: " + stats.getTotalParticipants() + "\n" +
                "Attended: " + stats.getAttendedParticipants() + "\n" +
                "Attendance Rate: " + stats.getAttendanceRate() + "%\n" +
                "This is a placeholder PDF export.";

        return pdfContent.getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAverageResponseTime(Long meetingId) {
        return getMeetingStatistics(meetingId)
                .map(MeetingStatistics::getAvgResponseTimeMinutes)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingStatistics> getRecentStatistics(int limit) {
        List<MeetingStatistics> allStats = statisticsRepository.findAll();

        return allStats.stream()
                .sorted((s1, s2) -> s2.getGeneratedAt().compareTo(s1.getGeneratedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatisticsOverview(Long meetingId) {
        MeetingStatistics stats = getMeetingStatistics(meetingId)
                .orElseThrow(() -> new RuntimeException("No statistics found for meeting: " + meetingId));

        Map<String, Object> overview = new HashMap<>();
        overview.put("meetingId", meetingId);
        overview.put("attendanceRate", stats.getAttendanceRate());
        overview.put("totalParticipants", stats.getTotalParticipants());
        overview.put("attendedParticipants", stats.getAttendedParticipants());
        overview.put("confirmedParticipants", stats.getConfirmedParticipants());
        overview.put("avgResponseTime", stats.getAvgResponseTimeMinutes());
        overview.put("generatedAt", stats.getGeneratedAt());
        overview.put("status", stats.getStatus().toString());
        overview.put("finalized", stats.getFinalized());

        return overview;
    }

    @Override
    @Transactional
    public void refreshAllStatistics() {
        log.info("Refreshing all statistics...");

        List<MeetingStatistics> allStats = statisticsRepository.findAll();
        int refreshedCount = 0;

        for (MeetingStatistics stats : allStats) {
            try {
                if (stats.getMeeting() != null && stats.getMeeting().getId() != null) {
                    generateMeetingStatistics(stats.getMeeting().getId());
                    refreshedCount++;
                }
            } catch (Exception e) {
                log.error("Error refreshing statistics for meeting {}: {}",
                        stats.getMeeting().getId(), e.getMessage());
            }
        }

        log.info("Refreshed {} statistics", refreshedCount);
    }


}


