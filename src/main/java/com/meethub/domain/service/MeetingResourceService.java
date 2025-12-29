// MeetingResourceService.java
package com.meethub.domain.service;

import com.meethub.domain.model.enums.ResourceType;
import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
import com.meethub.domain.model.response.MeetingResourceResponse;
import com.meethub.domain.model.response.MeetingResourceStats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public interface MeetingResourceService {

    MeetingResourceResponse addResource(
            @NotNull Long meetingId,
            @Valid MeetingResourceRequest request,
            @NotNull Long userId
    );

    List<MeetingResourceResponse> getMeetingResources(@NotNull Long meetingId, @NotNull Long userId);
    MeetingResourceResponse getResource(@NotNull Long resourceId, @NotNull Long userId);

    MeetingResourceResponse updateResource(
            @NotNull Long resourceId,
            @Valid UpdateMeetingResourceRequest request,
            @NotNull Long userId
    );

    void deleteResource(@NotNull Long resourceId, @NotNull Long userId);

    List<MeetingResourceResponse> getResourcesByType(
            @NotNull Long meetingId,
            @NotNull ResourceType resourceType,
            @NotNull Long userId
    );

    List<MeetingResourceResponse> getResourcesByTag(
            @NotNull Long meetingId,
            String tag,
            @NotNull Long userId
    );

    MeetingResourceStats getMeetingResourceStats(@NotNull Long meetingId, @NotNull Long userId);
}