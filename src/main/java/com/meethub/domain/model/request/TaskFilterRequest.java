// TaskFilterRequest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskFilterRequest {

    private String title;
    private TaskStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deadlineFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deadlineTo;

    private Long assignedToUserId;
    private Boolean overdueOnly;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private Boolean ascending = false;

    // Metody pomocnicze
    public boolean hasTitleFilter() {
        return title != null && !title.trim().isEmpty();
    }

    public boolean hasStatusFilter() {
        return status != null;
    }

    public boolean hasDateFilter() {
        return deadlineFrom != null || deadlineTo != null;
    }

    public boolean hasAssignmentFilter() {
        return assignedToUserId != null;
    }
}