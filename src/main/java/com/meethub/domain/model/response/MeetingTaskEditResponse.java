package com.meethub.domain.model.response;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.Task;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeetingTaskEditResponse {
    private Meeting meeting;
    private Task task;
    private String formattedDeadline;
}
