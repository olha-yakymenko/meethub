// MeetingResourceRepository.java
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.MeetingResource;
import com.meethub.domain.model.enums.AccessLevel;
import com.meethub.domain.model.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingResourceRepository extends JpaRepository<MeetingResource, Long> {


    List<MeetingResource> findByMeetingIdAndIsCurrentTrueOrderByUploadedAtDesc(Long meetingId);

    List<MeetingResource> findByMeetingIdAndResourceTypeOrderByUploadedAtDesc(Long meetingId, ResourceType resourceType);

    List<MeetingResource> findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(Long meetingId, String tag);

}