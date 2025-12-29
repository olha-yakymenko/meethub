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

    @Query(value = "SELECT DISTINCT mr.* FROM meethub_schema.meeting_resources mr " +
            "JOIN meethub_schema.resource_tags rt ON mr.id = rt.resource_id " +
            "WHERE mr.meeting_id = :meetingId AND rt.tag LIKE CONCAT('%', :tag, '%') " +
            "ORDER BY mr.uploaded_at DESC", nativeQuery = true)
    List<MeetingResource> findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(
            @Param("meetingId") Long meetingId,
            @Param("tag") String tag);

}