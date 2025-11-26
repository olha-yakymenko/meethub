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

    List<MeetingResource> findByMeetingIdOrderByUploadedAtDesc(Long meetingId);

    List<MeetingResource> findByMeetingIdAndIsCurrentTrueOrderByUploadedAtDesc(Long meetingId);

    List<MeetingResource> findByMeetingIdAndResourceTypeOrderByUploadedAtDesc(Long meetingId, ResourceType resourceType);

    List<MeetingResource> findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(Long meetingId, String tag);

    List<MeetingResource> findByMeetingIdAndAccessLevelOrderByUploadedAtDesc(Long meetingId, AccessLevel accessLevel);

    @Query("SELECT mr FROM MeetingResource mr WHERE mr.meeting.id = :meetingId AND mr.uploadedBy.id = :userId")
    List<MeetingResource> findByMeetingIdAndUploadedBy(@Param("meetingId") Long meetingId,
                                                       @Param("userId") Long userId);

    Optional<MeetingResource> findByIdAndMeetingId(Long id, Long meetingId);

    boolean existsByMeetingIdAndOriginalFilename(Long meetingId, String originalFilename);

    Long countByMeetingId(Long meetingId);

    Long countByMeetingIdAndResourceType(Long meetingId, ResourceType resourceType);

    @Query("SELECT DISTINCT mr.tags FROM MeetingResource mr WHERE mr.meeting.id = :meetingId")
    List<String> findDistinctTagsByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT mr FROM MeetingResource mr WHERE mr.meeting.id = :meetingId AND mr.isCurrent = true")
    List<MeetingResource> findCurrentResourcesByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT mr FROM MeetingResource mr WHERE mr.filename = :filename AND mr.isCurrent = true")
    Optional<MeetingResource> findByFilenameAndCurrent(@Param("filename") String filename);
}