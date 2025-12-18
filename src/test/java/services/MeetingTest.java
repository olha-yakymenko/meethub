package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Meeting Entity Tests")
class MeetingTest {

    private User organizer;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        organizer = new User();
        organizer.setId(1L);
        organizer.setEmail("organizer@example.com");

        startDate = LocalDateTime.now().plusDays(1);
        endDate = startDate.plusHours(2);
    }

    @Test
    @DisplayName("Meeting powinien mieć domyślny status PLANNED")
    void meeting_ShouldHaveDefaultStatusPlanned() {
        // When
        Meeting meeting = new Meeting();

        // Then
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.PLANNED);
    }

    @Test
    @DisplayName("Konstruktor z parametrami powinien poprawnie inicjalizować pola")
    void constructorWithParameters_ShouldInitializeFieldsCorrectly() {
        // Given
        String title = "Test Meeting";
        MeetingType type = MeetingType.PHYSICAL;
        MeetingVisibility visibility = MeetingVisibility.PUBLIC;

        // When
        Meeting meeting = new Meeting(title, type, visibility, startDate, endDate, organizer);

        // Then
        assertAll(
                () -> assertThat(meeting.getTitle()).isEqualTo(title),
                () -> assertThat(meeting.getType()).isEqualTo(type),
                () -> assertThat(meeting.getVisibility()).isEqualTo(visibility),
                () -> assertThat(meeting.getStartDate()).isEqualTo(startDate),
                () -> assertThat(meeting.getEndDate()).isEqualTo(endDate),
                () -> assertThat(meeting.getOrganizer()).isEqualTo(organizer),
                () -> assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.PLANNED)
        );
    }

    @Nested
    @DisplayName("Business Logic Methods Tests")
    class BusinessLogicTests {

        private Meeting meeting;

        @BeforeEach
        void setUp() {
            meeting = new Meeting();
            meeting.setStartDate(startDate);
            meeting.setEndDate(endDate);
        }

        @Test
        @DisplayName("isUpcoming powinno zwracać true dla przyszłej daty")
        void isUpcoming_ShouldReturnTrueForFutureDate() {
            // Given
            meeting.setStartDate(LocalDateTime.now().plusHours(1));

            // When & Then
            assertThat(meeting.isUpcoming()).isTrue();
        }

        @Test
        @DisplayName("isUpcoming powinno zwracać false dla przeszłej daty")
        void isUpcoming_ShouldReturnFalseForPastDate() {
            // Given
            meeting.setStartDate(LocalDateTime.now().minusHours(1));

            // When & Then
            assertThat(meeting.isUpcoming()).isFalse();
        }

        @Test
        @DisplayName("isUpcoming powinno zwracać false dla null daty")
        void isUpcoming_ShouldReturnFalseForNullDate() {
            // Given
            meeting.setStartDate(null);

            // When & Then
            assertThat(meeting.isUpcoming()).isFalse();
        }

        @Test
        @DisplayName("isOngoing powinno zwracać true dla trwającego spotkania")
        void isOngoing_ShouldReturnTrueForCurrentMeeting() {
            // Given
            meeting.setStartDate(LocalDateTime.now().minusHours(1));
            meeting.setEndDate(LocalDateTime.now().plusHours(1));

            // When & Then
            assertThat(meeting.isOngoing()).isTrue();
        }

        @Test
        @DisplayName("isOngoing powinno zwracać false dla przyszłego spotkania")
        void isOngoing_ShouldReturnFalseForFutureMeeting() {
            // Given
            meeting.setStartDate(LocalDateTime.now().plusHours(1));
            meeting.setEndDate(LocalDateTime.now().plusHours(2));

            // When & Then
            assertThat(meeting.isOngoing()).isFalse();
        }

        @Test
        @DisplayName("isOngoing powinno zwracać false dla null dat")
        void isOngoing_ShouldReturnFalseForNullDates() {
            // Given
            meeting.setStartDate(null);
            meeting.setEndDate(null);

            // When & Then
            assertThat(meeting.isOngoing()).isFalse();
        }

        @Test
        @DisplayName("isPast powinno zwracać true dla zakończonego spotkania")
        void isPast_ShouldReturnTrueForPastMeeting() {
            // Given
            meeting.setEndDate(LocalDateTime.now().minusMinutes(1));

            // When & Then
            assertThat(meeting.isPast()).isTrue();
        }

        @Test
        @DisplayName("isPast powinno zwracać false dla trwającego spotkania")
        void isPast_ShouldReturnFalseForCurrentMeeting() {
            // Given
            meeting.setEndDate(LocalDateTime.now().plusHours(1));

            // When & Then
            assertThat(meeting.isPast()).isFalse();
        }

        @Test
        @DisplayName("isPast powinno zwracać false dla null daty")
        void isPast_ShouldReturnFalseForNullDate() {
            // Given
            meeting.setEndDate(null);

            // When & Then
            assertThat(meeting.isPast()).isFalse();
        }
    }

    @Nested
    @DisplayName("Participants Management Tests")
    class ParticipantsManagementTests {

        private Meeting meeting;
        private MeetingParticipant participant;

        @BeforeEach
        void setUp() {
            meeting = new Meeting();
            participant = new MeetingParticipant();
            participant.setId(1L);

            User user = new User();
            user.setId(2L);
            participant.setUser(user);
        }

        @Test
        @DisplayName("addParticipant powinno dodać uczestnika i ustawić meeting")
        void addParticipant_ShouldAddParticipantAndSetMeeting() {
            // When
            meeting.addParticipant(participant);

            // Then
            assertAll(
                    () -> assertThat(meeting.getParticipants()).contains(participant),
                    () -> assertThat(participant.getMeeting()).isEqualTo(meeting)
            );
        }

        @Test
        @DisplayName("addParticipant powinno zainicjalizować pusty set jeśli null")
        void addParticipant_ShouldInitializeSetIfNull() {
            // Given
            meeting.setParticipants(null);

            // When
            meeting.addParticipant(participant);

            // Then
            assertThat(meeting.getParticipants()).isNotNull().contains(participant);
        }

        //!nie dziala remove
        @Test
        @DisplayName("removeParticipant powinno usunąć uczestnika i wyczyścić meeting")
        void removeParticipant_ShouldRemoveParticipantAndClearMeeting() {
            // Given
            System.out.println("Before add - meeting hash: " + System.identityHashCode(meeting));
            System.out.println("Before add - participant meeting: " + participant.getMeeting());

            meeting.addParticipant(participant);

            System.out.println("After add - participants size: " + meeting.getParticipants().size());
            System.out.println("After add - participant meeting: " + participant.getMeeting());
            System.out.println("After add - participant meeting hash: " +
                    System.identityHashCode(participant.getMeeting()));

            // When
            meeting.removeParticipant(participant);

            System.out.println("After remove - participants size: " + meeting.getParticipants().size());
            System.out.println("After remove - participant meeting: " + participant.getMeeting());

            // Then
            assertAll(
                    () -> {
                        System.out.println("Checking if participants contains participant...");
                        assertThat(meeting.getParticipants()).doesNotContain(participant);
                    },
                    () -> {
                        System.out.println("Checking if participant meeting is null...");
                        assertThat(participant.getMeeting()).isNull();
                    }
            );
        }

        @Test
        @DisplayName("removeParticipant powinno nic nie robić dla null participants")
        void removeParticipant_ShouldDoNothingForNullParticipants() {
            // Given
            meeting.setParticipants(null);

            // When
            meeting.removeParticipant(participant);

            // Then
            assertThat(meeting.getParticipants()).isNull();
        }
    }

    @Nested
    @DisplayName("Available Spots Tests")
    class AvailableSpotsTests {

        private Meeting meeting;
        private MeetingParticipant confirmedParticipant;
        private MeetingParticipant pendingParticipant;

        @BeforeEach
        void setUp() {
            meeting = new Meeting();

            confirmedParticipant = new MeetingParticipant();
            confirmedParticipant.setStatus(com.meethub.domain.model.enums.ParticipationStatus.CONFIRMED);

            pendingParticipant = new MeetingParticipant();
            pendingParticipant.setStatus(com.meethub.domain.model.enums.ParticipationStatus.PENDING);
        }

        @Test
        @DisplayName("hasAvailableSpots powinno zwracać true gdy maxParticipants jest null")
        void hasAvailableSpots_ShouldReturnTrueWhenMaxParticipantsIsNull() {
            // Given
            meeting.setMaxParticipants(null);
            meeting.setParticipants(new HashSet<>(Set.of(confirmedParticipant)));

            // When & Then
            assertThat(meeting.hasAvailableSpots()).isTrue();
        }

        @Test
        @DisplayName("hasAvailableSpots powinno zwracać true gdy participants jest null")
        void hasAvailableSpots_ShouldReturnTrueWhenParticipantsIsNull() {
            // Given
            meeting.setMaxParticipants(10);
            meeting.setParticipants(null);

            // When & Then
            assertThat(meeting.hasAvailableSpots()).isTrue();
        }

        @Test
        @DisplayName("hasAvailableSpots powinno zwracać true gdy są wolne miejsca")
        void hasAvailableSpots_ShouldReturnTrueWhenSpotsAvailable() {
            // Given
            meeting.setMaxParticipants(5);
            Set<MeetingParticipant> participants = new HashSet<>();
            participants.add(confirmedParticipant);
            meeting.setParticipants(participants);

            // When & Then
            assertThat(meeting.hasAvailableSpots()).isTrue();
        }


        @Test
        @DisplayName("hasAvailableSpots powinno ignorować niepotwierdzonych uczestników")
        void hasAvailableSpots_ShouldIgnoreNonConfirmedParticipants() {
            // Given
            meeting.setMaxParticipants(1);
            Set<MeetingParticipant> participants = new HashSet<>();
            participants.add(pendingParticipant); // Tylko PENDING
            meeting.setParticipants(participants);

            // When & Then
            assertThat(meeting.hasAvailableSpots()).isTrue();
        }
    }

    @Nested
    @DisplayName("Confirmed Participants Count Tests")
    class ConfirmedParticipantsCountTests {

        private Meeting meeting;

        @BeforeEach
        void setUp() {
            meeting = new Meeting();
        }

        @Test
        @DisplayName("getConfirmedParticipantsCount powinno zwracać 0 dla null participants")
        void getConfirmedParticipantsCount_ShouldReturnZeroForNullParticipants() {
            // Given
            meeting.setParticipants(null);

            // When & Then
            assertThat(meeting.getConfirmedParticipantsCount()).isZero();
        }

        @Test
        @DisplayName("getConfirmedParticipantsCount powinno zliczać tylko CONFIRMED")
        void getConfirmedParticipantsCount_ShouldCountOnlyConfirmed() {
            // Given
            MeetingParticipant confirmed = new MeetingParticipant();
            confirmed.setStatus(com.meethub.domain.model.enums.ParticipationStatus.CONFIRMED);

            MeetingParticipant pending = new MeetingParticipant();
            pending.setStatus(com.meethub.domain.model.enums.ParticipationStatus.PENDING);

            Set<MeetingParticipant> participants = new HashSet<>();
            participants.add(confirmed);
            participants.add(pending);
            meeting.setParticipants(participants);

            // When & Then
            assertThat(meeting.getConfirmedParticipantsCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("getConfirmedParticipantsCount powinno ignorować null uczestników")
        void getConfirmedParticipantsCount_ShouldIgnoreNullParticipants() {
            // Given
            MeetingParticipant confirmed = new MeetingParticipant();
            confirmed.setStatus(com.meethub.domain.model.enums.ParticipationStatus.CONFIRMED);

            Set<MeetingParticipant> participants = new HashSet<>();
            participants.add(confirmed);
            participants.add(null); // null participant
            meeting.setParticipants(participants);

            // When & Then
            assertThat(meeting.getConfirmedParticipantsCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Tags Management Tests")
    class TagsManagementTests {

        private Meeting meeting;

        @BeforeEach
        void setUp() {
            meeting = new Meeting();
        }

        @Test
        @DisplayName("addTag powinno dodać tag")
        void addTag_ShouldAddTag() {
            // When
            meeting.addTag("important");

            // Then
            assertThat(meeting.getTags()).contains("important");
        }

        @Test
        @DisplayName("addTag powinno zainicjalizować pusty set jeśli null")
        void addTag_ShouldInitializeSetIfNull() {
            // Given
            meeting.setTags(null);

            // When
            meeting.addTag("urgent");

            // Then
            assertThat(meeting.getTags()).isNotNull().contains("urgent");
        }

        @Test
        @DisplayName("removeTag powinno usunąć tag")
        void removeTag_ShouldRemoveTag() {
            // Given
            meeting.addTag("meeting");
            meeting.addTag("business");

            // When
            meeting.removeTag("meeting");

            // Then
            assertAll(
                    () -> assertThat(meeting.getTags()).doesNotContain("meeting"),
                    () -> assertThat(meeting.getTags()).contains("business")
            );
        }

        @Test
        @DisplayName("removeTag powinno nic nie robić dla null tags")
        void removeTag_ShouldDoNothingForNullTags() {
            // Given
            meeting.setTags(null);

            // When
            meeting.removeTag("test");

            // Then
            assertThat(meeting.getTags()).isNull();
        }
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Builder powinno tworzyć poprawny obiekt Meeting")
        void builder_ShouldCreateValidMeeting() {
            // Given
            String title = "Builder Test Meeting";
            MeetingType type = MeetingType.PHYSICAL;
            MeetingVisibility visibility = MeetingVisibility.PRIVATE;
            Integer maxParticipants = 10;

            Location location = new Location();
            location.setId(1L);

            // When
            Meeting meeting = Meeting.builder()
                    .title(title)
                    .description("Test Description")
                    .agenda("Test Agenda")
                    .type(type)
                    .visibility(visibility)
                    .startDate(startDate)
                    .endDate(endDate)
                    .maxParticipants(maxParticipants)
                    .organizer(organizer)
                    .location(location)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(meeting.getTitle()).isEqualTo(title),
                    () -> assertThat(meeting.getDescription()).isEqualTo("Test Description"),
                    () -> assertThat(meeting.getAgenda()).isEqualTo("Test Agenda"),
                    () -> assertThat(meeting.getType()).isEqualTo(type),
                    () -> assertThat(meeting.getVisibility()).isEqualTo(visibility),
                    () -> assertThat(meeting.getStartDate()).isEqualTo(startDate),
                    () -> assertThat(meeting.getEndDate()).isEqualTo(endDate),
                    () -> assertThat(meeting.getMaxParticipants()).isEqualTo(maxParticipants),
                    () -> assertThat(meeting.getOrganizer()).isEqualTo(organizer),
                    () -> assertThat(meeting.getLocation()).isEqualTo(location)
            );
        }

        @Test
        @DisplayName("Builder powinno obsługiwać null wartości")
        void builder_ShouldHandleNullValues() {
            // When
            Meeting meeting = Meeting.builder()
                    .title("Test")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(meeting.getTitle()).isEqualTo("Test"),
                    () -> assertThat(meeting.getDescription()).isNull(),
                    () -> assertThat(meeting.getAgenda()).isNull(),
                    () -> assertThat(meeting.getType()).isNull(),
                    () -> assertThat(meeting.getVisibility()).isNull(),
                    () -> assertThat(meeting.getStartDate()).isNull(),
                    () -> assertThat(meeting.getEndDate()).isNull(),
                    () -> assertThat(meeting.getMaxParticipants()).isNull(),
                    () -> assertThat(meeting.getOrganizer()).isNull(),
                    () -> assertThat(meeting.getLocation()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("equals powinno zwracać true dla tego samego obiektu")
        void equals_ShouldReturnTrueForSameObject() {
            // Given
            Meeting meeting = Meeting.builder()
                    .title("Test")
                    .startDate(startDate)
                    .organizer(organizer)
                    .build();
            meeting.setId(1L);

            // When & Then
            assertThat(meeting.equals(meeting)).isTrue();
        }

        @Test
        @DisplayName("equals powinno zwracać false dla null")
        void equals_ShouldReturnFalseForNull() {
            // Given
            Meeting meeting = new Meeting();

            // When & Then
            assertThat(meeting.equals(null)).isFalse();
        }

        @Test
        @DisplayName("equals powinno zwracać false dla innej klasy")
        void equals_ShouldReturnFalseForDifferentClass() {
            // Given
            Meeting meeting = new Meeting();

            // When & Then
            assertThat(meeting.equals(new Object())).isFalse();
        }

        @Test
        @DisplayName("equals powinno porównywać po id, title, startDate i organizer")
        void equals_ShouldCompareByIdTitleStartDateAndOrganizer() {
            // Given
            Meeting meeting1 = Meeting.builder()
                    .title("Meeting 1")
                    .startDate(startDate)
                    .organizer(organizer)
                    .build();
            meeting1.setId(1L);

            Meeting meeting2 = Meeting.builder()
                    .title("Meeting 1")
                    .startDate(startDate)
                    .organizer(organizer)
                    .build();
            meeting2.setId(1L);

            Meeting meeting3 = Meeting.builder()
                    .title("Meeting 2") // Różny tytuł
                    .startDate(startDate)
                    .organizer(organizer)
                    .build();
            meeting3.setId(1L);

            // Then
            assertAll(
                    () -> assertThat(meeting1.equals(meeting2)).isTrue(),
                    () -> assertThat(meeting1.equals(meeting3)).isFalse()
            );
        }

        @Test
        @DisplayName("hashCode powinno być zgodne z equals")
        void hashCode_ShouldBeConsistentWithEquals() {
            // Given
            Meeting meeting1 = Meeting.builder()
                    .title("Test")
                    .startDate(startDate)
                    .organizer(organizer)
                    .build();
            meeting1.setId(1L);

            Meeting meeting2 = Meeting.builder()
                    .title("Test")
                    .startDate(startDate)
                    .organizer(organizer)
                    .build();
            meeting2.setId(1L);

            // Then
            assertAll(
                    () -> assertThat(meeting1).hasSameHashCodeAs(meeting2),
                    () -> assertThat(meeting1.equals(meeting2)).isTrue()
            );
        }
    }

    @ParameterizedTest
    @CsvSource({
            "true, false, false",
            "false, true, false",
            "false, false, true"
    })
    @DisplayName("Statusy spotkania powinny być wzajemnie wykluczające")
    void meetingStatuses_ShouldBeMutuallyExclusive(boolean upcoming, boolean ongoing, boolean past) {
        // Given
        Meeting meeting = new Meeting();

        if (upcoming) {
            meeting.setStartDate(LocalDateTime.now().plusHours(1));
            meeting.setEndDate(LocalDateTime.now().plusHours(2));
        } else if (ongoing) {
            meeting.setStartDate(LocalDateTime.now().minusHours(1));
            meeting.setEndDate(LocalDateTime.now().plusHours(1));
        } else if (past) {
            meeting.setStartDate(LocalDateTime.now().minusHours(2));
            meeting.setEndDate(LocalDateTime.now().minusHours(1));
        }

        // When & Then
        assertAll(
                () -> assertThat(meeting.isUpcoming()).isEqualTo(upcoming),
                () -> assertThat(meeting.isOngoing()).isEqualTo(ongoing),
                () -> assertThat(meeting.isPast()).isEqualTo(past)
        );
    }
}