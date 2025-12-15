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

@DataJpaTest
@ActiveProfiles("postgres")
class MeetingResourceRepositoryTest {

    @Autowired
    private MeetingResourceRepository meetingResourceRepository;

    @Test
    void testFindByMeetingIdAndIsCurrentTrueOrderByUploadedAtDesc() {
        List<MeetingResource> resources = meetingResourceRepository
                .findByMeetingIdAndIsCurrentTrueOrderByUploadedAtDesc(1L);

        assertThat(resources).hasSize(3);
        assertThat(resources.get(0).getFilename()).isEqualTo("presentation1.pptx"); // najnowszy upload
        assertThat(resources.get(1).getFilename()).isEqualTo("file1.pdf");
    }

    @Test
    void testFindByMeetingIdAndResourceTypeOrderByUploadedAtDesc() {
        List<MeetingResource> documentResources = meetingResourceRepository
                .findByMeetingIdAndResourceTypeOrderByUploadedAtDesc(1L, ResourceType.DOCUMENT);

        assertThat(documentResources).hasSize(2);
        assertThat(documentResources.get(0).getFilename()).isEqualTo("file1.pdf");
        assertThat(documentResources.get(1).getFilename()).isEqualTo("file2.docx");
    }

    @Test
    void testFindByMeetingIdAndTagsContainingOrderByUploadedAtDesc() {
        // Given: Dane z data.sql
        // W data.sql spotkanie ma ID=1, a w zasobach są takie tagi:
        // - resource_id=1: tagi "pdf", "important" (dla file1.pdf)
        // - resource_id=2: tag "docx" (dla file2.docx)
        // - resource_id=3: tag "presentation" (dla presentation1.pptx)
        Long meetingId = 1L; // ID spotkania z data.sql

        // When: Szukamy zasobów z tagiem zawierającym "pdf"
        List<MeetingResource> pdfResources = meetingResourceRepository
                .findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(meetingId, "pdf");

        // Then: Powinien znaleźć file1.pdf
        assertThat(pdfResources)
                .hasSize(1)
                .first()
                .satisfies(resource -> {
                    assertThat(resource.getFilename()).isEqualTo("file1.pdf");
                    assertThat(resource.getOriginalFilename()).isEqualTo("file1.pdf");
                });

        // When: Szukamy zasobów z tagiem zawierającym "presentation"
        List<MeetingResource> presentationResources = meetingResourceRepository
                .findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(meetingId, "presentation");

        // Then: Powinien znaleźć presentation1.pptx
        assertThat(presentationResources)
                .hasSize(1)
                .first()
                .satisfies(resource -> {
                    assertThat(resource.getFilename()).isEqualTo("presentation1.pptx");
                    assertThat(resource.getResourceType()).isEqualTo(PRESENTATION);
                });

        // When: Szukamy zasobów z tagiem zawierającym "important"
        List<MeetingResource> importantResources = meetingResourceRepository
                .findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(meetingId, "important");

        // Then: Powinien znaleźć file1.pdf (który ma tag "important")
        assertThat(importantResources)
                .hasSize(1)
                .first()
                .extracting(MeetingResource::getFilename)
                .isEqualTo("file1.pdf");

        // Dodatkowe asercje na kolejność sortowania
        // file1.pdf ma uploaded_at = DATEADD('DAY', -2, CURRENT_TIMESTAMP)
        // presentation1.pptx ma uploaded_at = DATEADD('DAY', -1, CURRENT_TIMESTAMP) - czyli nowszy
        List<MeetingResource> allResources = meetingResourceRepository
                .findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(meetingId, "");

        // Puste tag zwróci wszystkie zasoby (LIKE '%%')
        assertThat(allResources).hasSize(3);

        // Sprawdzenie kolejności - najnowsze pierwsze
        if (allResources.size() >= 2) {
            assertThat(allResources.get(0).getUploadedAt())
                    .isAfterOrEqualTo(allResources.get(1).getUploadedAt());
        }
}
}
