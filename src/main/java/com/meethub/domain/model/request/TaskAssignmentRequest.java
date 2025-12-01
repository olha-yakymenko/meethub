// TaskAssignmentRequest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignmentRequest {

    @NotNull(message = "ID użytkownika jest wymagane")
    private Long userId;

    private String comment; // Opcjonalny komentarz przy przypisaniu

    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;
}