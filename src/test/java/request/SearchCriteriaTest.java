// SearchCriteriaTest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchCriteriaTest {

    @Test
    void testBuilderWithAllFields() {
        SearchCriteria criteria = SearchCriteria.builder()
                .keywords("team meeting")
                .tags("business,quarterly")
                .searchFields(Arrays.asList("title", "description"))
                .dateFrom(LocalDate.of(2024, 1, 1))
                .dateTo(LocalDate.of(2024, 12, 31))
                .type(MeetingType.ONLINE)
                .statuses(Arrays.asList("UPCOMING", "IN_PROGRESS"))
                .minParticipants(5)
                .maxParticipants(50)
                .organizerName("John Doe")
                .myParticipation("organizer")
                .visibility(MeetingVisibility.PUBLIC)
                .categoryIds(Arrays.asList(1L, 2L, 3L))
                .recurringOnly(true)
                .templatesOnly(false)
                .hasAttachments(true)
                .currentUserId(123L)
                .userAuthenticated(true)
                .includePublic(true)
                .build();

        assertAll("Complete SearchCriteria builder test",
                () -> assertEquals("team meeting", criteria.getKeywords(),
                        "Keywords should match"),
                () -> assertEquals("business,quarterly", criteria.getTags(),
                        "Tags should match"),
                () -> assertEquals(2, criteria.getSearchFields().size(),
                        "Should have 2 search fields"),
                () -> assertEquals(LocalDate.of(2024, 1, 1), criteria.getDateFrom(),
                        "Date from should match"),
                () -> assertEquals(LocalDate.of(2024, 12, 31), criteria.getDateTo(),
                        "Date to should match"),
                () -> assertEquals(MeetingType.ONLINE, criteria.getType(),
                        "Meeting type should be BUSINESS"),
                () -> assertEquals(2, criteria.getStatuses().size(),
                        "Should have 2 statuses"),
                () -> assertEquals(5, criteria.getMinParticipants(),
                        "Min participants should be 5"),
                () -> assertEquals(50, criteria.getMaxParticipants(),
                        "Max participants should be 50"),
                () -> assertEquals("John Doe", criteria.getOrganizerName(),
                        "Organizer name should match"),
                () -> assertEquals("organizer", criteria.getMyParticipation(),
                        "My participation should match"),
                () -> assertEquals(MeetingVisibility.PUBLIC, criteria.getVisibility(),
                        "Visibility should be PUBLIC"),
                () -> assertEquals(3, criteria.getCategoryIds().size(),
                        "Should have 3 category IDs"),
                () -> assertTrue(criteria.getRecurringOnly(),
                        "Recurring only should be true"),
                () -> assertFalse(criteria.getTemplatesOnly(),
                        "Templates only should be false"),
                () -> assertTrue(criteria.getHasAttachments(),
                        "Has attachments should be true"),
                () -> assertEquals(123L, criteria.getCurrentUserId(),
                        "Current user ID should match"),
                () -> assertTrue(criteria.getUserAuthenticated(),
                        "User should be authenticated"),
                () -> assertTrue(criteria.isIncludePublic(),
                        "Should include public meetings")
        );
    }

    @Test
    void testHelperMethods() {
        SearchCriteria emptyCriteria = SearchCriteria.builder().build();

        SearchCriteria fullCriteria = SearchCriteria.builder()
                .keywords("test")
                .tags("tag1,tag2")
                .dateFrom(LocalDate.now())
                .type(MeetingType.ONLINE)
                .statuses(Arrays.asList("ACTIVE"))
                .minParticipants(10)
                .maxParticipants(20)
                .organizerName("Admin")
                .myParticipation("participant")
                .visibility(MeetingVisibility.PRIVATE)
                .categoryIds(Arrays.asList(1L))
                .recurringOnly(true)
                .hasAttachments(true)
                .build();

        assertAll("Helper methods validation",
                // Empty criteria
                () -> assertFalse(emptyCriteria.hasKeywords(),
                        "Empty criteria should not have keywords"),
                () -> assertFalse(emptyCriteria.hasTags(),
                        "Empty criteria should not have tags"),
                () -> assertFalse(emptyCriteria.hasDateRange(),
                        "Empty criteria should not have date range"),
                () -> assertFalse(emptyCriteria.hasType(),
                        "Empty criteria should not have type"),
                () -> assertFalse(emptyCriteria.hasStatuses(),
                        "Empty criteria should not have statuses"),
                () -> assertFalse(emptyCriteria.hasParticipantsFilter(),
                        "Empty criteria should not have participants filter"),
                () -> assertFalse(emptyCriteria.hasOrganizerFilter(),
                        "Empty criteria should not have organizer filter"),
                () -> assertFalse(emptyCriteria.hasMyParticipationFilter(),
                        "Empty criteria should not have participation filter"),
                () -> assertFalse(emptyCriteria.hasVisibilityFilter(),
                        "Empty criteria should not have visibility filter"),
                () -> assertFalse(emptyCriteria.hasCategoryFilter(),
                        "Empty criteria should not have category filter"),
                () -> assertFalse(emptyCriteria.hasRecurringFilter(),
                        "Empty criteria should not have recurring filter"),
                () -> assertFalse(emptyCriteria.hasTemplatesFilter(),
                        "Empty criteria should not have templates filter"),
                () -> assertFalse(emptyCriteria.hasAttachmentsFilter(),
                        "Empty criteria should not have attachments filter"),
                () -> assertTrue(emptyCriteria.isEmpty(),
                        "Empty criteria should be empty"),
                () -> assertEquals(0, emptyCriteria.getTagsList().size(),
                        "Empty criteria should have empty tags list"),

                // Full criteria
                () -> assertTrue(fullCriteria.hasKeywords(),
                        "Full criteria should have keywords"),
                () -> assertTrue(fullCriteria.hasTags(),
                        "Full criteria should have tags"),
                () -> assertTrue(fullCriteria.hasDateRange(),
                        "Full criteria should have date range"),
                () -> assertTrue(fullCriteria.hasType(),
                        "Full criteria should have type"),
                () -> assertTrue(fullCriteria.hasStatuses(),
                        "Full criteria should have statuses"),
                () -> assertTrue(fullCriteria.hasParticipantsFilter(),
                        "Full criteria should have participants filter"),
                () -> assertTrue(fullCriteria.hasOrganizerFilter(),
                        "Full criteria should have organizer filter"),
                () -> assertTrue(fullCriteria.hasMyParticipationFilter(),
                        "Full criteria should have participation filter"),
                () -> assertTrue(fullCriteria.hasVisibilityFilter(),
                        "Full criteria should have visibility filter"),
                () -> assertTrue(fullCriteria.hasCategoryFilter(),
                        "Full criteria should have category filter"),
                () -> assertTrue(fullCriteria.hasRecurringFilter(),
                        "Full criteria should have recurring filter"),
                () -> assertTrue(fullCriteria.hasAttachmentsFilter(),
                        "Full criteria should have attachments filter"),
                () -> assertFalse(fullCriteria.isEmpty(),
                        "Full criteria should not be empty"),
                () -> assertEquals(2, fullCriteria.getTagsList().size(),
                        "Full criteria should have 2 tags in list"),
                () -> assertTrue(fullCriteria.getTagsList().contains("tag1"),
                        "Tags list should contain 'tag1'"),
                () -> assertTrue(fullCriteria.getTagsList().contains("tag2"),
                        "Tags list should contain 'tag2'")
        );
    }

    @Test
    void testSetTypeMethod() {
        SearchCriteria criteria = new SearchCriteria();

        assertAll("Set type method tests",
                () -> {
                    criteria.setType("ONLINE");
                    assertEquals(MeetingType.ONLINE, criteria.getType(),
                            "Should set BUSINESS type from string");
                },
                () -> {
                    criteria.setType("IN_PERSON");
                    assertEquals(MeetingType.IN_PERSON, criteria.getType(),
                            "Should set INFORMAL type from lowercase string");
                },
                () -> {
                    criteria.setType("PHYSICAL");
                    assertEquals(MeetingType.PHYSICAL, criteria.getType(),
                            "Should trim and set VIRTUAL type");
                },
                () -> {
                    criteria.setType(null);
                    assertNull(criteria.getType(),
                            "Should set null for null input");
                },
                () -> {
                    criteria.setType("");
                    assertNull(criteria.getType(),
                            "Should set null for empty string");
                },
                () -> {
                    criteria.setType("   ");
                    assertNull(criteria.getType(),
                            "Should set null for blank string");
                }
        );
    }

    @Test
    void testDefaultValues() {
        SearchCriteria criteria = new SearchCriteria();

        assertAll("Default values test",
                () -> assertEquals(0, criteria.getMinParticipants(),
                        "Default min participants should be 0"),
                () -> assertEquals(100, criteria.getMaxParticipants(),
                        "Default max participants should be 100"),
                () -> assertNotNull(criteria.getStatuses(),
                        "Statuses should not be null"),
                () -> assertTrue(criteria.getStatuses().isEmpty(),
                        "Default statuses should be empty"),
                () -> assertNotNull(criteria.getCategoryIds(),
                        "Category IDs should not be null"),
                () -> assertTrue(criteria.getCategoryIds().isEmpty(),
                        "Default category IDs should be empty"),
                () -> assertTrue(criteria.isIncludePublic(),
                        "Default includePublic should be true")
        );
    }
}