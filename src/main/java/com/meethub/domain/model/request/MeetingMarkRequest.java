package com.meethub.domain.model.request;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class MeetingMarkRequest {
    @NotNull
    private Long userId;
}
