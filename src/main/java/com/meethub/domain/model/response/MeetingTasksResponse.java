package com.meethub.domain.model.response;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.Task;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MeetingTasksResponse {
    private Meeting meeting;
    private List<Task> tasks;
    private boolean isOrganizer;
}
