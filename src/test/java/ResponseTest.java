// Testy dla ReportSummary.java
package com.meethub.domain.model.response;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.entity.TaskAssignment;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReportSummaryTest {

    @Test
    void testBuilderAndGetters() {
        ReportSummary report = ReportSummary.builder()
                .totalMeetings(10)
                .totalParticipants(100)
                .avgAttendanceRate(new BigDecimal("85.75"))
                .avgEngagementScore(new BigDecimal("78.50"))
                .build();

        assertEquals(10, report.getTotalMeetings());
        assertEquals(100, report.getTotalParticipants());
        assertEquals(new BigDecimal("85.75"), report.getAvgAttendanceRate());
        assertEquals(new BigDecimal("78.50"), report.getAvgEngagementScore());
    }

    @Test
    void testNoArgsConstructor() {
        ReportSummary report = new ReportSummary();
        assertNull(report.getTotalMeetings());
        assertNull(report.getAvgAttendanceRate());
    }

    @Test
    void testAllArgsConstructor() {
        ReportSummary report = new ReportSummary(5, 50,
                new BigDecimal("90.0"), new BigDecimal("80.0"));

        assertEquals(5, report.getTotalMeetings());
        assertEquals(50, report.getTotalParticipants());
        assertEquals(new BigDecimal("90.0"), report.getAvgAttendanceRate());
        assertEquals(new BigDecimal("80.0"), report.getAvgEngagementScore());
    }

    @Test
    void testEmpty() {
        ReportSummary empty = ReportSummary.empty();

        assertEquals(0, empty.getTotalMeetings());
        assertEquals(0, empty.getTotalParticipants());
        assertEquals(BigDecimal.ZERO, empty.getAvgAttendanceRate());
        assertEquals(BigDecimal.ZERO, empty.getAvgEngagementScore());
    }

    @Test
    void testEqualsAndHashCode() {
        ReportSummary report1 = ReportSummary.builder()
                .totalMeetings(5)
                .avgAttendanceRate(new BigDecimal("90.0"))
                .build();

        ReportSummary report2 = ReportSummary.builder()
                .totalMeetings(5)
                .avgAttendanceRate(new BigDecimal("90.0"))
                .build();

        ReportSummary report3 = ReportSummary.builder()
                .totalMeetings(10)
                .avgAttendanceRate(new BigDecimal("85.0"))
                .build();

        assertEquals(report1, report2);
        assertNotEquals(report1, report3);
        assertEquals(report1.hashCode(), report2.hashCode());
        assertNotEquals(report1.hashCode(), report3.hashCode());
    }

    @Test
    void testToString() {
        ReportSummary report = ReportSummary.builder()
                .totalMeetings(5)
                .avgAttendanceRate(new BigDecimal("90.5"))
                .build();

        String toString = report.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("totalMeetings=5"));
    }
}

// Testy dla MeetingTaskDetailsResponse.java
class MeetingTaskDetailsResponseTest {

    @Test
    void testBuilderAndDataAnnotations() {
        Meeting meeting = new Meeting();
        Task task = new Task();

        MeetingTaskDetailsResponse response = MeetingTaskDetailsResponse.builder()
                .meeting(meeting)
                .task(task)
                .isOrganizer(true)
                .userId(123L)
                .build();

        assertEquals(meeting, response.getMeeting());
        assertEquals(task, response.getTask());
        assertTrue(response.isOrganizer());
        assertEquals(123L, response.getUserId());
    }

}

// Testy dla MeetingTasksResponse.java
class MeetingTasksResponseTest {

    @Test
    void testBuilderWithLists() {
        Meeting meeting = new Meeting();
        Task task1 = new Task();
        Task task2 = new Task();

        MeetingTasksResponse response = MeetingTasksResponse.builder()
                .meeting(meeting)
                .tasks(List.of(task1, task2))
                .isOrganizer(true)
                .build();

        assertEquals(meeting, response.getMeeting());
        assertEquals(2, response.getTasks().size());
        assertTrue(response.isOrganizer());
        assertTrue(response.getTasks().contains(task1));
        assertTrue(response.getTasks().contains(task2));
    }
}

// Testy dla StatusChangeResponse.java
class StatusChangeResponseTest {

    @Test
    void testStatusChangeBuilder() {
        LocalDateTime now = LocalDateTime.now();

        StatusChangeResponse response = StatusChangeResponse.builder()
                .oldStatus("PENDING")
                .newStatus("APPROVED")
                .changedAt(now)
                .changedByName("John Doe")
                .reason("All requirements met")
                .build();

        assertEquals("PENDING", response.getOldStatus());
        assertEquals("APPROVED", response.getNewStatus());
        assertEquals(now, response.getChangedAt());
        assertEquals("John Doe", response.getChangedByName());
        assertEquals("All requirements met", response.getReason());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 10, 0);

        StatusChangeResponse response1 = StatusChangeResponse.builder()
                .oldStatus("PENDING")
                .newStatus("APPROVED")
                .changedAt(time)
                .changedByName("User1")
                .build();

        StatusChangeResponse response2 = StatusChangeResponse.builder()
                .oldStatus("PENDING")
                .newStatus("APPROVED")
                .changedAt(time)
                .changedByName("User1")
                .build();

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
}

// Testy dla UserResponse.java
class UserResponseTest {

    @Test
    void testBuilderPattern() {
        UserResponse user = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.ADMIN)
                .phoneNumber("123456789")
                .createdAt(LocalDateTime.now())
                .build();

        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals("123456789", user.getPhoneNumber());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void testConstructor() {
        UserResponse user = new UserResponse(1L, "test@example.com",
                "John", "Doe", UserRole.ADMIN);

        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals(UserRole.ADMIN, user.getRole());
    }

    @Test
    void testFullConstructor() {
        LocalDateTime now = LocalDateTime.now();
        UserResponse user = new UserResponse(1L, "test@example.com",
                "John", "Doe", UserRole.ADMIN, "123456789", now);

        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals("123456789", user.getPhoneNumber());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    void testHelperMethods() {
        UserResponse user = UserResponse.builder()
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.ADMIN)
                .phoneNumber("123456789")
                .build();

        assertEquals("John Doe", user.getFullName());
        assertTrue(user.isAdmin());
        assertTrue(user.isOrganizer());
        assertFalse(user.isParticipant());
        assertTrue(user.hasPhoneNumber());
    }

    @Test
    void testEmptyPhoneNumber() {
        UserResponse user = UserResponse.builder()
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("   ")
                .build();

        assertFalse(user.hasPhoneNumber());
    }

    @Test
    void testEqualsAndHashCode() {
        UserResponse user1 = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        UserResponse user2 = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        UserResponse user3 = UserResponse.builder()
                .id(2L)
                .email("different@example.com")
                .build();

        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1.hashCode(), user3.hashCode());
    }

    @Test
    void testToString() {
        UserResponse user = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .build();

        String toString = user.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("test@example.com"));
        assertTrue(toString.contains("John"));
        assertTrue(toString.contains("***")); // phone number masked
    }
}

// Testy dla DashboardStatsResponse.java
class DashboardStatsResponseTest {

    @Test
    void testDashboardStatsBuilder() {
        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalMeetings(100L)
                .upcomingMeetings(25L)
                .participantsCount(500L)
                .organizedMeetings(30L)
                .invitedMeetings(50L)
                .confirmedMeetings(40L)
                .averageParticipants(25.5)
                .meetingsToday(3L)
                .meetingsThisWeek(10L)
                .meetingsThisMonth(35L)
                .build();

        assertEquals(100L, stats.getTotalMeetings());
        assertEquals(25L, stats.getUpcomingMeetings());
        assertEquals(500L, stats.getParticipantsCount());
        assertEquals(30L, stats.getOrganizedMeetings());
        assertEquals(50L, stats.getInvitedMeetings());
        assertEquals(40L, stats.getConfirmedMeetings());
        assertEquals(25.5, stats.getAverageParticipants());
        assertEquals(3L, stats.getMeetingsToday());
        assertEquals(10L, stats.getMeetingsThisWeek());
        assertEquals(35L, stats.getMeetingsThisMonth());
    }
}

// Testy dla ApiResponse.java
class ApiResponseTest {

    @Test
    void testSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("Operation completed", "ResultData");

        assertTrue(response.isSuccess());
        assertEquals("Operation completed", response.getMessage());
        assertEquals("ResultData", response.getData());
    }

    @Test
    void testErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error("Operation failed");

        assertFalse(response.isSuccess());
        assertEquals("Operation failed", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testNoArgsConstructor() {
        ApiResponse<String> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Test");
        response.setData("Data");

        assertTrue(response.isSuccess());
        assertEquals("Test", response.getMessage());
        assertEquals("Data", response.getData());
    }

    @Test
    void testAllArgsConstructor() {
        ApiResponse<Integer> response = new ApiResponse<>(true, "Test", 42);

        assertTrue(response.isSuccess());
        assertEquals("Test", response.getMessage());
        assertEquals(42, response.getData());
    }
}

// Testy dla TaskStatsResponse.java
class TaskStatsResponseTest {

    @Test
    void testBuilderWithAllFields() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(7);

        TaskStatsResponse stats = TaskStatsResponse.builder()
                .taskId(1L)
                .taskTitle("Test Task")
                .taskStatus("IN_PROGRESS")
                .taskDeadline(deadline)
                .taskCreatedAt(LocalDateTime.now())
                .totalAssignments(10)
                .completedAssignments(5)
                .inProgressAssignments(3)
                .pendingAssignments(2)
                .rejectedAssignments(0)
                .totalFiles(15)
                .totalFileSize(1024L * 1024L) // 1 MB
                .uniqueUsersWithFiles(5)
                .completionRate(50.0)
                .participationRate(50.0)
                .averageFilesPerUser(3.0)
                .isOverdue(false)
                .daysUntilDeadline(7L)
                .timeRemaining("7 dni")
                .averageCompletionTime(24.5)
                .activeUsersCount(8)
                .inactiveUsersCount(2)
                .build();

        assertEquals(1L, stats.getTaskId());
        assertEquals("Test Task", stats.getTaskTitle());
        assertEquals(10, stats.getTotalAssignments());
        assertEquals(5, stats.getCompletedAssignments());
        assertEquals(50.0, stats.getCompletionRate());
    }

    @Test
    void testFormattedMethods() {
        TaskStatsResponse stats = TaskStatsResponse.builder()
                .completionRate(85.5)
                .participationRate(75.25)
                .totalFileSize(1536L) // 1.5 KB
                .averageCompletionTime(0.5) // 0.5 hour = 30 minutes
                .build();

        assertEquals("85,5%", stats.getCompletionRateFormatted());
        assertEquals("75,3%", stats.getParticipationRateFormatted());
        assertTrue(stats.getTotalFileSizeFormatted().contains("KB"));
        assertTrue(stats.getAverageCompletionTimeFormatted().contains("min"));
    }

    @Test
    void testProgressStatusAndColor() {
        // Test DOSKONAŁY
        TaskStatsResponse excellent = TaskStatsResponse.builder()
                .completionRate(95.0)
                .build();
        assertEquals("DOSKONAŁY", excellent.getProgressStatus());
        assertEquals("success", excellent.getProgressColor());

        // Test DOBRY
        TaskStatsResponse good = TaskStatsResponse.builder()
                .completionRate(80.0)
                .build();
        assertEquals("DOBRY", good.getProgressStatus());
        assertEquals("info", good.getProgressColor());

        // Test ŚREDNI
        TaskStatsResponse average = TaskStatsResponse.builder()
                .completionRate(60.0)
                .build();
        assertEquals("ŚREDNI", average.getProgressStatus());
        assertEquals("warning", average.getProgressColor());

        // Test SŁABY
        TaskStatsResponse poor = TaskStatsResponse.builder()
                .completionRate(30.0)
                .build();
        assertEquals("SŁABY", poor.getProgressStatus());
        assertEquals("orange", poor.getProgressColor());

        // Test KRYTYCZNY
        TaskStatsResponse critical = TaskStatsResponse.builder()
                .completionRate(10.0)
                .build();
        assertEquals("KRYTYCZNY", critical.getProgressStatus());
        assertEquals("danger", critical.getProgressColor());

        // Test BRAK_DANYCH
        TaskStatsResponse noData = TaskStatsResponse.builder()
                .completionRate(null)
                .build();
        assertEquals("BRAK_DANYCH", noData.getProgressStatus());
        assertEquals("secondary", noData.getProgressColor());
    }

    @Test
    void testPriorityMethods() {
        TaskStatsResponse highPriority = TaskStatsResponse.builder()
                .isOverdue(true)
                .daysUntilDeadline(0L)
                .build();
        assertTrue(highPriority.isHighPriority());

        TaskStatsResponse needsAttention = TaskStatsResponse.builder()
                .completionRate(40.0)
                .isOverdue(true)
                .daysUntilDeadline(2L)
                .build();
        assertTrue(needsAttention.needsAttention());
    }

    @Test
    void testBuilderHelperMethods() {
        TaskStatsResponse.TaskStatsResponseBuilder builder = TaskStatsResponse.builder()
                .totalAssignments(20)
                .completedAssignments(15)
                .uniqueUsersWithFiles(10)
                .totalFiles(30)
                .calculateRates();

        assertEquals(75.0, builder.completionRate);
        assertEquals(50.0, builder.participationRate);
        assertEquals(3.0, builder.averageFilesPerUser);
    }

    @Test
    void testTimeMetricsCalculation() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(3).plusHours(5);

        TaskStatsResponse.TaskStatsResponseBuilder builder = TaskStatsResponse.builder()
                .calculateTimeMetrics(deadline);

        assertEquals(3L, builder.daysUntilDeadline);
        assertNotNull(builder.timeRemaining);
    }

    @Test
    void testEmpty() {
        TaskStatsResponse empty = TaskStatsResponse.empty();

        assertEquals(0, empty.getTotalAssignments());
        assertEquals(0, empty.getCompletedAssignments());
        assertEquals(0.0, empty.getCompletionRate());
        assertEquals(0L, empty.getTotalFileSize());

    }

    @Test
    void testFromBasicStats() {
        TaskStatsResponse stats = TaskStatsResponse.fromBasicStats(
                1L, "Test Task", "ACTIVE", 10, 5, 8);

        assertEquals(1L, stats.getTaskId());
        assertEquals("Test Task", stats.getTaskTitle());
        assertEquals("ACTIVE", stats.getTaskStatus());
        assertEquals(10, stats.getTotalAssignments());
        assertEquals(5, stats.getCompletedAssignments());
        assertEquals(8, stats.getTotalFiles());
        assertEquals(50.0, stats.getCompletionRate());
    }
}

// Testy dla MeetingTaskEditResponse.java
class MeetingTaskEditResponseTest {

    @Test
    void testBuilder() {
        Meeting meeting = new Meeting();
        Task task = new Task();

        MeetingTaskEditResponse response = MeetingTaskEditResponse.builder()
                .meeting(meeting)
                .task(task)
                .formattedDeadline("2024-12-31 23:59")
                .build();

        assertEquals(meeting, response.getMeeting());
        assertEquals(task, response.getTask());
        assertEquals("2024-12-31 23:59", response.getFormattedDeadline());
    }
}

// Testy dla MeetingParticipationInfo.java
class MeetingParticipationInfoTest {

    @Test
    void testBuilderWithPermissions() {
        MeetingParticipationInfo info = MeetingParticipationInfo.builder()
                .isOrganizer(true)
                .isParticipant(true)
                .isRelated(false)
                .participantRole("ORGANIZER")
                .permissions(List.of("EDIT", "DELETE", "MANAGE"))
                .canEdit(true)
                .canDelete(true)
                .canManageParticipants(true)
                .canJoin(false)
                .canViewDetails(true)
                .canUpload(true)
                .canDownload(true)
                .build();

        assertTrue(info.isOrganizer());
        assertTrue(info.isParticipant());
        assertFalse(info.isRelated());
        assertEquals("ORGANIZER", info.getParticipantRole());
        assertEquals(3, info.getPermissions().size());
        assertTrue(info.getPermissions().contains("EDIT"));
    }
}

// Testy dla MeetingResourceResponse.java
class MeetingResourceResponseTest {

    @Test
    void testBuilderWithEnums() {
        UserResponse uploader = UserResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .build();

        MeetingResourceResponse resource = MeetingResourceResponse.builder()
                .id(1L)
                .filename("document.pdf")
                .originalFilename("original_document.pdf")
                .description("Meeting notes")
                .fileSize(1024L * 1024L) // 1 MB
                .fileSizeFormatted("1 MB")
                .mimeType("application/pdf")
                .resourceType(ResourceType.DOCUMENT)
                .tags(Set.of("notes", "important"))
                .version(1)
                .isCurrent(true)
                .accessLevel(AccessLevel.PUBLIC)
                .uploadedBy(uploader)
                .uploadedAt(LocalDateTime.now())
                .downloadUrl("/download/1")
                .previewUrl("/preview/1")
                .canEdit(true)
                .canDelete(false)
                .build();

        assertEquals(1L, resource.getId());
        assertEquals("document.pdf", resource.getFilename());
        assertEquals("Meeting notes", resource.getDescription());
        assertEquals(ResourceType.DOCUMENT, resource.getResourceType());
        assertEquals(AccessLevel.PUBLIC, resource.getAccessLevel());
        assertEquals(uploader, resource.getUploadedBy());
        assertTrue(resource.getCanEdit());
        assertFalse(resource.getCanDelete());
        assertEquals(2, resource.getTags().size());
    }

    @Test
    void testNoArgsConstructor() {
        MeetingResourceResponse resource = new MeetingResourceResponse();
        resource.setId(1L);
        resource.setFilename("test.txt");

        assertEquals(1L, resource.getId());
        assertEquals("test.txt", resource.getFilename());
    }

    @Test
    void testAllArgsConstructor() {
        MeetingResourceResponse resource = new MeetingResourceResponse(
                1L, "test.txt", "test.txt", "Description",
                1024L, "1 KB", "text/plain", ResourceType.DOCUMENT,
                Set.of("test"), 1, true, AccessLevel.PUBLIC,
                null, LocalDateTime.now(), "/download/1",
                "/preview/1", true, false);

        assertEquals(1L, resource.getId());
        assertEquals("test.txt", resource.getFilename());
        assertEquals(ResourceType.DOCUMENT, resource.getResourceType());
    }
}

// Testy dla WinningOptionResponse.java
class WinningOptionResponseTest {

    @Test
    void testBuilderWithStatistics() {
        LocalDateTime optionDate = LocalDateTime.of(2024, 12, 25, 14, 0);

        WinningOptionResponse response = WinningOptionResponse.builder()
                .optionId(1L)
                .optionDate(optionDate)
                .durationMinutes(120)
                .voteCount(25)
                .percentage(62.5)
                .algorithmUsed("MAJORITY")
                .isTie(false)
                .totalVoters(40)
                .totalParticipants(50)
                .participationRate(80.0)
                .confidenceLevel("HIGH")
                .optionDisplayText("Dec 25, 2024 - 14:00 (2h)")
                .wasSuggested(true)
                .suggestedBy("John Doe")
                .firstChoiceVotes(15)
                .secondChoiceVotes(7)
                .thirdChoiceVotes(3)
                .build();

        assertEquals(1L, response.getOptionId());
        assertEquals(optionDate, response.getOptionDate());
        assertEquals(120, response.getDurationMinutes());
        assertEquals(25, response.getVoteCount());
        assertEquals(62.5, response.getPercentage());
        assertEquals("MAJORITY", response.getAlgorithmUsed());
        assertFalse(response.getIsTie());
        assertEquals(40, response.getTotalVoters());
        assertEquals(50, response.getTotalParticipants());
        assertEquals(80.0, response.getParticipationRate());
        assertEquals("HIGH", response.getConfidenceLevel());
        assertEquals("John Doe", response.getSuggestedBy());
        assertTrue(response.getWasSuggested());
    }

    @Test
    void testFormattedMethods() {
        WinningOptionResponse response = WinningOptionResponse.builder()
                .percentage(75.5)
                .optionDate(LocalDateTime.of(2024, 12, 25, 14, 30))
                .durationMinutes(90)
                .build();

        assertEquals("75,5%", response.getFormattedPercentage());
        assertEquals("25.12.2024 14:30", response.getFormattedDateTime());
        assertEquals("1h 30min", response.getFormattedDuration());
    }

    @Test
    void testDurationFormatting() {
        // Test minutes
        WinningOptionResponse minutes = WinningOptionResponse.builder()
                .durationMinutes(45)
                .build();
        assertEquals("45 min", minutes.getFormattedDuration());

        // Test hours
        WinningOptionResponse hours = WinningOptionResponse.builder()
                .durationMinutes(120)
                .build();
        assertEquals("2h", hours.getFormattedDuration());

        // Test hours with minutes
        WinningOptionResponse hoursMinutes = WinningOptionResponse.builder()
                .durationMinutes(135)
                .build();
        assertEquals("2h 15min", hoursMinutes.getFormattedDuration());
    }

    @Test
    void testConfidenceBadge() {
        WinningOptionResponse high = WinningOptionResponse.builder()
                .percentage(80.0)
                .build();
        assertEquals("success", high.getConfidenceBadge());

        WinningOptionResponse medium = WinningOptionResponse.builder()
                .percentage(60.0)
                .build();
        assertEquals("warning", medium.getConfidenceBadge());

        WinningOptionResponse low = WinningOptionResponse.builder()
                .percentage(40.0)
                .build();
        assertEquals("danger", low.getConfidenceBadge());

        WinningOptionResponse none = WinningOptionResponse.builder()
                .percentage(null)
                .build();
        assertEquals("secondary", none.getConfidenceBadge());
    }
}

// Testy dla CategoryResponse.java
class CategoryResponseTest {

    @Test
    void testBuilder() {
        CategoryResponse category = CategoryResponse.builder()
                .id(1L)
                .name("Development")
                .colorCode("#FF5733")
                .description("Software development meetings")
                .build();

        assertEquals(1L, category.getId());
        assertEquals("Development", category.getName());
        assertEquals("#FF5733", category.getColorCode());
        assertEquals("Software development meetings", category.getDescription());
    }

    @Test
    void testNoArgsConstructor() {
        CategoryResponse category = new CategoryResponse();
        category.setId(1L);
        category.setName("Test");

        assertEquals(1L, category.getId());
        assertEquals("Test", category.getName());
    }

    @Test
    void testAllArgsConstructor() {
        CategoryResponse category = new CategoryResponse(
                1L, "Test", "#000000", "Test category");

        assertEquals(1L, category.getId());
        assertEquals("Test", category.getName());
        assertEquals("#000000", category.getColorCode());
        assertEquals("Test category", category.getDescription());
    }

    @Test
    void testEqualsAndHashCode() {
        CategoryResponse cat1 = CategoryResponse.builder().id(1L).name("Test").build();
        CategoryResponse cat2 = CategoryResponse.builder().id(1L).name("Test").build();
        CategoryResponse cat3 = CategoryResponse.builder().id(2L).name("Different").build();

        assertEquals(cat1, cat2);
        assertNotEquals(cat1, cat3);
        assertEquals(cat1.hashCode(), cat2.hashCode());
        assertNotEquals(cat1.hashCode(), cat3.hashCode());
    }
}

// Testy dla NotificationResponse.java
class NotificationResponseTest {

    @Test
    void testNotificationResponse() {
        NotificationResponse notification = new NotificationResponse();
        notification.setId(1L);
        notification.setTitle("New Meeting");
        notification.setMessage("You have been invited to a meeting");
        notification.setType(NotificationType.MEETING_INVITATION);
        notification.setStatus(NotificationStatus.SENT);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setReadAt(null);
        notification.setTemplateVariables(Map.of("meetingName", "Team Sync"));
        notification.setReferenceId(123L);
        notification.setReferenceType("MEETING");

        assertEquals(1L, notification.getId());
        assertEquals("New Meeting", notification.getTitle());
        assertEquals(NotificationType.MEETING_INVITATION, notification.getType());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(NotificationChannel.EMAIL, notification.getChannel());
        assertNull(notification.getReadAt());
        assertEquals(1, notification.getTemplateVariables().size());
        assertEquals(123L, notification.getReferenceId());
        assertEquals("MEETING", notification.getReferenceType());
    }
}

// Testy dla StatisticsResponse.java
class StatisticsResponseTest {

    @Test
    void testBuilder() {
        StatisticsResponse stats = StatisticsResponse.builder()
                .totalMeetings(100L)
                .completedMeetings(80L)
                .cancelledMeetings(10L)
                .averageDuration(45.5)
                .totalParticipants(500L)
                .build();

        assertEquals(100L, stats.getTotalMeetings());
        assertEquals(80L, stats.getCompletedMeetings());
        assertEquals(10L, stats.getCancelledMeetings());
        assertEquals(45.5, stats.getAverageDuration());
        assertEquals(500L, stats.getTotalParticipants());
    }
}

// Testy dla EmailTemplateResponse.java
class EmailTemplateResponseTest {

    @Test
    void testEmailTemplateResponse() {
        EmailTemplateResponse template = new EmailTemplateResponse();
        template.setId(1L);
        template.setTemplateKey("MEETING_INVITATION");
        template.setName("Meeting Invitation");
        template.setSubject("You're invited to a meeting");
        template.setBodyTemplate("Dear {{userName}}, you're invited...");
        template.setLanguage("en");
        template.setCategory("Meeting");
        template.setDescription("Template for meeting invitations");
        template.setVariablesHelp("userName, meetingTitle, date");
        template.setIsActive(true);
        template.setVersion(1);
        template.setChannel("EMAIL");
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setAvailableVariables("userName,meetingTitle,date,time");

        assertEquals(1L, template.getId());
        assertEquals("MEETING_INVITATION", template.getTemplateKey());
        assertEquals("Meeting Invitation", template.getName());
        assertEquals("You're invited to a meeting", template.getSubject());
        assertTrue(template.getIsActive());
        assertEquals("EMAIL", template.getChannel());
        assertNotNull(template.getAvailableVariables());
    }
}

// Testy dla UserProfileResponse.java
class UserProfileResponseTest {

    @Test
    void testUserProfileResponse() {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(1L);
        profile.setEmail("john.doe@example.com");
        profile.setFirstName("John");
        profile.setLastName("Doe");
        profile.setPhoneNumber("123456789");
        profile.setRole("ADMIN");
        profile.setTimezone("UTC");
        profile.setLanguage("en");
        profile.setCreatedAt(LocalDateTime.now());
        profile.setEmailNotificationsEnabled(true);
        profile.setPushNotificationsEnabled(true);
        profile.setSmsNotificationsEnabled(false);
        profile.setDigestEnabled(true);
        profile.setDigestFrequency("DAILY");
        profile.setEnabledChannels(Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH));
        profile.setTotalNotifications(100L);
        profile.setUnreadNotifications(5L);
        profile.setUpcomingMeetings(3L);

        assertEquals(1L, profile.getId());
        assertEquals("john.doe@example.com", profile.getEmail());
        assertEquals("John", profile.getFirstName());
        assertEquals("Doe", profile.getLastName());
        assertEquals("ADMIN", profile.getRole());
        assertTrue(profile.getEmailNotificationsEnabled());
        assertFalse(profile.getSmsNotificationsEnabled());
        assertEquals(100L, profile.getTotalNotifications());
        assertEquals(5L, profile.getUnreadNotifications());
        assertEquals(3L, profile.getUpcomingMeetings());
        assertEquals(2, profile.getEnabledChannels().size());
    }
}

// Testy dla MeetingResourceStats.java
class MeetingResourceStatsTest {

    @Test
    void testBuilder() {
        MeetingResourceStats stats = MeetingResourceStats.builder()
                .totalResources(100L)
                .documentCount(40L)
                .presentationCount(20L)
                .imageCount(15L)
                .videoCount(10L)
                .audioCount(5L)
                .otherCount(10L)
                .totalSize(1024L * 1024L * 100) // 100 MB
                .totalSizeFormatted("100 MB")
                .build();

        assertEquals(100L, stats.getTotalResources());
        assertEquals(40L, stats.getDocumentCount());
        assertEquals(20L, stats.getPresentationCount());
        assertEquals(15L, stats.getImageCount());
        assertEquals(10L, stats.getVideoCount());
        assertEquals(5L, stats.getAudioCount());
        assertEquals(10L, stats.getOtherCount());
        assertEquals(1024L * 1024L * 100, stats.getTotalSize());
    }

    @Test
    void testNoArgsConstructor() {
        MeetingResourceStats stats = new MeetingResourceStats();
        stats.setTotalResources(10L);

        assertEquals(10L, stats.getTotalResources());
    }

    @Test
    void testAllArgsConstructor() {
        MeetingResourceStats stats = new MeetingResourceStats(
                10L, 5L, 2L, 1L, 1L, 1L, 0L,
                1024L * 1024L, "1 MB");

        assertEquals(10L, stats.getTotalResources());
        assertEquals(5L, stats.getDocumentCount());
    }
}

// Testy dla MeetingTaskAssignmentsResponse.java
class MeetingTaskAssignmentsResponseTest {

    @Test
    void testBuilderWithCollections() {
        Meeting meeting = new Meeting();
        Task task = new Task();
        User user1 = new User();
        User user2 = new User();
        TaskAssignment assignment = new TaskAssignment();

        MeetingTaskAssignmentsResponse response = MeetingTaskAssignmentsResponse.builder()
                .meeting(meeting)
                .task(task)
                .availableUsers(List.of(user1, user2))
                .assignedUsers(List.of(user1))
                .assignments(List.of(assignment))
                .build();

        assertEquals(meeting, response.getMeeting());
        assertEquals(task, response.getTask());
        assertEquals(2, response.getAvailableUsers().size());
        assertEquals(1, response.getAssignedUsers().size());
        assertEquals(1, response.getAssignments().size());
        assertTrue(response.getAvailableUsers().contains(user1));
        assertTrue(response.getAssignedUsers().contains(user1));
    }
}