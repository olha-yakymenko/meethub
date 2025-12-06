package com.meethub.domain.repository.specification;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.request.SearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class MeetingSpecification implements Specification<Meeting> {

    private final SearchCriteria criteria;
    private final Long currentUserId;

    @Override
    public Predicate toPredicate(Root<Meeting> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // ✅ Dołącz tabele potrzebne do joinów
        Join<Meeting, User> organizerJoin = root.join("organizer", JoinType.LEFT);

        // ✅ 1. Kryteria tekstowe
        if (StringUtils.hasText(criteria.getKeywords())) {
            String keyword = "%" + criteria.getKeywords().toLowerCase() + "%";

            List<Predicate> textPredicates = new ArrayList<>();

            // Domyślne pola do wyszukiwania
            textPredicates.add(cb.like(cb.lower(root.get("title")), keyword));
            textPredicates.add(cb.like(cb.lower(root.get("description")), keyword));

            // Opcjonalne pola do wyszukiwania
            if (criteria.getSearchFields() != null) {
                if (criteria.getSearchFields().contains("AGENDA")) {
                    textPredicates.add(cb.like(cb.lower(root.get("agenda")), keyword));
                }
                if (criteria.getSearchFields().contains("LOCATION")) {
                    textPredicates.add(cb.like(cb.lower(root.get("location")), keyword));
                }
            }

            predicates.add(cb.or(textPredicates.toArray(new Predicate[0])));
        }

        // ✅ 2. Tagi
        if (StringUtils.hasText(criteria.getTags())) {
            String[] tagArray = criteria.getTags().split(",");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    predicates.add(cb.isMember(trimmedTag, root.get("tags")));
                }
            }
        }

        // ✅ 3. Zakres dat
        if (criteria.getDateFrom() != null) {
            LocalDateTime startOfDay = criteria.getDateFrom().atStartOfDay();
            predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startOfDay));
        }

        if (criteria.getDateTo() != null) {
            LocalDateTime endOfDay = criteria.getDateTo().atTime(LocalTime.MAX);
            predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endOfDay));
        }

        // ✅ 4. Typ spotkania
        if (criteria.hasType()) {
            predicates.add(cb.equal(root.get("type"), criteria.getType()));
        }

        // ✅ 5. Statusy spotkań
        if (criteria.hasStatuses()) {
            List<Predicate> statusPredicates = new ArrayList<>();
            for (String statusStr : criteria.getStatuses()) {
                try {
                    MeetingStatus status = MeetingStatus.valueOf(statusStr);
                    statusPredicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid meeting status in search: {}", statusStr);
                }
            }
            if (!statusPredicates.isEmpty()) {
                predicates.add(cb.or(statusPredicates.toArray(new Predicate[0])));
            }
        }

        // ✅ 6. Liczba uczestników
        if (criteria.getMinParticipants() != null && criteria.getMinParticipants() > 0) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("maxParticipants"), criteria.getMinParticipants()));
        }

        if (criteria.getMaxParticipants() != null && criteria.getMaxParticipants() < 100) {
            predicates.add(cb.lessThanOrEqualTo(root.get("maxParticipants"), criteria.getMaxParticipants()));
        }

        // ✅ 7. Organizator
        if (StringUtils.hasText(criteria.getOrganizerName())) {
            String organizerName = "%" + criteria.getOrganizerName().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(organizerJoin.get("firstName")), organizerName),
                    cb.like(cb.lower(organizerJoin.get("lastName")), organizerName),
                    cb.like(cb.lower(organizerJoin.get("email")), organizerName)
            ));
        }

        // ✅ 8. Mój udział
        if (currentUserId != null && StringUtils.hasText(criteria.getMyParticipation())) {
            // Tworzymy subquery dla uczestników
            Subquery<Long> participantSubquery = query.subquery(Long.class);
            Root<MeetingParticipant> participantRoot = participantSubquery.from(MeetingParticipant.class);

            participantSubquery.select(cb.literal(1L))
                    .where(cb.and(
                            cb.equal(participantRoot.get("meeting").get("id"), root.get("id")),
                            cb.equal(participantRoot.get("user").get("id"), currentUserId)
                    ));

            switch (criteria.getMyParticipation()) {
                case "ORGANIZER":
                    predicates.add(cb.equal(root.get("organizer").get("id"), currentUserId));
                    break;

                case "CONFIRMED":
                    Join<Meeting, MeetingParticipant> confirmedJoin = root.join("participants", JoinType.LEFT);
                    predicates.add(cb.and(
                            cb.equal(confirmedJoin.get("user").get("id"), currentUserId),
                            cb.equal(confirmedJoin.get("participationStatus"), ParticipationStatus.CONFIRMED)
                    ));
                    break;

                case "PENDING":
                    Join<Meeting, MeetingParticipant> pendingJoin = root.join("participants", JoinType.LEFT);
                    predicates.add(cb.and(
                            cb.equal(pendingJoin.get("user").get("id"), currentUserId),
                            cb.equal(pendingJoin.get("participationStatus"), ParticipationStatus.PENDING)
                    ));
                    break;

                case "INVITED":
                    Join<Meeting, MeetingParticipant> invitedJoin = root.join("participants", JoinType.LEFT);
                    predicates.add(cb.and(
                            cb.equal(invitedJoin.get("user").get("id"), currentUserId),
                            cb.equal(invitedJoin.get("participationStatus"), ParticipationStatus.INVITED)
                    ));
                    break;

                case "NOT_PARTICIPATING":
                    // Nie jest uczestnikiem w żadnym statusie
                    predicates.add(cb.not(cb.exists(participantSubquery)));
                    break;

                case "DECLINED":
                    Join<Meeting, MeetingParticipant> declinedJoin = root.join("participants", JoinType.LEFT);
                    predicates.add(cb.and(
                            cb.equal(declinedJoin.get("user").get("id"), currentUserId),
                            cb.equal(declinedJoin.get("participationStatus"), ParticipationStatus.DECLINED)
                    ));
                    break;

                case "WAITING":
                    Join<Meeting, MeetingParticipant> waitingJoin = root.join("participants", JoinType.LEFT);
                    predicates.add(cb.and(
                            cb.equal(waitingJoin.get("user").get("id"), currentUserId),
                            cb.equal(waitingJoin.get("participationStatus"), ParticipationStatus.WAITING_LIST)
                    ));
                    break;

                case "ATTENDED":
                    Join<Meeting, MeetingParticipant> attendedJoin = root.join("participants", JoinType.LEFT);
                    predicates.add(cb.and(
                            cb.equal(attendedJoin.get("user").get("id"), currentUserId),
                            cb.equal(attendedJoin.get("participationStatus"), ParticipationStatus.ATTENDED)
                    ));
                    break;
            }
        }

        // ✅ 9. Widoczność
        if (criteria.hasVisibilityFilter()) {
            predicates.add(cb.equal(root.get("visibility"), criteria.getVisibility()));
        } else if (currentUserId != null) {
            // Dla zalogowanych użytkowników pokazuj spotkania, do których mają dostęp
            // (jest organizatorem, jest uczestnikiem, lub spotkanie jest publiczne)

            // Subquery dla uczestników
            Subquery<Long> participantAccessSubquery = query.subquery(Long.class);
            Root<MeetingParticipant> participantAccessRoot = participantAccessSubquery.from(MeetingParticipant.class);

            participantAccessSubquery.select(cb.literal(1L))
                    .where(cb.and(
                            cb.equal(participantAccessRoot.get("meeting").get("id"), root.get("id")),
                            cb.equal(participantAccessRoot.get("user").get("id"), currentUserId),
                            participantAccessRoot.get("participationStatus").in(
                                    ParticipationStatus.CONFIRMED,
                                    ParticipationStatus.PENDING,
                                    ParticipationStatus.INVITED,
                                    ParticipationStatus.TENTATIVE,
                                    ParticipationStatus.ORGANIZER,
                                    ParticipationStatus.CO_ORGANIZER
                            )
                    ));

            predicates.add(cb.or(
                    cb.equal(root.get("organizer").get("id"), currentUserId),
                    cb.exists(participantAccessSubquery),
                    cb.equal(root.get("visibility"), MeetingVisibility.PUBLIC)
            ));
        } else {
            // Dla niezalogowanych tylko publiczne
            predicates.add(cb.equal(root.get("visibility"), MeetingVisibility.PUBLIC));
        }

        // ✅ 10. Tylko cykliczne
        if (criteria.hasRecurringFilter()) {
            predicates.add(cb.isTrue(root.get("recurring")));
        }

        // ✅ 11. Tylko szablony
        if (criteria.hasTemplatesFilter()) {
            predicates.add(cb.isTrue(root.get("template")));
        }

        // ✅ 12. Kategorie
        if (criteria.hasCategoryFilter()) {
            Join<Meeting, ?> categoryJoin = root.join("categories");
            predicates.add(categoryJoin.get("id").in(criteria.getCategoryIds()));
        }

        // ✅ 13. Spotkania z załącznikami
        if (criteria.hasAttachmentsFilter()) {
            predicates.add(cb.isNotEmpty(root.get("attachments")));
        }

        // ✅ Ustaw distinct
        query.distinct(true);

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}