
// MeetingVotingServiceImpl.java
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.model.request.*;
import com.meethub.domain.model.response.*;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.MeetingVotingService;
import com.meethub.exception.VotingAccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingVotingServiceImpl implements MeetingVotingService {

    private final MeetingVotingRepository votingRepository;
    private final VotingOptionRepository optionRepository;
    private final VoteRepository voteRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingParticipantRepository participantRepository;

    @Override
    @Transactional
    public VotingResponse createVoting(Long meetingId, CreateVotingRequest request, Long organizerId) {
        log.info("Creating voting for meeting: {}, organizer: {}", meetingId, organizerId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono spotkania"));

        // Sprawdź czy użytkownik jest organizatorem
        if (!meeting.getOrganizer().getId().equals(organizerId)) {
            throw new RuntimeException("Brak uprawnień do tworzenia głosowania");
        }

        MeetingVoting voting = MeetingVoting.builder()
                .meeting(meeting)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .status(VotingStatus.ACTIVE)
                .maxChoices(request.getMaxChoices())
                .allowSuggestions(request.getAllowSuggestions())
                .deadlineDate(request.getDeadlineDate())
                .autoClose(request.getAutoClose())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        voting = votingRepository.save(voting);

        // Tworzenie opcji głosowania
        MeetingVoting finalVoting = voting;
        List<VotingOption> options = request.getOptions().stream()
                .map(optRequest -> VotingOption.builder()
                        .voting(finalVoting)
                        .optionDate(optRequest.getOptionDate())
                        .durationMinutes(optRequest.getDurationMinutes())
                        .isSuggested(false)
                        .build())
                .collect(Collectors.toList());

        optionRepository.saveAll(options);
        voting.setOptions(options);

        log.info("Voting created successfully: {}", voting.getId());
        return mapToVotingResponse(voting, organizerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VotingResponse> getMeetingVotings(Long meetingId, Long userId) {
        log.debug("Getting votings for meeting: {}, user: {}", meetingId, userId);

        List<MeetingVoting> votings = votingRepository.findByMeetingId(meetingId);
        return votings.stream()
                .map(voting -> mapToVotingResponse(voting, userId))
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void closeExpiredVotingIfNeeded(Long votingId) {
        MeetingVoting voting = votingRepository.findById(votingId)
                .orElseThrow(() -> new RuntimeException("Głosowanie nie zostało znalezione"));

        if (voting.getStatus() == VotingStatus.ACTIVE &&
                voting.getDeadlineDate() != null &&
                LocalDateTime.now().isAfter(voting.getDeadlineDate())) {
            System.out.println("KONCZE SPOTKANIE");
            voting.setStatus(VotingStatus.CLOSED);
            voting.setUpdatedAt(LocalDateTime.now());
            votingRepository.save(voting);
        }

        System.out.println("JUZ BYLEM");
    }

    @Override
    @Transactional(readOnly = true)
    public VotingResponse getVotingDetails(Long votingId, Long userId) {
        log.debug("Getting voting details: {}, user: {}", votingId, userId);

        MeetingVoting voting = votingRepository.findById(votingId)
                .orElseThrow(() -> new RuntimeException("Głosowanie nie zostało znalezione"));

        return mapToVotingResponse(voting, userId);
    }

    @Override
    @Transactional
    public VoteResponse submitVote(Long votingId, VoteRequest request, Long userId) {
        log.info("Submitting vote for voting: {}, user: {}", votingId, userId);

        MeetingVoting voting = votingRepository.findById(votingId)
                .orElseThrow(() -> new RuntimeException("Głosowanie nie zostało znalezione"));

        // Sprawdź czy użytkownik może głosować
        validateVotingPermission(voting, userId);

        // Sprawdź czy głosowanie jest aktywne
        if (voting.getStatus() != VotingStatus.ACTIVE) {
            throw new RuntimeException("Głosowanie jest zamknięte");
        }

        // Sprawdź deadline
        if (voting.getDeadlineDate() != null && LocalDateTime.now().isAfter(voting.getDeadlineDate())) {
            throw new RuntimeException("Czas na głosowanie upłynął");
        }

        // Walidacja wyborów
        validateVoteRequest(voting, request);

        // Usuń poprzednie głosy użytkownika
        voteRepository.deleteByVotingIdAndUserId(votingId, userId);

        // Zapisz nowe głosy
        List<Vote> votes = new ArrayList<>();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony"));

        for (int i = 0; i < request.getOptionIds().size(); i++) {
            Long optionId = request.getOptionIds().get(i);
            VotingOption option = optionRepository.findById(optionId)
                    .orElseThrow(() -> new RuntimeException("Opcja nie została znaleziona"));

            Vote vote = Vote.builder()
                    .voting(voting)
                    .option(option)
                    .user(user)
                    .preferenceOrder(request.getPreferenceOrder() != null && i < request.getPreferenceOrder().size() ?
                            request.getPreferenceOrder().get(i) : null)
                    .voteWeight(calculateVoteWeight(userId, voting.getMeeting().getId()))
                    .votedAt(LocalDateTime.now())
                    .build();

            votes.add(vote);
        }

        voteRepository.saveAll(votes);

        log.info("Vote submitted successfully for user: {}", userId);
        return VoteResponse.builder()
                .success(true)
                .message("Głos został oddany pomyślnie")
                .votedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public VotingOptionResponse suggestOption(Long votingId, VotingOptionRequest request, Long userId) {
        log.info("Suggesting option for voting: {}, user: {}", votingId, userId);

        MeetingVoting voting = votingRepository.findById(votingId)
                .orElseThrow(() -> new RuntimeException("Głosowanie nie zostało znalezione"));

        if (!Boolean.TRUE.equals(voting.getAllowSuggestions())) {
            throw new RuntimeException("Sugerowanie opcji jest wyłączone");
        }

        validateVotingPermission(voting, userId);

        VotingOption option = VotingOption.builder()
                .voting(voting)
                .optionDate(request.getOptionDate())
                .durationMinutes(request.getDurationMinutes())
                .isSuggested(true)
                .suggestedBy(userId)
                .build();

        option = optionRepository.save(option);

        log.info("Option suggested successfully: {}", option.getId());
        return mapToOptionResponse(option, userId);
    }

    @Override
    @Transactional
    public VotingResponse closeVoting(Long votingId, Long organizerId) {
        log.info("Closing voting: {}, organizer: {}", votingId, organizerId);

        MeetingVoting voting = votingRepository.findById(votingId)
                .orElseThrow(() -> new RuntimeException("Głosowanie nie zostało znalezione"));

        if (!voting.getMeeting().getOrganizer().getId().equals(organizerId)) {
            throw new RuntimeException("Tylko organizator może zamknąć głosowanie");
        }

        voting.setStatus(VotingStatus.CLOSED);
        voting.setUpdatedAt(LocalDateTime.now());
        voting = votingRepository.save(voting);

        // Automatycznie wybierz zwycięski termin
        autoSelectWinningOption(voting);

        log.info("Voting closed successfully: {}", votingId);
        return mapToVotingResponse(voting, organizerId);
    }

    @Override
    public WinningOptionResponse findOptimalTime(Long votingId) {
        log.debug("Finding optimal time for voting: {}", votingId);

        MeetingVoting voting = votingRepository.findById(votingId)
                .orElseThrow(() -> new RuntimeException("Głosowanie nie zostało znalezione"));

        List<VotingOption> options = optionRepository.findByVotingId(votingId);
        Map<Long, Integer> voteCounts = getVoteCountsByOption(votingId);

        switch (voting.getType()) {
            case SINGLE_CHOICE:
                return findWinnerSimpleMajority(options, voteCounts);
            case RANKED:
                return findWinnerRankedChoice(options, voteCounts, votingId);
            case PREFERENCE:
                return findWinnerPreferenceBased(options, voteCounts, votingId);
            default:
                return findWinnerSimpleMajority(options, voteCounts);
        }
    }

    @Override
    public boolean hasActiveVoting(Long meetingId) {
        return votingRepository.hasActiveVoting(meetingId);
    }

    @Override
    public List<VotingResponse> getExpiredVotings() {
        LocalDateTime now = LocalDateTime.now();
        List<MeetingVoting> expiredVotings = votingRepository.findExpiredVotings(now);

        return expiredVotings.stream()
                .map(voting -> mapToVotingResponse(voting, voting.getMeeting().getOrganizer().getId()))
                .collect(Collectors.toList());
    }

    // ========== PRIVATE METHODS ==========

    private WinningOptionResponse findWinnerSimpleMajority(List<VotingOption> options,
                                                           Map<Long, Integer> voteCounts) {
        int totalVotes = voteCounts.values().stream().mapToInt(Integer::intValue).sum();

        List<WinningOptionResponse> candidates = options.stream()
                .map(option -> {
                    int votes = voteCounts.getOrDefault(option.getId(), 0);
                    double percentage = totalVotes > 0 ? (votes * 100.0) / totalVotes : 0.0;

                    return WinningOptionResponse.builder()
                            .optionId(option.getId())
                            .optionDate(option.getOptionDate())
                            .durationMinutes(option.getDurationMinutes())
                            .voteCount(votes)
                            .percentage(percentage)
                            .totalVoters(voteCounts.size())
                            .build();
                })
                .collect(Collectors.toList());

        // Znajdź zwycięzcę
        WinningOptionResponse winner = candidates.stream()
                .max(Comparator.comparing(WinningOptionResponse::getVoteCount)
                        .thenComparing(WinningOptionResponse::getPercentage))
                .orElse(null);

        // Sprawdź remis
        if (winner != null) {
            long winnersCount = candidates.stream()
                    .filter(c -> c.getVoteCount().equals(winner.getVoteCount()))
                    .count();
            winner.setIsTie(winnersCount > 1);
        }

        return winner;
    }

    private WinningOptionResponse findWinnerRankedChoice(List<VotingOption> options,
                                                         Map<Long, Integer> voteCounts,
                                                         Long votingId) {
        // Implementacja Instant Runoff Voting
        Map<Long, List<Integer>> preferences = getPreferenceOrders(votingId);
        return runRankedChoiceVoting(options, preferences);
    }

    private WinningOptionResponse findWinnerPreferenceBased(List<VotingOption> options,
                                                            Map<Long, Integer> voteCounts,
                                                            Long votingId) {
        // Implementacja metody Borda
        return runBordaCount(options, votingId);
    }

    private Map<Long, Integer> getVoteCountsByOption(Long votingId) {
        List<Object[]> voteCounts = voteRepository.countVotesByOption(votingId);
        Map<Long, Integer> result = new HashMap<>();

        if (voteCounts != null) {
            for (Object[] row : voteCounts) {
                if (row.length >= 2 && row[0] != null && row[1] != null) {
                    Long optionId = ((Number) row[0]).longValue();
                    Long count = ((Number) row[1]).longValue();
                    result.put(optionId, count.intValue());
                }
            }
        }

        return result;
    }

    private Map<Long, List<Integer>> getPreferenceOrders(Long votingId) {
        // Pobierz głosy z preferencjami
        List<Vote> votes = voteRepository.findByVotingId(votingId);
        Map<Long, List<Integer>> preferences = new HashMap<>();

        for (Vote vote : votes) {
            Long userId = vote.getUser().getId();
            Integer preference = vote.getPreferenceOrder();

            if (preference != null) {
                preferences.computeIfAbsent(userId, k -> new ArrayList<>()).add(preference);
            }
        }

        return preferences;
    }

    private WinningOptionResponse runRankedChoiceVoting(List<VotingOption> options,
                                                        Map<Long, List<Integer>> preferences) {
        // Uproszczona implementacja Ranked Choice Voting
        // W rzeczywistej implementacji należy dodać pełny algorytm
        if (preferences.isEmpty()) {
            return findWinnerSimpleMajority(options, new HashMap<>());
        }

        // Dla uproszczenia używamy metody Bordy
        return runBordaCountFromPreferences(options, preferences);
    }

    private WinningOptionResponse runBordaCount(List<VotingOption> options, Long votingId) {
        Map<Long, Integer> bordaScores = new HashMap<>();
        List<Vote> votes = voteRepository.findByVotingId(votingId);

        for (Vote vote : votes) {
            if (vote.getPreferenceOrder() != null) {
                Long optionId = vote.getOption().getId();
                int points = options.size() - vote.getPreferenceOrder();
                bordaScores.merge(optionId, points, Integer::sum);
            }
        }

        return bordaScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> {
                    VotingOption option = options.stream()
                            .filter(o -> o.getId().equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);

                    int totalVotes = bordaScores.values().stream().mapToInt(Integer::intValue).sum();
                    double percentage = totalVotes > 0 ? (entry.getValue() * 100.0) / totalVotes : 0.0;

                    return WinningOptionResponse.builder()
                            .optionId(option.getId())
                            .optionDate(option.getOptionDate())
                            .durationMinutes(option.getDurationMinutes())
                            .voteCount(entry.getValue())
                            .percentage(percentage)
                            .algorithmUsed("Metoda Bordy")
                            .build();
                })
                .orElse(null);
    }

    private WinningOptionResponse runBordaCountFromPreferences(List<VotingOption> options,
                                                               Map<Long, List<Integer>> preferences) {
        Map<Long, Integer> bordaScores = new HashMap<>();

        for (List<Integer> userPreferences : preferences.values()) {
            for (int i = 0; i < userPreferences.size(); i++) {
                // Zakładamy, że userPreferences zawiera ID opcji w kolejności preferencji
                if (i < userPreferences.size()) {
                    Long optionId = ((Number) userPreferences.get(i)).longValue();
                    int points = options.size() - i - 1;
                    bordaScores.merge(optionId, points, Integer::sum);
                }
            }
        }

        return bordaScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> {
                    VotingOption option = options.stream()
                            .filter(o -> o.getId().equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);

                    return WinningOptionResponse.builder()
                            .optionId(option.getId())
                            .optionDate(option.getOptionDate())
                            .durationMinutes(option.getDurationMinutes())
                            .voteCount(entry.getValue())
                            .percentage(0.0)
                            .algorithmUsed("Metoda Bordy z preferencjami")
                            .build();
                })
                .orElse(null);
    }

    private void autoSelectWinningOption(MeetingVoting voting) {
        try {
            WinningOptionResponse winner = findOptimalTime(voting.getId());
            if (winner != null) {
                // Zaktualizuj spotkanie zwycięskim terminem
                Meeting meeting = voting.getMeeting();
                meeting.setStartDate(winner.getOptionDate());
                if (winner.getDurationMinutes() != null) {
                    meeting.setEndDate(winner.getOptionDate().plusMinutes(winner.getDurationMinutes()));
                }
                meetingRepository.save(meeting);
                log.info("Auto-selected winning option for meeting: {}", meeting.getId());
            }
        } catch (Exception e) {
            log.error("Error auto-selecting winning option for voting {}: {}", voting.getId(), e.getMessage());
        }
    }

    public void validateVotingPermission(MeetingVoting voting, Long userId) {
        boolean isOrganizer = voting.getMeeting().getOrganizer().getId().equals(userId);
        boolean isParticipant = participantRepository.existsByMeetingIdAndUserId(
                voting.getMeeting().getId(), userId);

        if (!isOrganizer && !isParticipant) {
            throw new VotingAccessDeniedException("Nie masz uprawnień do udziału w tym głosowaniu");
        }
    }

    private void validateVoteRequest(MeetingVoting voting, VoteRequest request) {
        if (request.getOptionIds() == null || request.getOptionIds().isEmpty()) {
            throw new RuntimeException("Musisz wybrać przynajmniej jedną opcję");
        }

        if (voting.getMaxChoices() != null && request.getOptionIds().size() > voting.getMaxChoices()) {
            throw new RuntimeException("Możesz wybrać maksymalnie " + voting.getMaxChoices() + " opcji");
        }

        // Sprawdź czy opcje należą do tego głosowania
        List<VotingOption> validOptions = optionRepository.findByVotingId(voting.getId());
        List<Long> validOptionIds = validOptions.stream()
                .map(VotingOption::getId)
                .collect(Collectors.toList());

        for (Long optionId : request.getOptionIds()) {
            if (!validOptionIds.contains(optionId)) {
                throw new RuntimeException("Nieprawidłowe opcje głosowania: " + optionId);
            }
        }

        // Walidacja preferencji dla głosowania rankingowego
        if (voting.getType() == VotingType.RANKED || voting.getType() == VotingType.PREFERENCE) {
            if (request.getPreferenceOrder() == null || request.getPreferenceOrder().size() != request.getOptionIds().size()) {
                throw new RuntimeException("Dla głosowania rankingowego wymagane są preferencje dla wszystkich opcji");
            }
        }
    }

    private Integer calculateVoteWeight(Long userId, Long meetingId) {
        // Możesz dostosować wagę głosu na podstawie roli użytkownika
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting != null && meeting.getOrganizer().getId().equals(userId)) {
            return 2; // Organizator ma podwójny głos
        }
        return 1;
    }


    private VotingResponse mapToVotingResponse(MeetingVoting voting, Long userId) {
        System.out.println("=== DEBUG: mapToVotingResponse ===");
        System.out.println("Voting ID: " + voting.getId());
        System.out.println("Original options size: " + voting.getOptions().size());

        List<VotingOptionResponse> optionResponses = voting.getOptions().stream()
                .map(option -> {
                    System.out.println("Mapping option ID: " + option.getId() + ", date: " + option.getOptionDate());
                    VotingOptionResponse response = mapToOptionResponse(option, userId);
                    System.out.println("Mapped option - ID: " + response.getId() + ", voteCount: " + response.getVoteCount());
                    return response;
                })
                .collect(Collectors.toList());

        System.out.println("Mapped options size: " + optionResponses.size());

        // Reszta metody pozostaje bez zmian...
        VoteStatsResponse stats = calculateVotingStats(voting);
        List<VoteResponse> userVotes = getUserVotes(voting.getId(), userId);

        int totalVotes = optionResponses.stream()
                .mapToInt(opt -> opt.getVoteCount() != null ? opt.getVoteCount() : 0)
                .sum();

        System.out.println("Total votes: " + totalVotes);

        for (VotingOptionResponse option : optionResponses) {
            if (totalVotes > 0 && option.getVoteCount() != null) {
                double percentage = (option.getVoteCount() * 100.0) / totalVotes;
                option.setPercentage(percentage);
            }
        }

        VotingResponse result = VotingResponse.builder()
                .id(voting.getId())
                .title(voting.getTitle())
                .description(voting.getDescription())
                .status(voting.getStatus())
                .type(voting.getType())
                .maxChoices(voting.getMaxChoices())
                .allowSuggestions(voting.getAllowSuggestions())
                .deadlineDate(voting.getDeadlineDate())
                .autoClose(voting.getAutoClose())
                .createdAt(voting.getCreatedAt())
                .updatedAt(voting.getUpdatedAt())
                .options(optionResponses)
                .stats(stats)
                .hasVoted(!userVotes.isEmpty())
                .userVotes(userVotes)
                .createdBy(mapToUserResponse(voting.getMeeting().getOrganizer()))
                .canVote(canUserVote(voting, userId))
                .canManage(canUserManage(voting, userId))
                .build();

        System.out.println("Final VotingResponse options size: " + result.getOptions().size());
        return result;
    }



    private VoteStatsResponse calculateVotingStats(MeetingVoting voting) {
        long totalVotes = voteRepository.countByVotingId(voting.getId());
        long totalVoters = voteRepository.countDistinctVotersByVotingId(voting.getId());
        long totalOptions = optionRepository.countByVotingId(voting.getId());

        // Oblicz całkowitą liczbę uczestników spotkania
        int totalParticipants = participantRepository.findByMeetingId(voting.getMeeting().getId()).size();

        WinningOptionResponse winningOption = null;
        if (voting.getStatus() == VotingStatus.CLOSED) {
            winningOption = findOptimalTime(voting.getId());
        }

        return VoteStatsResponse.builder()
                .totalVotes((int) totalVotes)
                .totalVoters((int) totalVoters)
                .totalOptions((int) totalOptions)
                .votingEndsIn(voting.getDeadlineDate())
                .isClosed(voting.getStatus() == VotingStatus.CLOSED)
                .winningOption(winningOption)
                .calculateDerivedStats(totalParticipants)
                .build();
    }


    // Alternatywnie - prostsze podejście bez lambdy
    private List<VoteResponse> getUserVotes(Long votingId, Long userId) {
        List<Vote> votes = voteRepository.findUserVotes(votingId, userId);
        List<VoteResponse> voteResponses = new ArrayList<>();

        for (Vote vote : votes) {
            VoteResponse voteResponse = VoteResponse.builder()
                    .id(vote.getId())
                    .optionId(vote.getOption().getId())
                    .votedAt(vote.getVotedAt())
                    .preferenceOrder(vote.getPreferenceOrder())
                    .voteWeight(vote.getVoteWeight())
                    .optionDate(vote.getOption() != null ? vote.getOption().getOptionDate() : null)
                    .durationMinutes(vote.getOption() != null ? vote.getOption().getDurationMinutes() : null)
                    .build();

            voteResponses.add(voteResponse);
        }

        return voteResponses;
    }

    // Poprawiona metoda mapowania VotingOptionResponse
    private VotingOptionResponse mapToOptionResponse(VotingOption option, Long userId) {
        long voteCount = voteRepository.countByOptionId(option.getId());
        boolean userVotedFor = voteRepository.existsByVotingIdAndUserIdAndOptionId(
                option.getVoting().getId(), userId, option.getId());

        AtomicReference<String> suggestedByName = new AtomicReference<>();
        if (option.getSuggestedBy() != null) {
            userRepository.findById(option.getSuggestedBy())
                    .ifPresent(user -> suggestedByName.set(user.getFirstName() + " " + user.getLastName()));
        }

        // Użyj buildera z przypisaniem do zmiennej
        VotingOptionResponse.VotingOptionResponseBuilder builder = VotingOptionResponse.builder()
                .id(option.getId())
                .optionDate(option.getOptionDate())
                .durationMinutes(option.getDurationMinutes())
                .isSuggested(option.getIsSuggested())
                .suggestedBy(option.getSuggestedBy())
                .voteCount((int) voteCount)
                .userVotedFor(userVotedFor);

        // Ustaw suggestedByName jeśli znalezione
        if (suggestedByName.get() != null) {
            builder.suggestedByName(suggestedByName.get());
        }

        return builder.build();
    }

    private UserResponse mapToUserResponse(User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }

    private boolean canUserVote(MeetingVoting voting, Long userId) {
        if (userId == null) return false;
        if (voting.getStatus() != VotingStatus.ACTIVE) return false;
        if (voting.getDeadlineDate() != null && LocalDateTime.now().isAfter(voting.getDeadlineDate())) return false;

        List<VoteResponse> userVotes = getUserVotes(voting.getId(), userId);
        return userVotes.isEmpty();
    }

    private boolean canUserManage(MeetingVoting voting, Long userId) {
        if (userId == null) return false;
        return voting.getMeeting().getOrganizer().getId().equals(userId);
    }
    @Transactional(readOnly = true)
    @Override
    public MeetingVoting getVotingEntity(Long votingId) {
        return votingRepository.findById(votingId)
                .orElseThrow(() -> new RuntimeException("Głosowanie nie zostało znalezione"));
    }

    @Override
    public VotingResponse getVotingDetailsForUser(Long votingId, Long userId) {
        MeetingVoting voting = getVotingEntity(votingId);
        validateVotingPermission(voting, userId);
        closeExpiredVotingIfNeeded(votingId);
        return mapToVotingResponse(voting, userId);
    }


    @Transactional(readOnly = true)
    @Override
    public void validateUserCanVote(Long meetingId, Long votingId, Long userId) {
        MeetingVoting voting = votingRepository.findById(votingId)
                .orElseThrow(() -> new RuntimeException("Głosowanie nie zostało znalezione"));

        Meeting meeting = voting.getMeeting();

        boolean isOrganizer = meeting.getOrganizer().getId().equals(userId);
        boolean isParticipant = participantRepository.existsByMeetingIdAndUserId(meetingId, userId);

        if (!isOrganizer && !isParticipant) {
            throw new VotingAccessDeniedException("Tylko uczestnicy spotkania mogą głosować");
        }
    }


}



