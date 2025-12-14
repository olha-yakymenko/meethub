package com.meethub.domain.service;

import com.meethub.domain.model.enums.ResourceType;
import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
import com.meethub.domain.model.response.MeetingResourceResponse;
import com.meethub.domain.model.response.MeetingResourceStats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface MeetingResourceService {

    MeetingResourceResponse addResource(
            @NotNull @Positive Long meetingId,
            @Valid MeetingResourceRequest request,
            @NotNull @Positive Long userId
    );

    List<MeetingResourceResponse> getMeetingResources(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    MeetingResourceResponse getResource(
            @NotNull @Positive Long resourceId,
            @NotNull @Positive Long userId
    );

    MeetingResourceResponse updateResource(
            @NotNull @Positive Long resourceId,
            @Valid UpdateMeetingResourceRequest request,
            @NotNull @Positive Long userId
    );

    void deleteResource(
            @NotNull @Positive Long resourceId,
            @NotNull @Positive Long userId
    );

    List<MeetingResourceResponse> getResourcesByType(
            @NotNull @Positive Long meetingId,
            @NotNull ResourceType resourceType,
            @NotNull @Positive Long userId
    );

    List<MeetingResourceResponse> getResourcesByTag(
            @NotNull @Positive Long meetingId,
            @NotBlank String tag,
            @NotNull @Positive Long userId
    );

    MeetingResourceStats getMeetingResourceStats(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );
}




//// MeetingResourceService.java
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.enums.ResourceType;
//import com.meethub.domain.model.request.MeetingResourceRequest;
//import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
//import com.meethub.domain.model.response.MeetingResourceResponse;
//import com.meethub.domain.model.response.MeetingResourceStats;
//
//import java.util.List;
//
//public interface MeetingResourceService {
//
//    MeetingResourceResponse addResource(Long meetingId, MeetingResourceRequest request, Long userId);
//
//    List<MeetingResourceResponse> getMeetingResources(Long meetingId, Long userId);
//
//    MeetingResourceResponse getResource(Long resourceId, Long userId);
//
//    MeetingResourceResponse updateResource(Long resourceId, UpdateMeetingResourceRequest request, Long userId);
//
//    void deleteResource(Long resourceId, Long userId);
//
//    List<MeetingResourceResponse> getResourcesByType(Long meetingId, ResourceType resourceType, Long userId);
//
//    List<MeetingResourceResponse> getResourcesByTag(Long meetingId, String tag, Long userId);
//
//    MeetingResourceStats getMeetingResourceStats(Long meetingId, Long userId);
//}