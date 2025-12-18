package com.meethub.domain.service;

import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.enums.ParticipationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Validated
public interface ParticipationService {

    MeetingParticipant confirmParticipation(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    MeetingParticipant declineParticipation(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    MeetingParticipant markAsAttended(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

     Map<ParticipationStatus, Long> getResponseStatistics(
            @NotNull @Positive Long meetingId
    );

    Double getAverageResponseTime(
            @NotNull @Positive Long meetingId
    );

     boolean isUserParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean isUserConfirmed(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

     MeetingParticipant updateUserStatus(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId,
            @NotNull ParticipationStatus status
    );

    List<MeetingParticipant> getMeetingParticipants(
            @NotNull @Positive Long meetingId
    );

    List<MeetingParticipant> getConfirmedParticipants(
            @NotNull @Positive Long meetingId
    );

     MeetingParticipant addToWaitingList(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    MeetingParticipant promoteFromWaitingList(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );
}






//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.MeetingParticipant;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import java.util.List;
//import java.util.Map;
//
//public interface ParticipationService {
//
//    // Status uczestnictwa
//    MeetingParticipant confirmParticipation(Long meetingId, Long userId);
//    MeetingParticipant declineParticipation(Long meetingId, Long userId);
//
//    // Obecność
//    MeetingParticipant markAsAttended(Long meetingId, Long userId);
//
//    // Statystyki
//    Map<ParticipationStatus, Long> getResponseStatistics(Long meetingId);
//    Double getAverageResponseTime(Long meetingId);
////    List<MeetingParticipant> getSlowResponders(Long meetingId, int thresholdHours);
//
//    // Sprawdzanie
////    boolean hasUserResponded(Long meetingId, Long userId);
//    boolean isUserParticipant(Long meetingId, Long userId);
//    boolean isUserConfirmed(Long meetingId, Long userId);
//
////    // Operacje dla organizatora
//    MeetingParticipant updateUserStatus(Long meetingId, Long userId, ParticipationStatus status);
//    List<MeetingParticipant> getMeetingParticipants(Long meetingId);
//    List<MeetingParticipant> getConfirmedParticipants(Long meetingId);
////
////    // Lista rezerwowa
//    MeetingParticipant addToWaitingList(Long meetingId, Long userId);
//    MeetingParticipant promoteFromWaitingList(Long meetingId, Long userId);
//}