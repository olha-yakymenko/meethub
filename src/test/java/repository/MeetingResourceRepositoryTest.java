package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.MeetingResource;
import com.meethub.domain.model.enums.ResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.meethub.domain.model.enums.ResourceType.PRESENTATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@ActiveProfiles("postgres")
class MeetingResourceRepositoryTest {

    @Autowired
    private MeetingResourceRepository meetingResourceRepository;

    @Test
    void testFindByMeetingIdAndIsCurrentTrueOrderByUploadedAtDesc() {
        List<MeetingResource> resources = meetingResourceRepository
                .findByMeetingIdAndIsCurrentTrueOrderByUploadedAtDesc(1L);

        assertAll("Current meeting resources ordered by uploadedAt desc",
                () -> assertThat(resources).hasSize(3),
                () -> assertThat(resources.get(0).getFilename()).isEqualTo("presentation1.pptx"),
                () -> assertThat(resources.get(1).getFilename()).isEqualTo("file1.pdf")
        );
    }

    @Test
    void testFindByMeetingIdAndResourceTypeOrderByUploadedAtDesc() {
        List<MeetingResource> documentResources = meetingResourceRepository
                .findByMeetingIdAndResourceTypeOrderByUploadedAtDesc(1L, ResourceType.DOCUMENT);

        assertAll("Document resources ordered by uploadedAt desc",
                () -> assertThat(documentResources).hasSize(2),
                () -> assertThat(documentResources.get(0).getFilename()).isEqualTo("file1.pdf"),
                () -> assertThat(documentResources.get(1).getFilename()).isEqualTo("file2.docx")
        );
    }

    @Test
    void testFindByMeetingIdAndTagsContainingOrderByUploadedAtDesc() {
        Long meetingId = 1L;

        List<MeetingResource> pdfResources = meetingResourceRepository
                .findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(meetingId, "pdf");

        List<MeetingResource> presentationResources = meetingResourceRepository
                .findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(meetingId, "presentation");

        List<MeetingResource> importantResources = meetingResourceRepository
                .findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(meetingId, "important");

        List<MeetingResource> allResources = meetingResourceRepository
                .findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(meetingId, "");

        assertAll("Meeting resources by tags",
                () -> assertAll("PDF resources",
                        () -> assertThat(pdfResources).hasSize(1),
                        () -> assertThat(pdfResources.get(0).getFilename()).isEqualTo("file1.pdf"),
                        () -> assertThat(pdfResources.get(0).getOriginalFilename()).isEqualTo("file1.pdf")
                ),
                () -> assertAll("Presentation resources",
                        () -> assertThat(presentationResources).hasSize(1),
                        () -> assertThat(presentationResources.get(0).getFilename()).isEqualTo("presentation1.pptx"),
                        () -> assertThat(presentationResources.get(0).getResourceType()).isEqualTo(PRESENTATION)
                ),
                () -> assertAll("Important resources",
                        () -> assertThat(importantResources).hasSize(1),
                        () -> assertThat(importantResources.get(0).getFilename()).isEqualTo("file1.pdf")
                ),
                () -> assertAll("All resources ordering",
                        () -> assertThat(allResources).hasSize(3),
                        () -> {
                            if (allResources.size() >= 2) {
                                assertThat(allResources.get(0).getUploadedAt())
                                        .isAfterOrEqualTo(allResources.get(1).getUploadedAt());
                            }
                        }
                )
        );
    }
}
