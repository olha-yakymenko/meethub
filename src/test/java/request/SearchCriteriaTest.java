package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchCriteriaTest {

    @Test
    void shouldCreateValidSearchCriteria() {
        // Given
        var criteria = SearchCriteria.builder()
                .keywords("team meeting")
                .tags("urgent,important")
                .dateFrom(LocalDate.now().minusDays(30))
                .dateTo(LocalDate.now().plusDays(30))
                .type(MeetingType.ONLINE)
                .statuses(List.of("ACTIVE", "PLANNED"))
                .minParticipants(5)
                .maxParticipants(50)
                .organizerName("John Doe")
                .myParticipation("INVITED")
                .visibility(MeetingVisibility.PUBLIC)
                .categoryIds(List.of(1L, 2L, 3L))
                .recurringOnly(true)
                .templatesOnly(false)
                .hasAttachments(true)
                .build();

        // Then
        assertAll(
                () -> assertEquals("team meeting", criteria.getKeywords()),
                () -> assertEquals("urgent,important", criteria.getTags()),
                () -> assertEquals(MeetingType.ONLINE, criteria.getType()),
                () -> assertEquals(2, criteria.getStatuses().size()),
                () -> assertEquals(5, criteria.getMinParticipants()),
                () -> assertEquals(50, criteria.getMaxParticipants()),
                () -> assertEquals("John Doe", criteria.getOrganizerName()),
                () -> assertEquals("INVITED", criteria.getMyParticipation()),
                () -> assertEquals(MeetingVisibility.PUBLIC, criteria.getVisibility()),
                () -> assertEquals(3, criteria.getCategoryIds().size()),
                () -> assertTrue(criteria.getRecurringOnly()),
                () -> assertFalse(criteria.getTemplatesOnly()),
                () -> assertTrue(criteria.getHasAttachments()),

                // Test helper methods
                () -> assertTrue(criteria.hasKeywords()),
                () -> assertTrue(criteria.hasTags()),
                () -> assertTrue(criteria.hasDateRange()),
                () -> assertTrue(criteria.hasType()),
                () -> assertTrue(criteria.hasStatuses()),
                () -> assertTrue(criteria.hasParticipantsFilter()),
                () -> assertTrue(criteria.hasOrganizerFilter()),
                () -> assertTrue(criteria.hasMyParticipationFilter()),
                () -> assertTrue(criteria.hasVisibilityFilter()),
                () -> assertTrue(criteria.hasCategoryFilter()),
                () -> assertTrue(criteria.hasRecurringFilter()),
                () -> assertFalse(criteria.hasTemplatesFilter()),
                () -> assertTrue(criteria.hasAttachmentsFilter()),
                () -> assertFalse(criteria.isEmpty()),

                // Test tags parsing
                () -> assertEquals(2, criteria.getTagsList().size()),
                () -> assertTrue(criteria.getTagsList().contains("urgent")),
                () -> assertTrue(criteria.getTagsList().contains("important"))
        );
    }

    @Test
    void shouldDetectEmptyCriteria() {
        // Given
        var criteria = SearchCriteria.builder().build();

        // Then
        assertAll(
                () -> assertTrue(criteria.isEmpty()),
                () -> assertFalse(criteria.hasKeywords()),
                () -> assertFalse(criteria.hasTags()),
                () -> assertFalse(criteria.hasDateRange()),
                () -> assertFalse(criteria.hasType()),
                () -> assertFalse(criteria.hasStatuses()),
                () -> assertFalse(criteria.hasParticipantsFilter()),
                () -> assertFalse(criteria.hasOrganizerFilter()),
                () -> assertFalse(criteria.hasMyParticipationFilter()),
                () -> assertFalse(criteria.hasVisibilityFilter()),
                () -> assertFalse(criteria.hasCategoryFilter()),
                () -> assertFalse(criteria.hasRecurringFilter()),
                () -> assertFalse(criteria.hasTemplatesFilter()),
                () -> assertFalse(criteria.hasAttachmentsFilter()),
                () -> assertTrue(criteria.getTagsList().isEmpty())
        );
    }
}