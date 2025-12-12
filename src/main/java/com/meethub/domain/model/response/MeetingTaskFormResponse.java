package com.meethub.domain.model.response;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.request.CreateTaskRequest;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeetingTaskFormResponse {
    private Meeting meeting;
    private CreateTaskRequest createTaskRequest;
}
