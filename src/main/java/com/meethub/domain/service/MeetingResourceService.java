// MeetingResourceService.java
package com.meethub.domain.service;

import com.meethub.domain.model.enums.ResourceType;
import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
import com.meethub.domain.model.response.MeetingResourceResponse;
import com.meethub.domain.model.response.MeetingResourceStats;

import java.util.List;

public interface MeetingResourceService {

    MeetingResourceResponse addResource(Long meetingId, MeetingResourceRequest request, Long userId);

    List<MeetingResourceResponse> getMeetingResources(Long meetingId, Long userId);

    MeetingResourceResponse getResource(Long resourceId, Long userId);

    MeetingResourceResponse updateResource(Long resourceId, UpdateMeetingResourceRequest request, Long userId);

    void deleteResource(Long resourceId, Long userId);

    List<MeetingResourceResponse> getResourcesByType(Long meetingId, ResourceType resourceType, Long userId);

    List<MeetingResourceResponse> getResourcesByTag(Long meetingId, String tag, Long userId);

    MeetingResourceStats getMeetingResourceStats(Long meetingId, Long userId);
}