package com.meethub.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingMarkResponse {
    private Long meetingId;
    private Long userId;
    private boolean important;
    private String message; // np. "Spotkanie oznaczone jako ważne"
}
