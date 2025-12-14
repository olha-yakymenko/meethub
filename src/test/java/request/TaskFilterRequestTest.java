package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TaskFilterRequestTest {

    @Test
    void shouldCreateValidTaskFilter() {
        // Given
        var filter = TaskFilterRequest.builder()
                .title("Report")
                .status(TaskStatus.IN_PROGRESS)
                .deadlineFrom(LocalDateTime.now())
                .deadlineTo(LocalDateTime.now().plusDays(7))
                .assignedToUserId(123L)
                .overdueOnly(true)
                .page(1)
                .size(50)
                .sortBy("deadline")
                .ascending(true)
                .build();

        // Then
        assertAll(
                () -> assertEquals("Report", filter.getTitle()),
                () -> assertEquals(TaskStatus.IN_PROGRESS, filter.getStatus()),
                () -> assertNotNull(filter.getDeadlineFrom()),
                () -> assertNotNull(filter.getDeadlineTo()),
                () -> assertEquals(123L, filter.getAssignedToUserId()),
                () -> assertTrue(filter.getOverdueOnly()),
                () -> assertEquals(1, filter.getPage()),
                () -> assertEquals(50, filter.getSize()),
                () -> assertEquals("deadline", filter.getSortBy()),
                () -> assertTrue(filter.getAscending()),
                () -> assertTrue(filter.hasTitleFilter()),
                () -> assertTrue(filter.hasStatusFilter()),
                () -> assertTrue(filter.hasDateFilter()),
                () -> assertTrue(filter.hasAssignmentFilter())
        );
    }

    @Test
    void shouldHaveDefaultValues() {
        // Given
        var filter = TaskFilterRequest.builder().build();

        // Then
        assertAll(
                () -> assertEquals(0, filter.getPage()),
                () -> assertEquals(20, filter.getSize()),
                () -> assertEquals("createdAt", filter.getSortBy()),
                () -> assertFalse(filter.getAscending()),
                () -> assertFalse(filter.hasTitleFilter()),
                () -> assertFalse(filter.hasStatusFilter()),
                () -> assertFalse(filter.hasDateFilter()),
                () -> assertFalse(filter.hasAssignmentFilter())
        );
    }
}