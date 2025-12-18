



package com.meethub.domain.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.meethub.domain.model.dto.ParticipantCountDto;
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
import com.meethub.domain.service.MeetingParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingAnalyticsServiceImpl implements MeetingAnalyticsService {

    private final MeetingStatisticsRepository statisticsRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantService meetingParticipantService;

    private final FeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public MeetingStatistics generateMeetingStatistics(Long meetingId) {
        log.info("Generating statistics for meeting: {}", meetingId);

        // 1. Pobierz spotkanie
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        ParticipantCountDto participantCounts = meetingParticipantService.getParticipantCounts(meetingId);

        if (participantCounts == null) {
            participantCounts = new ParticipantCountDto(0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        // 3. Pobierz średni czas odpowiedzi Z BAZY
//        BigDecimal avgResponseTime = BigDecimal.ZERO;
//        Double avgResponseTimeDb = meetingParticipantService.getAverageResponseTimeMinutes(meetingId);
//        if (avgResponseTimeDb != null) {
//            avgResponseTime = BigDecimal.valueOf(avgResponseTimeDb)
//                    .setScale(2, RoundingMode.HALF_UP);
//        }

        // 4. Pobierz statystyki FEEDBACK Z BAZY
        BigDecimal averageRating = BigDecimal.ZERO;
        int feedbackCount = 0;

        try {
            Double avgRatingDb = feedbackRepository.findAverageRatingByMeetingId(meetingId);
            if (avgRatingDb != null) {
                averageRating = BigDecimal.valueOf(avgRatingDb)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            Long feedbackCountDb = feedbackRepository.countByMeetingId(meetingId);
            if (feedbackCountDb != null) {
                feedbackCount = feedbackCountDb.intValue();
            }
        } catch (Exception e) {
            log.warn("Could not calculate feedback statistics: {}", e.getMessage());
        }

        // 5. Sprawdź czy istnieją już statystyki
        Optional<MeetingStatistics> existingStats = statisticsRepository.findByMeetingId(meetingId);

        MeetingStatistics statistics;
        if (existingStats.isPresent()) {
            statistics = existingStats.get();
//            updateStatistics(statistics, participantCounts, avgResponseTime, averageRating, feedbackCount);
            updateStatistics(statistics, participantCounts, averageRating, feedbackCount);

            log.debug("Updated existing statistics for meeting: {}", meetingId);
        } else {
//            statistics = createNewStatistics(meeting, participantCounts, avgResponseTime, averageRating, feedbackCount);
            statistics = createNewStatistics(meeting, participantCounts, averageRating, feedbackCount);

            log.debug("Created new statistics for meeting: {}", meetingId);
        }

        // 6. Ustaw status i timestampy
        setStatisticsStatus(statistics, meeting);
        statistics.setGeneratedAt(LocalDateTime.now());
        statistics.setLastCalculatedAt(LocalDateTime.now());
        statistics.setUpdatedAt(LocalDateTime.now());

        // 7. Zapisz do bazy
        return statisticsRepository.save(statistics);
    }


    private MeetingStatistics createNewStatistics(
            Meeting meeting,
            ParticipantCountDto participantCounts,
//            BigDecimal avgResponseTime,
            BigDecimal averageRating,
            int feedbackCount) {

        return MeetingStatistics.builder()
                .meeting(meeting)
                .totalParticipants(participantCounts.getTotal() != null ?
                        participantCounts.getTotal().intValue() : 0)
                .attendedParticipants(participantCounts.getAttended() != null ?
                        participantCounts.getAttended().intValue() : 0)
                .confirmedParticipants(participantCounts.getConfirmed() != null ?
                        participantCounts.getConfirmed().intValue() : 0)
                .declinedParticipants(participantCounts.getDeclined() != null ?
                        participantCounts.getDeclined().intValue() : 0)
                .pendingParticipants(participantCounts.getPending() != null ?
                        participantCounts.getPending().intValue() : 0)
                .attendanceRate(participantCounts.getAttendanceRate())
                .confirmationRate(participantCounts.getConfirmationRate())
//                .avgResponseTimeMinutes(avgResponseTime)
                .averageRating(averageRating)
                .feedbackCount(feedbackCount)
                .generatedAt(LocalDateTime.now())
                .status(determineStatisticsStatus(meeting))
                .finalized(false)
                .additionalMetrics(new HashMap<>())
                .build();
    }

    private void updateStatistics(
            MeetingStatistics statistics,
            ParticipantCountDto participantCounts,
//            BigDecimal avgResponseTime,
            BigDecimal averageRating,
            int feedbackCount) {

        statistics.setTotalParticipants(participantCounts.getTotal() != null ?
                participantCounts.getTotal().intValue() : 0);
        statistics.setAttendedParticipants(participantCounts.getAttended() != null ?
                participantCounts.getAttended().intValue() : 0);
        statistics.setConfirmedParticipants(participantCounts.getConfirmed() != null ?
                participantCounts.getConfirmed().intValue() : 0);
        statistics.setDeclinedParticipants(participantCounts.getDeclined() != null ?
                participantCounts.getDeclined().intValue() : 0);
        statistics.setPendingParticipants(participantCounts.getPending() != null ?
                participantCounts.getPending().intValue() : 0);

        // Oblicz stawki procentowe (encja ma swoją metodę calculateDerivedMetrics)
        // ale możemy też ustawić bezpośrednio z DTO
        statistics.setAttendanceRate(participantCounts.getAttendanceRate());
        statistics.setConfirmationRate(participantCounts.getConfirmationRate());

//        statistics.setAvgResponseTimeMinutes(avgResponseTime);
        statistics.setAverageRating(averageRating);
        statistics.setFeedbackCount(feedbackCount);
        statistics.setLastCalculatedAt(LocalDateTime.now());
        statistics.setUpdatedAt(LocalDateTime.now());

        // Wywołaj metodę obliczania pochodnych metryk z encji
        statistics.calculateDerivedMetrics();
    }

    private MeetingStatistics.StatisticsStatus determineStatisticsStatus(Meeting meeting) {
        LocalDateTime now = LocalDateTime.now();

        if (meeting.getEndDate() != null && meeting.getEndDate().isBefore(now)) {
            return MeetingStatistics.StatisticsStatus.FINAL;
        } else if (meeting.getStartDate() != null && meeting.getStartDate().isBefore(now)) {
            return MeetingStatistics.StatisticsStatus.PRELIMINARY;
        } else {
            return MeetingStatistics.StatisticsStatus.DRAFT;
        }
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

//    @Override
//    @Transactional(readOnly = true)
//    public byte[] exportReportToPdf(Long organizerId, ReportFilter filter) {
//        // Prosta implementacja - w prawdziwym projekcie użyj biblioteki PDF
//        String pdfContent = "Organizer Report PDF\n" +
//                "===================\n" +
//                "Organizer ID: " + organizerId + "\n" +
//                "Generated: " + LocalDateTime.now() + "\n" +
//                "This is a placeholder PDF export.\n" +
//                "In production, use a library like iText or Apache PDFBox.";
//
//        return pdfContent.getBytes();
//    }



    @Override
    @Transactional(readOnly = true)
    public byte[] exportMeetingStatisticsToPdf(Long meetingId) {
        MeetingStatistics stats = getMeetingStatistics(meetingId)
                .orElseThrow(() -> new RuntimeException("No statistics found for meeting: " + meetingId));

        return generateMeetingStatisticsPdf(stats);
    }

    private byte[] generateMeetingStatisticsPdf(MeetingStatistics stats) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Dodaj nagłówek i stopkę
            HeaderFooterPageEvent event = new HeaderFooterPageEvent();
            writer.setPageEvent(event);

            document.open();

            Meeting meeting = stats.getMeeting();

            // Tytuł dokumentu
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("RAPORT STATYSTYK SPOTKANIA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Informacje o spotkaniu
            addMeetingInfo(document, meeting, stats);

//            // Dodaj linię separatora
//            document.add(new Paragraph(" "));
//            document.add(createSeparator());
//            document.add(new Paragraph(" "));

            // Sekcja podstawowych statystyk
            addBasicStatistics(document, stats);

            // Wykres słupkowy (prosty)
            addAttendanceChart(document, stats);

            // Tabela z detalami
            addDetailedTable(document, stats);

            // Podsumowanie
            addSummary(document, stats);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF for meeting statistics", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private void addMeetingInfo(Document document, Meeting meeting, MeetingStatistics stats) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(10);

        // Wiersze z informacjami
        addInfoRow(infoTable, "Tytuł spotkania:", meeting.getTitle(), headerFont, normalFont);
        addInfoRow(infoTable, "Organizator:",
                meeting.getOrganizer().getFirstName() + " " + meeting.getOrganizer().getLastName(),
                headerFont, normalFont);

        if (meeting.getStartDate() != null) {
            addInfoRow(infoTable, "Data rozpoczęcia:",
                    meeting.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    headerFont, normalFont);
        }

        if (meeting.getEndDate() != null) {
            addInfoRow(infoTable, "Data zakończenia:",
                    meeting.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    headerFont, normalFont);
        }

        addInfoRow(infoTable, "Status statystyk:",
                stats.getStatus().toString(), headerFont, normalFont);

        addInfoRow(infoTable, "Wygenerowano:",
                stats.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                headerFont, normalFont);

        document.add(infoTable);
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        valueCell.setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addBasicStatistics(Document document, MeetingStatistics stats) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph sectionTitle = new Paragraph("PODSTAWOWE STATYSTYKI", sectionFont);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        PdfPTable statsTable = new PdfPTable(2);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingBefore(10);

        // Konfiguracja komórek
        Font statLabelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Font statValueFont = new Font(Font.FontFamily.HELVETICA, 10);

        // Wiersze ze statystykami
        addStatRow(statsTable, "Łączna liczba uczestników:",
                String.valueOf(stats.getTotalParticipants()), statLabelFont, statValueFont);

        addStatRow(statsTable, "Obecni uczestnicy:",
                String.valueOf(stats.getAttendedParticipants()), statLabelFont, statValueFont);

        addStatRow(statsTable, "Frekwencja:",
                formatPercentage(stats.getAttendanceRate()), statLabelFont, statValueFont);

        addStatRow(statsTable, "Potwierdzeni uczestnicy:",
                String.valueOf(stats.getConfirmedParticipants()), statLabelFont, statValueFont);

        addStatRow(statsTable, "Wskaźnik potwierdzeń:",
                formatPercentage(stats.getConfirmationRate()), statLabelFont, statValueFont);

        addStatRow(statsTable, "Odmówili udziału:",
                String.valueOf(stats.getDeclinedParticipants()), statLabelFont, statValueFont);

        addStatRow(statsTable, "Oczekujący na odpowiedź:",
                String.valueOf(stats.getPendingParticipants()), statLabelFont, statValueFont);

        if (stats.getAverageRating() != null && stats.getAverageRating().compareTo(BigDecimal.ZERO) > 0) {
            addStatRow(statsTable, "Średnia ocena:",
                    String.format("%.2f/5", stats.getAverageRating()), statLabelFont, statValueFont);
        }

        if (stats.getFeedbackCount() != null && stats.getFeedbackCount() > 0) {
            addStatRow(statsTable, "Liczba ocen:",
                    String.valueOf(stats.getFeedbackCount()), statLabelFont, statValueFont);
        }

        if (stats.getAvgResponseTimeMinutes() != null &&
                stats.getAvgResponseTimeMinutes().compareTo(BigDecimal.ZERO) > 0) {
            addStatRow(statsTable, "Średni czas odpowiedzi:",
                    String.format("%.1f minut", stats.getAvgResponseTimeMinutes()), statLabelFont, statValueFont);
        }

        document.add(statsTable);
    }

    private void addStatRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        labelCell.setPadding(8);
        labelCell.setBorderWidth(0.5f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(8);
        valueCell.setBorderWidth(0.5f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addAttendanceChart(Document document, MeetingStatistics stats) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph sectionTitle = new Paragraph("FREKWENCJA - WIZUALIZACJA", sectionFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        // Prosta wizualizacja przy użyciu tabeli z kolorami
        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);

        // Oblicz procenty
        int total = stats.getTotalParticipants() != null ? stats.getTotalParticipants() : 0;
        int attended = stats.getAttendedParticipants() != null ? stats.getAttendedParticipants() : 0;
        int confirmed = stats.getConfirmedParticipants() != null ? stats.getConfirmedParticipants() : 0;
        int declined = stats.getDeclinedParticipants() != null ? stats.getDeclinedParticipants() : 0;
        int pending = stats.getPendingParticipants() != null ? stats.getPendingParticipants() : 0;

        if (total > 0) {
            // Legenda
            Font legendFont = new Font(Font.FontFamily.HELVETICA, 9);

            // Rząd z legandą
            PdfPCell legendCell = new PdfPCell();
            legendCell.setBorder(Rectangle.NO_BORDER);
            legendCell.setPadding(5);

            Paragraph legend = new Paragraph();
            legend.add(new Chunk("■ ", new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GREEN)));
            legend.add(new Chunk("Obecni (" + attended + ")", legendFont));
            legend.add(new Chunk("   ■ ", new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLUE)));
            legend.add(new Chunk("Potwierdzeni (" + confirmed + ")", legendFont));
            legend.add(new Chunk("   ■ ", new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.RED)));
            legend.add(new Chunk("Odmówili (" + declined + ")", legendFont));
            legend.add(new Chunk("   ■ ", new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY)));
            legend.add(new Chunk("Oczekujący (" + pending + ")", legendFont));

            legendCell.addElement(legend);
            chartTable.addCell(legendCell);

            // Prosty wykres słupkowy
            PdfPCell chartCell = new PdfPCell();
            chartCell.setBorder(Rectangle.NO_BORDER);
            chartCell.setPadding(5);

            Paragraph chart = new Paragraph();

            // Oblicz szerokości dla każdej grupy
            float attendedWidth = (float) attended / total * 100;
            float confirmedWidth = (float) confirmed / total * 100;
            float declinedWidth = (float) declined / total * 100;
            float pendingWidth = (float) pending / total * 100;

            // Dodaj prostokąty reprezentujące
            chart.add(createBarSegment("Obecni: " + attended + " (" + formatDecimal((float) attended/total*100) + "%)",
                    attendedWidth, BaseColor.GREEN));
            chart.add(Chunk.NEWLINE);

            chart.add(createBarSegment("Potwierdzeni: " + confirmed + " (" + formatDecimal((float) confirmed/total*100) + "%)",
                    confirmedWidth, BaseColor.BLUE));
            chart.add(Chunk.NEWLINE);

            chart.add(createBarSegment("Odmówili: " + declined + " (" + formatDecimal((float) declined/total*100) + "%)",
                    declinedWidth, BaseColor.RED));
            chart.add(Chunk.NEWLINE);

            chart.add(createBarSegment("Oczekujący: " + pending + " (" + formatDecimal((float) pending/total*100) + "%)",
                    pendingWidth, BaseColor.GRAY));

            chartCell.addElement(chart);
            chartTable.addCell(chartCell);
        } else {
            PdfPCell noDataCell = new PdfPCell(new Phrase("Brak danych do wyświetlenia",
                    new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC)));
            noDataCell.setBorder(Rectangle.NO_BORDER);
            noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            chartTable.addCell(noDataCell);
        }

        document.add(chartTable);
    }

    private Paragraph createBarSegment(String label, float width, BaseColor color) {
        Paragraph segment = new Paragraph();

        // Dodaj kolorowy prostokąt
        Chunk bar = new Chunk("   ");
        bar.setBackground(color);
        bar.setHorizontalScaling(width * 3); // Skalowanie dla wizualizacji

        segment.add(bar);
        segment.add(new Chunk(" " + label, new Font(Font.FontFamily.HELVETICA, 9)));

        return segment;
    }

    private void addDetailedTable(Document document, MeetingStatistics stats) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph sectionTitle = new Paragraph("SZCZEGÓŁOWA ANALIZA", sectionFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        PdfPTable detailedTable = new PdfPTable(3);
        detailedTable.setWidthPercentage(100);
        detailedTable.setSpacingBefore(10);

        // Nagłówki tabeli
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);

        PdfPCell header1 = new PdfPCell(new Phrase("KATEGORIA", headerFont));
        header1.setBackgroundColor(BaseColor.DARK_GRAY);
        header1.setHorizontalAlignment(Element.ALIGN_CENTER);
        header1.setPadding(10);

        PdfPCell header2 = new PdfPCell(new Phrase("LICZBA", headerFont));
        header2.setBackgroundColor(BaseColor.DARK_GRAY);
        header2.setHorizontalAlignment(Element.ALIGN_CENTER);
        header2.setPadding(10);

        PdfPCell header3 = new PdfPCell(new Phrase("PROCENT", headerFont));
        header3.setBackgroundColor(BaseColor.DARK_GRAY);
        header3.setHorizontalAlignment(Element.ALIGN_CENTER);
        header3.setPadding(10);

        detailedTable.addCell(header1);
        detailedTable.addCell(header2);
        detailedTable.addCell(header3);

        // Wiersze z danymi
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10);
        int total = stats.getTotalParticipants() != null ? stats.getTotalParticipants() : 0;

        addDetailRow(detailedTable, "Obecni", stats.getAttendedParticipants(), total, dataFont);
        addDetailRow(detailedTable, "Potwierdzeni", stats.getConfirmedParticipants(), total, dataFont);
        addDetailRow(detailedTable, "Odmówili", stats.getDeclinedParticipants(), total, dataFont);
        addDetailRow(detailedTable, "Oczekujący", stats.getPendingParticipants(), total, dataFont);

        // Wiersz sumy
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("RAZEM", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        totalLabelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        totalLabelCell.setPadding(8);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(String.valueOf(total),
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        totalValueCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalValueCell.setPadding(8);

        PdfPCell totalPercentCell = new PdfPCell(new Phrase("100%",
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        totalPercentCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        totalPercentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalPercentCell.setPadding(8);

        detailedTable.addCell(totalLabelCell);
        detailedTable.addCell(totalValueCell);
        detailedTable.addCell(totalPercentCell);

        document.add(detailedTable);
    }

    private void addDetailRow(PdfPTable table, String category, Integer value, int total, Font font) {
        table.addCell(new PdfPCell(new Phrase(category, font)));

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? String.valueOf(value) : "0", font));
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(valueCell);

        float percentage = total > 0 ? (float) (value != null ? value : 0) / total * 100 : 0;
        PdfPCell percentCell = new PdfPCell(new Phrase(formatDecimal(percentage) + "%", font));
        percentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(percentCell);
    }

    private void addSummary(Document document, MeetingStatistics stats) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph sectionTitle = new Paragraph("PODSUMOWANIE", sectionFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        Font summaryFont = new Font(Font.FontFamily.HELVETICA, 11);

        List<String> summaryPoints = new ArrayList<>();

        // Dodaj punkty podsumowania na podstawie statystyk
        if (stats.getAttendanceRate() != null) {
            if (stats.getAttendanceRate().compareTo(new BigDecimal("80")) >= 0) {
                summaryPoints.add("• Wysoka frekwencja (" + formatPercentage(stats.getAttendanceRate()) + ") - bardzo dobry wynik");
            } else if (stats.getAttendanceRate().compareTo(new BigDecimal("60")) >= 0) {
                summaryPoints.add("• Średnia frekwencja (" + formatPercentage(stats.getAttendanceRate()) + ") - satysfakcjonujący wynik");
            } else {
                summaryPoints.add("• Niska frekwencja (" + formatPercentage(stats.getAttendanceRate()) + ") - wymaga poprawy");
            }
        }

        if (stats.getAverageRating() != null && stats.getAverageRating().compareTo(BigDecimal.ZERO) > 0) {
            if (stats.getAverageRating().compareTo(new BigDecimal("4.0")) >= 0) {
                summaryPoints.add("• Wysoka ocena uczestników (" + formatDecimal(stats.getAverageRating()) + "/5) - spotkanie dobrze ocenione");
            } else if (stats.getAverageRating().compareTo(new BigDecimal("3.0")) >= 0) {
                summaryPoints.add("• Średnia ocena uczestników (" + formatDecimal(stats.getAverageRating()) + "/5) - dobre spotkanie");
            } else {
                summaryPoints.add("• Niska ocena uczestników (" + formatDecimal(stats.getAverageRating()) + "/5) - wymaga analizy");
            }
        }

        if (stats.getFeedbackCount() != null && stats.getFeedbackCount() > 0) {
            int feedbackRate = (stats.getAttendedParticipants() != null && stats.getAttendedParticipants() > 0)
                    ? (stats.getFeedbackCount() * 100 / stats.getAttendedParticipants())
                    : 0;

            summaryPoints.add("• " + stats.getFeedbackCount() + " osób (" + feedbackRate + "%) udzieliło feedbacku");
        }

        if (summaryPoints.isEmpty()) {
            summaryPoints.add("• Brak wystarczających danych do szczegółowego podsumowania");
        }

        summaryPoints.add("• Raport wygenerowano automatycznie " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        // Dodaj punkty do dokumentu
        for (String point : summaryPoints) {
            Paragraph pointParagraph = new Paragraph(point, summaryFont);
            pointParagraph.setSpacingAfter(5);
            document.add(pointParagraph);
        }
    }

    private Chunk createSeparator() {
        Chunk separator = new Chunk("________________________________________________________________");
        separator.setHorizontalScaling(100);
        return separator;
    }

    private String formatPercentage(BigDecimal value) {
        if (value == null) return "0.00%";
        return String.format("%.2f", value) + "%";
    }

    private String formatDecimal(Number value) {
        if (value == null) return "0.00";
        return String.format("%.2f", value);
    }

    // Klasa wewnętrzna dla nagłówka i stopki
    class HeaderFooterPageEvent extends PdfPageEventHelper {
        private PdfTemplate total;
        private BaseFont baseFont;
        private Date printDate;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                printDate = new Date();
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
                total = writer.getDirectContent().createTemplate(30, 16);
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(3);
            try {
                footer.setWidths(new int[]{24, 24, 2});
                footer.setTotalWidth(527);
                footer.setLockedWidth(true);
                footer.getDefaultCell().setFixedHeight(20);
                footer.getDefaultCell().setBorder(Rectangle.TOP);

                // Lewa strona - data
                footer.addCell(new Phrase("Wygenerowano: " +
                        new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(printDate),
                        new Font(Font.FontFamily.HELVETICA, 8)));

                // Środek - tytuł
                footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
                footer.addCell(new Phrase("Raport Statystyk Spotkania",
                        new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD)));

                footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);

// Tworzymy Phrase z odpowiednimi parametrami
                Phrase pageNumberPhrase = new Phrase(
                        String.format("Strona %d z ", writer.getPageNumber()),
                        new Font(Font.FontFamily.HELVETICA, 8)
                );

                PdfPCell pageNumberCell = new PdfPCell(pageNumberPhrase);
                pageNumberCell.setBorder(Rectangle.TOP);
                footer.addCell(pageNumberCell);

                // Pozycjonuj stopkę
                footer.writeSelectedRows(0, -1, 34, 50, writer.getDirectContent());

            } catch (DocumentException de) {
                throw new ExceptionConverter(de);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber() - 1),
                            new Font(Font.FontFamily.HELVETICA, 8)),
                    2, 2, 0);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportToPdf(Long organizerId, ReportFilter filter) {
        OrganizerReport report = generateOrganizerReport(organizerId, filter);
        return generateOrganizerReportPdf(report, filter);
    }

    private byte[] generateOrganizerReportPdf(OrganizerReport report, ReportFilter filter) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            HeaderFooterPageEvent event = new HeaderFooterPageEvent();
            writer.setPageEvent(event);

            document.open();

            // Tytuł
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("RAPORT ORGANIZATORA SPOTKAŃ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            // Informacje o raporcie
            addReportInfo(document, report, filter);

            // Statystyki podsumowujące
            addReportSummary(document, report);

            // Tabela ze spotkaniami (jeśli potrzebna)
            // addMeetingsTable(document, report);

            // Wykresy/trendy

            // Rekomendacje
            addRecommendations(document, report);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating organizer PDF report", e);
            throw new RuntimeException("Failed to generate organizer PDF: " + e.getMessage());
        }
    }

    private void addReportInfo(Document document, OrganizerReport report, ReportFilter filter) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(80);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);
//        infoTable.setSpacingBefore(10);
//        infoTable.setSpacingAfter(20);
        infoTable.setSpacingBefore(0);
        infoTable.setSpacingAfter(0);

        addInfoRow(infoTable, "Organizator ID:", String.valueOf(report.getOrganizerId()), headerFont, normalFont);
        addInfoRow(infoTable, "Okres raportu:", getDateRangeText(filter), headerFont, normalFont);
        addInfoRow(infoTable, "Liczba spotkań:", String.valueOf(report.getTotalMeetings()), headerFont, normalFont);
        addInfoRow(infoTable, "Wygenerowano:",
                report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                headerFont, normalFont);

        document.add(infoTable);
    }

    private String getDateRangeText(ReportFilter filter) {
        if (filter == null || (filter.getDateFrom() == null && filter.getDateTo() == null)) {
            return "Cały okres";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String from = filter.getDateFrom() != null ? filter.getDateFrom().format(formatter) : "nieokreślony";
        String to = filter.getDateTo() != null ? filter.getDateTo().format(formatter) : "nieokreślony";

        return from + " - " + to;
    }

    private void addReportSummary(Document document, OrganizerReport report) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph sectionTitle = new Paragraph("PODSUMOWANIE", sectionFont);
        sectionTitle.setSpacingAfter(20);
        document.add(sectionTitle);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 11);

        addStatRow(summaryTable, "Całkowita liczba uczestników:",
                String.valueOf(report.getTotalParticipants()), labelFont, valueFont);

        addStatRow(summaryTable, "Uczestnicy obecni:",
                String.valueOf(report.getTotalAttended()), labelFont, valueFont);

        addStatRow(summaryTable, "Średnia frekwencja:",
                formatDecimal(report.getAverageAttendanceRate()) + "%", labelFont, valueFont);

        // Oblicz efektywność
        if (report.getTotalParticipants() > 0 && report.getTotalAttended() > 0) {
            double efficiency = (double) report.getTotalAttended() / report.getTotalParticipants() * 100;
            String efficiencyText;
            if (efficiency >= 80) {
                efficiencyText = "Wysoka (" + formatDecimal(efficiency) + "%)";
            } else if (efficiency >= 60) {
                efficiencyText = "Średnia (" + formatDecimal(efficiency) + "%)";
            } else {
                efficiencyText = "Niska (" + formatDecimal(efficiency) + "%)";
            }

            addStatRow(summaryTable, "Efektywność organizacji:", efficiencyText, labelFont, valueFont);
        }

        document.add(summaryTable);
    }


    private void addRecommendations(Document document, OrganizerReport report) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph sectionTitle = new Paragraph("REKOMENDACJE", sectionFont);
        sectionTitle.setSpacingBefore(30);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        Font recFont = new Font(Font.FontFamily.HELVETICA, 10);

        List<String> recommendations = new ArrayList<>();

        // Dodaj rekomendacje na podstawie statystyk
        if (report.getAverageAttendanceRate() != null) {
            if (report.getAverageAttendanceRate().compareTo(new BigDecimal("85")) < 0) {
                recommendations.add("• Rozważ wysyłanie przypomnień na 24h przed spotkaniem");
                recommendations.add("• Upewnij się, że terminy spotkań są dogodne dla uczestników");
            }

            if (report.getTotalMeetings() > 10) {
                recommendations.add("• Regularnie analizuj feedback od uczestników");
                recommendations.add("• Dostosuj format spotkań na podstawie zebranych danych");
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("• Kontynuuj obecne praktyki - wyniki są satysfakcjonujące");
            recommendations.add("• Regularnie zbieraj feedback dla ciągłej poprawy");
        }

        recommendations.add("• Następny raport zalecany za 30 dni");

        for (String rec : recommendations) {
            Paragraph recParagraph = new Paragraph(rec, recFont);
            recParagraph.setSpacingAfter(5);
            recParagraph.setIndentationLeft(10);
            document.add(recParagraph);
        }
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

//    @Override
//    @Transactional(readOnly = true)
//    public byte[] exportMeetingStatisticsToPdf(Long meetingId) {
//        MeetingStatistics stats = getMeetingStatistics(meetingId)
//                .orElseThrow(() -> new RuntimeException("No statistics found for meeting: " + meetingId));
//
//        String pdfContent = "Meeting Statistics PDF\n" +
//                "=====================\n" +
//                "Meeting ID: " + meetingId + "\n" +
//                "Generated: " + stats.getGeneratedAt() + "\n" +
//                "Total Participants: " + stats.getTotalParticipants() + "\n" +
//                "Attended: " + stats.getAttendedParticipants() + "\n" +
//                "Attendance Rate: " + stats.getAttendanceRate() + "%\n" +
//                "This is a placeholder PDF export.";
//
//        return pdfContent.getBytes();
//    }

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
