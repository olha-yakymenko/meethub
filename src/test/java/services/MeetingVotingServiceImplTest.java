package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.model.request.*;
import com.meethub.domain.model.response.*;
import com.meethub.domain.repository.jpa.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingVotingServiceImplTest {

    @Mock
    private MeetingVotingRepository votingRepository;

    @Mock
    private VotingOptionRepository optionRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingParticipantRepository participantRepository;

    @InjectMocks
    private MeetingVotingServiceImpl meetingVotingService;

    private Meeting testMeeting;
    private User testOrganizer;
    private User testParticipant;
    private MeetingVoting testVoting;
    private CreateVotingRequest createRequest;
    private VotingOption testOption;

    @BeforeEach
    void setUp() {
        testOrganizer = new User();
        testOrganizer.setId(1L);
        testOrganizer.setFirstName("Jan");
        testOrganizer.setLastName("Kowalski");
        testOrganizer.setEmail("jan@example.com");

        testParticipant = new User();
        testParticipant.setId(2L);
        testParticipant.setFirstName("Anna");
        testParticipant.setLastName("Nowak");
        testParticipant.setEmail("anna@example.com");

        testMeeting = new Meeting();
        testMeeting.setId(1L);
        testMeeting.setTitle("Test Meeting");
        testMeeting.setOrganizer(testOrganizer);

        testOption = VotingOption.builder()
                .id(1L)
                .optionDate(LocalDateTime.now().plusDays(1))
                .durationMinutes(60)
                .isSuggested(false)
                .build();

        testVoting = MeetingVoting.builder()
                .id(1L)
                .title("Test Voting")
                .description("Test Description")
                .meeting(testMeeting)
                .status(VotingStatus.ACTIVE)
                .type(VotingType.SINGLE_CHOICE)
                .maxChoices(1)
                .allowSuggestions(true)
                .deadlineDate(LocalDateTime.now().plusDays(1))
                .autoClose(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .options(Arrays.asList(testOption))
                .build();

        testOption.setVoting(testVoting);

        createRequest = new CreateVotingRequest();
        createRequest.setTitle("New Voting");
        createRequest.setDescription("New Description");
        createRequest.setType(VotingType.SINGLE_CHOICE);
        createRequest.setMaxChoices(2);
        createRequest.setAllowSuggestions(true);
        createRequest.setDeadlineDate(LocalDateTime.now().plusDays(2));
        createRequest.setAutoClose(false);

        VotingOptionRequest option1 = new VotingOptionRequest();
        option1.setOptionDate(LocalDateTime.now().plusDays(1));
        option1.setDurationMinutes(60);

        VotingOptionRequest option2 = new VotingOptionRequest();
        option2.setOptionDate(LocalDateTime.now().plusDays(2));
        option2.setDurationMinutes(90);

        createRequest.setOptions(Arrays.asList(option1, option2));
    }

    @Test
    void createVoting_Success() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(votingRepository.save(any(MeetingVoting.class))).thenReturn(testVoting);
        when(optionRepository.saveAll(anyList())).thenReturn(Arrays.asList(testOption));

        VotingResponse response = meetingVotingService.createVoting(1L, createRequest, 1L);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(testVoting.getTitle(), response.getTitle()),
                () -> assertEquals(testVoting.getType(), response.getType()),
                () -> assertEquals(testVoting.getStatus(), response.getStatus())
        );

        verify(votingRepository, times(1)).save(any(MeetingVoting.class));
        verify(optionRepository, times(1)).saveAll(anyList());
    }

    @Test
    void createVoting_MeetingNotFound() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                meetingVotingService.createVoting(1L, createRequest, 1L)
        );

        assertAll(
                () -> assertEquals("Nie znaleziono spotkania", exception.getMessage())
        );
    }

    @Test
    void createVoting_NotOrganizer() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                meetingVotingService.createVoting(1L, createRequest, 2L)
        );

        assertAll(
                () -> assertEquals("Brak uprawnień do tworzenia głosowania", exception.getMessage())
        );
    }

    @Test
    void getMeetingVotings_Success() {
        when(votingRepository.findByMeetingId(1L)).thenReturn(Arrays.asList(testVoting));
        when(voteRepository.countByOptionId(anyLong())).thenReturn(0L);
        when(voteRepository.existsByVotingIdAndUserIdAndOptionId(anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(voteRepository.findUserVotes(anyLong(), anyLong())).thenReturn(new ArrayList<>());
        when(voteRepository.countByVotingId(anyLong())).thenReturn(0L);
        when(voteRepository.countDistinctVotersByVotingId(anyLong())).thenReturn(0L);
        when(optionRepository.countByVotingId(anyLong())).thenReturn(1L);
        when(participantRepository.findByMeetingId(anyLong())).thenReturn(new ArrayList<>());

        List<VotingResponse> responses = meetingVotingService.getMeetingVotings(1L, 1L);

        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size()),
                () -> assertEquals(testVoting.getId(), responses.get(0).getId())
        );
    }

    @Test
    void closeExpiredVotingIfNeeded_VotingExpired() {
        MeetingVoting expiredVoting = MeetingVoting.builder()
                .id(2L)
                .status(VotingStatus.ACTIVE)
                .deadlineDate(LocalDateTime.now().minusDays(1))
                .build();

        when(votingRepository.findById(2L)).thenReturn(Optional.of(expiredVoting));
        when(votingRepository.save(any(MeetingVoting.class))).thenReturn(expiredVoting);

        meetingVotingService.closeExpiredVotingIfNeeded(2L);

        verify(votingRepository, times(1)).save(any(MeetingVoting.class));
    }

    @Test
    void closeExpiredVotingIfNeeded_VotingNotExpired() {
        MeetingVoting activeVoting = MeetingVoting.builder()
                .id(3L)
                .status(VotingStatus.ACTIVE)
                .deadlineDate(LocalDateTime.now().plusDays(1))
                .build();

        when(votingRepository.findById(3L)).thenReturn(Optional.of(activeVoting));

        meetingVotingService.closeExpiredVotingIfNeeded(3L);

        verify(votingRepository, never()).save(any(MeetingVoting.class));
    }

    @Test
    void closeExpiredVotingIfNeeded_VotingAlreadyClosed() {
        MeetingVoting closedVoting = MeetingVoting.builder()
                .id(4L)
                .status(VotingStatus.CLOSED)
                .deadlineDate(LocalDateTime.now().minusDays(1))
                .build();

        when(votingRepository.findById(4L)).thenReturn(Optional.of(closedVoting));

        meetingVotingService.closeExpiredVotingIfNeeded(4L);

        verify(votingRepository, never()).save(any(MeetingVoting.class));
    }

    @Test
    void getVotingDetails_Success() {
        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(voteRepository.countByOptionId(anyLong())).thenReturn(0L);
        when(voteRepository.existsByVotingIdAndUserIdAndOptionId(anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(voteRepository.findUserVotes(anyLong(), anyLong())).thenReturn(new ArrayList<>());
        when(voteRepository.countByVotingId(anyLong())).thenReturn(0L);
        when(voteRepository.countDistinctVotersByVotingId(anyLong())).thenReturn(0L);
        when(optionRepository.countByVotingId(anyLong())).thenReturn(1L);
        when(participantRepository.findByMeetingId(anyLong())).thenReturn(new ArrayList<>());

        VotingResponse response = meetingVotingService.getVotingDetails(1L, 1L);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(testVoting.getId(), response.getId())
        );
    }

    @Test
    void getVotingDetails_VotingNotFound() {
        when(votingRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                meetingVotingService.getVotingDetails(1L, 1L)
        );

        assertAll(
                () -> assertEquals("Głosowanie nie zostało znalezione", exception.getMessage())
        );
    }

    @Test
    void submitVote_Success() {
        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setOptionIds(Arrays.asList(1L));
        voteRequest.setPreferenceOrder(Arrays.asList(1));

        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(participantRepository.existsByMeetingIdAndUserId(1L, 2L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(testParticipant));
        when(optionRepository.findById(1L)).thenReturn(Optional.of(testOption));
        when(optionRepository.findByVotingId(1L)).thenReturn(Arrays.asList(testOption));

        VoteResponse response = meetingVotingService.submitVote(1L, voteRequest, 2L);

        assertAll(
                () -> assertNotNull(response),
                () -> assertTrue(response.getSuccess()),
                () -> assertEquals("Głos został oddany pomyślnie", response.getMessage())
        );

        verify(voteRepository, times(1)).deleteByVotingIdAndUserId(1L, 2L);
        verify(voteRepository, times(1)).saveAll(anyList());
    }

    @Test
    void submitVote_VotingClosed() {
        MeetingVoting closedVoting = MeetingVoting.builder()
                .id(1L)
                .status(VotingStatus.CLOSED)
                .meeting(testMeeting)
                .build();

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setOptionIds(Arrays.asList(1L));

        when(votingRepository.findById(1L)).thenReturn(Optional.of(closedVoting));
        when(participantRepository.existsByMeetingIdAndUserId(1L, 2L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                meetingVotingService.submitVote(1L, voteRequest, 2L)
        );

        assertAll(
                () -> assertEquals("Głosowanie jest zamknięte", exception.getMessage())
        );
    }

    @Test
    void submitVote_DeadlinePassed() {
        MeetingVoting expiredVoting = MeetingVoting.builder()
                .id(1L)
                .status(VotingStatus.ACTIVE)
                .deadlineDate(LocalDateTime.now().minusDays(1))
                .meeting(testMeeting)
                .build();

        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setOptionIds(Arrays.asList(1L));

        when(votingRepository.findById(1L)).thenReturn(Optional.of(expiredVoting));
        when(participantRepository.existsByMeetingIdAndUserId(1L, 2L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                meetingVotingService.submitVote(1L, voteRequest, 2L)
        );

        assertAll(
                () -> assertEquals("Czas na głosowanie upłynął", exception.getMessage())
        );
    }

    @Test
    void submitVote_NoPermission() {
        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(participantRepository.existsByMeetingIdAndUserId(1L, 2L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                meetingVotingService.submitVote(1L, new VoteRequest(), 2L)
        );

        assertAll(
                () -> assertEquals("Nie masz uprawnień do udziału w tym głosowaniu", exception.getMessage())
        );
    }

    @Test
    void submitVote_OrganizerHasDoubleWeight() {
        VoteRequest voteRequest = new VoteRequest();
        voteRequest.setOptionIds(Arrays.asList(1L));

        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(participantRepository.existsByMeetingIdAndUserId(1L, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testOrganizer));
        when(optionRepository.findById(1L)).thenReturn(Optional.of(testOption));
        when(optionRepository.findByVotingId(1L)).thenReturn(Arrays.asList(testOption));

        meetingVotingService.submitVote(1L, voteRequest, 1L);

        verify(voteRepository, times(1)).saveAll(anyList());
    }

    @Test
    void suggestOption_Success() {
        VotingOptionRequest optionRequest = new VotingOptionRequest();
        optionRequest.setOptionDate(LocalDateTime.now().plusDays(3));
        optionRequest.setDurationMinutes(120);

        VotingOption suggestedOption = VotingOption.builder()
                .id(2L)
                .voting(testVoting)
                .optionDate(optionRequest.getOptionDate())
                .durationMinutes(optionRequest.getDurationMinutes())
                .isSuggested(true)
                .suggestedBy(2L)
                .build();

        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(participantRepository.existsByMeetingIdAndUserId(1L, 2L)).thenReturn(true);
        when(optionRepository.save(any(VotingOption.class))).thenReturn(suggestedOption);

        VotingOptionResponse response = meetingVotingService.suggestOption(1L, optionRequest, 2L);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(suggestedOption.getId(), response.getId()),
                () -> assertTrue(response.getIsSuggested())
        );

        verify(optionRepository, times(1)).save(any(VotingOption.class));
    }

    @Test
    void suggestOption_SuggestionsDisabled() {
        MeetingVoting votingNoSuggestions = MeetingVoting.builder()
                .id(1L)
                .allowSuggestions(false)
                .meeting(testMeeting)
                .build();

        VotingOptionRequest optionRequest = new VotingOptionRequest();

        when(votingRepository.findById(1L)).thenReturn(Optional.of(votingNoSuggestions));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                meetingVotingService.suggestOption(1L, optionRequest, 2L)
        );

        assertAll(
                () -> assertEquals("Sugerowanie opcji jest wyłączone", exception.getMessage())
        );
    }

    @Test
    void closeVoting_Success() {
        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(votingRepository.save(any(MeetingVoting.class))).thenReturn(testVoting);
        when(voteRepository.countByOptionId(anyLong())).thenReturn(0L);
        when(voteRepository.existsByVotingIdAndUserIdAndOptionId(anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(voteRepository.findUserVotes(anyLong(), anyLong())).thenReturn(new ArrayList<>());
        when(voteRepository.countByVotingId(anyLong())).thenReturn(0L);
        when(voteRepository.countDistinctVotersByVotingId(anyLong())).thenReturn(0L);
        when(optionRepository.countByVotingId(anyLong())).thenReturn(1L);
        when(participantRepository.findByMeetingId(anyLong())).thenReturn(new ArrayList<>());

        VotingResponse response = meetingVotingService.closeVoting(1L, 1L);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(VotingStatus.CLOSED, response.getStatus())
        );

        verify(votingRepository, times(1)).save(any(MeetingVoting.class));
    }

    @Test
    void closeVoting_NotOrganizer() {
        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                meetingVotingService.closeVoting(1L, 2L)
        );

        assertAll(
                () -> assertEquals("Tylko organizator może zamknąć głosowanie", exception.getMessage())
        );
    }

    @Test
    void findOptimalTime_SingleChoice() {
        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(optionRepository.findByVotingId(1L)).thenReturn(Arrays.asList(testOption));

        List<Object[]> voteCounts = new ArrayList<>();
        voteCounts.add(new Object[]{1L, 5L});

        when(voteRepository.countVotesByOption(1L)).thenReturn(voteCounts);

        WinningOptionResponse response = meetingVotingService.findOptimalTime(1L);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(testOption.getId(), response.getOptionId())
        );
    }

    @Test
    void hasActiveVoting_ReturnsTrue() {
        when(votingRepository.hasActiveVoting(1L)).thenReturn(true);

        boolean result = meetingVotingService.hasActiveVoting(1L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void hasActiveVoting_ReturnsFalse() {
        when(votingRepository.hasActiveVoting(1L)).thenReturn(false);

        boolean result = meetingVotingService.hasActiveVoting(1L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void getExpiredVotings_Success() {
        doReturn(Arrays.asList(testVoting)).when(votingRepository).findExpiredVotings(any(LocalDateTime.class));

        when(voteRepository.countByOptionId(anyLong())).thenReturn(0L);
        when(voteRepository.existsByVotingIdAndUserIdAndOptionId(anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(voteRepository.findUserVotes(anyLong(), anyLong())).thenReturn(new ArrayList<>());
        when(voteRepository.countByVotingId(anyLong())).thenReturn(0L);
        when(voteRepository.countDistinctVotersByVotingId(anyLong())).thenReturn(0L);
        when(optionRepository.countByVotingId(anyLong())).thenReturn(1L);
        when(participantRepository.findByMeetingId(anyLong())).thenReturn(new ArrayList<>());

        List<VotingResponse> responses = meetingVotingService.getExpiredVotings();

        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size())
        );
    }

    @Test
    void submitVote_ValidatesOrganizerPermission() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L));

        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(optionRepository.findById(1L)).thenReturn(Optional.of(testOption));
        when(optionRepository.findByVotingId(1L)).thenReturn(Arrays.asList(testOption));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testOrganizer));

        assertDoesNotThrow(() ->
                meetingVotingService.submitVote(1L, request, 1L)
        );
    }

    @Test
    void submitVote_ValidatesParticipantPermission() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L));

        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(participantRepository.existsByMeetingIdAndUserId(1L, 2L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(testParticipant));
        when(optionRepository.findById(1L)).thenReturn(Optional.of(testOption));
        when(optionRepository.findByVotingId(1L)).thenReturn(Arrays.asList(testOption));

        assertDoesNotThrow(() ->
                meetingVotingService.submitVote(1L, request, 2L)
        );
    }

    @Test
    void submitVote_ValidatesUnauthorizedUser() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L));

        when(votingRepository.findById(1L)).thenReturn(Optional.of(testVoting));
        when(participantRepository.existsByMeetingIdAndUserId(1L, 3L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                meetingVotingService.submitVote(1L, request, 3L)
        );

        assertAll(
                () -> assertEquals("Nie masz uprawnień do udziału w tym głosowaniu", exception.getMessage())
        );
    }

    @Test
    void getExpiredVotings_EmptyList() {
        doReturn(new ArrayList<MeetingVoting>()).when(votingRepository).findExpiredVotings(any(LocalDateTime.class));

        List<VotingResponse> responses = meetingVotingService.getExpiredVotings();

        assertAll(
                () -> assertNotNull(responses),
                () -> assertTrue(responses.isEmpty())
        );
    }
}