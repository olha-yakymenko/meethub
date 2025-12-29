package com.meethub.domain.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Builder
public class StatusChangeResponse {

        private String oldStatus;
        private String newStatus;
        private LocalDateTime changedAt;
        private String changedByName;
        private String reason;

}
