// TaskFilterRequestTest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TaskFilterRequestTest {

    @Test
    void testCompleteFilterRequest() {
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime to = LocalDateTime.of(2024, 12, 31, 23, 59);

        TaskFilterRequest filter = TaskFilterRequest.builder()
                .title("urgent report")
                .status(TaskStatus.IN_PROGRESS)
                .deadlineFrom(from)
                .deadlineTo(to)
                .assignedToUserId(123L)
                .overdueOnly(true)
                .page(1)
                .size(50)
                .sortBy("deadline")
                .ascending(true)
                .build();

        assertAll("Complete task filter request",
                () -> assertEquals("urgent report", filter.getTitle(),
                        "Title should match"),
                () -> assertEquals(TaskStatus.IN_PROGRESS, filter.getStatus(),
                        "Status should be IN_PROGRESS"),
                () -> assertEquals(from, filter.getDeadlineFrom(),
                        "Deadline from should match"),
                () -> assertEquals(to, filter.getDeadlineTo(),
                        "Deadline to should match"),
                () -> assertEquals(123L, filter.getAssignedToUserId(),
                        "Assigned user ID should match"),
                () -> assertTrue(filter.getOverdueOnly(),
                        "Overdue only should be true"),
                () -> assertEquals(1, filter.getPage(),
                        "Page should be 1"),
                () -> assertEquals(50, filter.getSize(),
                        "Size should be 50"),
                () -> assertEquals("deadline", filter.getSortBy(),
                        "Sort by should be 'deadline'"),
                () -> assertTrue(filter.getAscending(),
                        "Ascending should be true")
        );
    }

    @Test
    void testHelperMethods() {
        TaskFilterRequest emptyFilter = TaskFilterRequest.builder().build();

        TaskFilterRequest fullFilter = TaskFilterRequest.builder()
                .title("test")
                .status(TaskStatus.COMPLETED)
                .deadlineFrom(LocalDateTime.now())
                .assignedToUserId(456L)
                .overdueOnly(false)
                .build();

        assertAll("Helper methods",
                () -> assertFalse(emptyFilter.hasTitleFilter(),
                        "Empty filter should not have title filter"),
                () -> assertFalse(emptyFilter.hasStatusFilter(),
                        "Empty filter should not have status filter"),
                () -> assertFalse(emptyFilter.hasDateFilter(),
                        "Empty filter should not have date filter"),
                () -> assertFalse(emptyFilter.hasAssignmentFilter(),
                        "Empty filter should not have assignment filter"),

                () -> assertTrue(fullFilter.hasTitleFilter(),
                        "Full filter should have title filter"),
                () -> assertTrue(fullFilter.hasStatusFilter(),
                        "Full filter should have status filter"),
                () -> assertTrue(fullFilter.hasDateFilter(),
                        "Full filter should have date filter"),
                () -> assertTrue(fullFilter.hasAssignmentFilter(),
                        "Full filter should have assignment filter")
        );
    }

    @Test
    void testDefaultValues() {
        TaskFilterRequest filter = new TaskFilterRequest();

        assertAll("Default values",
                () -> assertEquals(0, filter.getPage(),
                        "Default page should be 0"),
                () -> assertEquals(20, filter.getSize(),
                        "Default size should be 20"),
                () -> assertEquals("createdAt", filter.getSortBy(),
                        "Default sort by should be 'createdAt'"),
                () -> assertFalse(filter.getAscending(),
                        "Default ascending should be false"),
                () -> assertNull(filter.getOverdueOnly(),
                        "Default overdueOnly should be null")
        );
    }
}