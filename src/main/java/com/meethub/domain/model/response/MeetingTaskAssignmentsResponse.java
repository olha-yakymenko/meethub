package com.meethub.domain.model.response;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.entity.TaskAssignment;
import com.meethub.domain.model.entity.User;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MeetingTaskAssignmentsResponse {
    private Meeting meeting;
    private Task task;
    private List<User> availableUsers;
    private List<User> assignedUsers;
    private List<TaskAssignment> assignments;
}
