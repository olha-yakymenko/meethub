package com.meethub.domain.model.request;

import lombok.Data;

import java.util.List;

@Data
public class BulkInviteRequest {
    private List<String> emails;
    private List<Long> userIds;
    private String groupName;
    private String customMessage;
}