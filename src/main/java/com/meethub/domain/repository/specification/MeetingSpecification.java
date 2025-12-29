

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class MeetingSpecification {

    public static Specification<Meeting> buildDynamicSpecification(
            String keywords,
            List<String> searchFields,
            String tags,
            LocalDate dateFrom,
            LocalDate dateTo,
            MeetingType type,
            List<String> statuses,
            Integer minParticipants,
            Integer maxParticipants,
            String organizerName,
            String myParticipation,
            Long currentUserId,
            MeetingVisibility visibility,
//            List<Long> categoryIds,
            Boolean recurringOnly,
            Boolean templatesOnly,
            Boolean hasAttachments
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keywords)) {
                predicates.add(buildTextPredicate(root, cb, keywords, searchFields));
            }

            if (StringUtils.hasText(tags)) {
                predicates.add(buildTagsPredicate(root, cb, tags));
            }

            if (dateFrom != null || dateTo != null) {
                predicates.add(buildDateRangePredicate(root, cb, dateFrom, dateTo));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(buildStatusPredicate(root, cb, statuses));
            }

            if (minParticipants != null && minParticipants > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxParticipants"), minParticipants));
            }
            if (maxParticipants != null && maxParticipants < 100) {
                predicates.add(cb.lessThanOrEqualTo(root.get("maxParticipants"), maxParticipants));
            }

            if (StringUtils.hasText(organizerName)) {
                predicates.add(buildOrganizerPredicate(root, query, cb, organizerName));
            }

            if (currentUserId != null && StringUtils.hasText(myParticipation)) {
                predicates.add(buildMyParticipationPredicate(root, query, cb, currentUserId, myParticipation));
            }

            predicates.add(buildVisibilityPredicate(root, query, cb, currentUserId, visibility));

            if (Boolean.TRUE.equals(recurringOnly)) {
                predicates.add(cb.isTrue(root.get("recurring")));
            }

            if (Boolean.TRUE.equals(templatesOnly)) {
                predicates.add(cb.isTrue(root.get("template")));
            }

            if (Boolean.TRUE.equals(hasAttachments)) {
                predicates.add(cb.isNotEmpty(root.get("attachments")));
            }

            query.distinct(true);

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    private static Predicate buildTextPredicate(Root<Meeting> root, CriteriaBuilder cb,
                                                String keywords, List<String> searchFields) {
        String keyword = "%" + keywords.toLowerCase() + "%";
        List<Predicate> textPredicates = new ArrayList<>();

        // Domyślne pola
        textPredicates.add(cb.like(cb.lower(root.get("title")), keyword));
        textPredicates.add(cb.like(cb.lower(root.get("description")), keyword));

        // Opcjonalne pola
        if (searchFields != null) {
            if (searchFields.contains("AGENDA")) {
                textPredicates.add(cb.like(cb.lower(root.get("agenda")), keyword));
            }
            if (searchFields.contains("LOCATION")) {
                textPredicates.add(cb.like(cb.lower(root.get("location")), keyword));
            }
        }

        return cb.or(textPredicates.toArray(new Predicate[0]));
    }

    public static Predicate buildTagsPredicate(Root<Meeting> root, CriteriaBuilder cb, String tags) {
        String[] tagArray = tags.split(",");
        List<Predicate> tagPredicates = new ArrayList<>();

        for (String tag : tagArray) {
            String trimmedTag = tag.trim();
            if (!trimmedTag.isEmpty()) {
                tagPredicates.add(cb.isMember(trimmedTag, root.get("tags")));
            }
        }

        return cb.and(tagPredicates.toArray(new Predicate[0]));
    }

    public static Predicate buildDateRangePredicate(Root<Meeting> root, CriteriaBuilder cb,
                                                    LocalDate dateFrom, LocalDate dateTo) {
        List<Predicate> datePredicates = new ArrayList<>();

        if (dateFrom != null) {
            LocalDateTime startOfDay = dateFrom.atStartOfDay();
            datePredicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), startOfDay));
        }

        if (dateTo != null) {
            LocalDateTime endOfDay = dateTo.atTime(LocalTime.MAX);
            datePredicates.add(cb.lessThanOrEqualTo(root.get("startDate"), endOfDay));
        }

        return cb.and(datePredicates.toArray(new Predicate[0]));
    }

    private static Predicate buildStatusPredicate(Root<Meeting> root, CriteriaBuilder cb,
                                                  List<String> statuses) {
        List<Predicate> statusPredicates = new ArrayList<>();

        for (String statusStr : statuses) {
            try {
                MeetingStatus status = MeetingStatus.valueOf(statusStr);
                statusPredicates.add(cb.equal(root.get("status"), status));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid meeting status: {}", statusStr);
            }
        }

        return statusPredicates.isEmpty() ? cb.conjunction() :
                cb.or(statusPredicates.toArray(new Predicate[0]));
    }

    public static Predicate buildOrganizerPredicate(Root<Meeting> root, CriteriaQuery<?> query,
                                                    CriteriaBuilder cb, String organizerName) {
        Join<Meeting, User> organizerJoin = root.join("organizer", JoinType.LEFT);
        String namePattern = "%" + organizerName.toLowerCase() + "%";

        return cb.or(
                cb.like(cb.lower(organizerJoin.get("firstName")), namePattern),
                cb.like(cb.lower(organizerJoin.get("lastName")), namePattern),
                cb.like(cb.lower(organizerJoin.get("email")), namePattern)
        );
    }

    private static Predicate buildMyParticipationPredicate(Root<Meeting> root, CriteriaQuery<?> query,
                                                           CriteriaBuilder cb, Long currentUserId,
                                                           String myParticipation) {
        switch (myParticipation) {
            case "ORGANIZER":
                return cb.equal(root.get("organizer").get("id"), currentUserId);

            case "CONFIRMED":
                return buildParticipationStatusPredicate(root, query, cb, currentUserId,
                        ParticipationStatus.CONFIRMED);

            case "PENDING":
                return buildParticipationStatusPredicate(root, query, cb, currentUserId,
                        ParticipationStatus.PENDING);

            case "INVITED":
                return buildParticipationStatusPredicate(root, query, cb, currentUserId,
                        ParticipationStatus.INVITED);

            case "DECLINED":
                return buildParticipationStatusPredicate(root, query, cb, currentUserId,
                        ParticipationStatus.DECLINED);

            case "ATTENDED":
                return buildParticipationStatusPredicate(root, query, cb, currentUserId,
                        ParticipationStatus.ATTENDED);

            default:
                return cb.conjunction();
        }
    }

    public static Predicate buildNotParticipatingPredicate(Root<Meeting> root, CriteriaQuery<?> query,
                                                           CriteriaBuilder cb, Long currentUserId) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<MeetingParticipant> participantRoot = subquery.from(MeetingParticipant.class);

        subquery.select(cb.literal(1L))
                .where(cb.and(
                        cb.equal(participantRoot.get("meeting").get("id"), root.get("id")),
                        cb.equal(participantRoot.get("user").get("id"), currentUserId)
                ));

        return cb.not(cb.exists(subquery));
    }

    public static Predicate buildParticipationStatusPredicate(Root<Meeting> root, CriteriaQuery<?> query,
                                                              CriteriaBuilder cb, Long currentUserId,
                                                              ParticipationStatus status) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<MeetingParticipant> participantRoot = subquery.from(MeetingParticipant.class);

        subquery.select(cb.literal(1L))
                .where(cb.and(
                        cb.equal(participantRoot.get("meeting").get("id"), root.get("id")),
                        cb.equal(participantRoot.get("user").get("id"), currentUserId),
                        cb.equal(participantRoot.get("status"), status)
                ));

        return cb.exists(subquery);
    }

    private static Predicate buildVisibilityPredicate(Root<Meeting> root, CriteriaQuery<?> query,
                                                      CriteriaBuilder cb, Long currentUserId,
                                                      MeetingVisibility visibility) {
        if (visibility != null) {
            return cb.equal(root.get("visibility"), visibility);
        }

        if (currentUserId != null) {
            Subquery<Long> participantSubquery = query.subquery(Long.class);
            Root<MeetingParticipant> participantRoot = participantSubquery.from(MeetingParticipant.class);

            participantSubquery.select(cb.literal(1L))
                    .where(cb.and(
                            cb.equal(participantRoot.get("meeting").get("id"), root.get("id")),
                            cb.equal(participantRoot.get("user").get("id"), currentUserId),
                            participantRoot.get("status").in(
                                    ParticipationStatus.CONFIRMED,
                                    ParticipationStatus.PENDING,
                                    ParticipationStatus.INVITED
                            )
                    ));

            return cb.or(
                    cb.equal(root.get("organizer").get("id"), currentUserId),
                    cb.exists(participantSubquery),
                    cb.equal(root.get("visibility"), MeetingVisibility.PUBLIC)
            );
        } else {
            return cb.equal(root.get("visibility"), MeetingVisibility.PUBLIC);
        }
    }


    public static Specification<Meeting> hasKeywords(String keywords, List<String> searchFields) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keywords)) return null;
            return buildTextPredicate(root, cb, keywords, searchFields);
        };
    }

    public static Specification<Meeting> byDateRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> buildDateRangePredicate(root, cb, from, to);
    }

    public static Specification<Meeting> byType(MeetingType type) {
        return (root, query, cb) -> type != null ? cb.equal(root.get("type"), type) : null;
    }


    public static Specification<Meeting> byStatuses(List<String> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) return null;

            log.info(" byStatuses called with: {}", statuses); // DODAJ LOG

            // Jeśli któryś status to "CONFIRMED" - to jest ParticipationStatus, nie MeetingStatus!
            for (String status : statuses) {
                if ("CONFIRMED".equalsIgnoreCase(status)) {
                    log.error("CONFIRMED is a ParticipationStatus, not MeetingStatus!");
                    log.error("   Available MeetingStatus values: {}",
                            Arrays.toString(MeetingStatus.values()));
                    // Możesz albo zignorować, albo rzucić wyjątek
                    throw new IllegalArgumentException(
                            "CONFIRMED is not a valid MeetingStatus. Use ParticipationStatus for user participation."
                    );
                }
            }

            return buildStatusPredicate(root, cb, statuses);
        };
    }

    public static Specification<Meeting> accessibleToUser(Long userId) {
        return (root, query, cb) -> buildVisibilityPredicate(root, query, cb, userId, null);
    }

    public static Specification<Meeting> isRecurring() {
        return (root, query, cb) -> cb.isTrue(root.get("recurring"));
    }

    public static Specification<Meeting> isTemplate() {
        return (root, query, cb) -> cb.isTrue(root.get("template"));
    }

    public static Specification<Meeting> hasAttachments() {
        return (root, query, cb) -> cb.isNotEmpty(root.get("attachments"));
    }





    public static Specification<Meeting> buildSpecification(SearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getKeywords())) {
                predicates.add(buildTextPredicate(root, cb, criteria.getKeywords(), criteria.getSearchFields()));
            }

            if (StringUtils.hasText(criteria.getTags())) {
                predicates.add(buildTagsPredicate(root, cb, criteria.getTags()));
            }

            if (criteria.getDateFrom() != null || criteria.getDateTo() != null) {
                predicates.add(buildDateRangePredicate(root, cb, criteria.getDateFrom(), criteria.getDateTo()));
            }

            if (criteria.hasType()) {
                predicates.add(cb.equal(root.get("type"), criteria.getType()));
            }

            if (criteria.hasStatuses()) {
                predicates.add(buildStatusPredicate(root, cb, criteria.getStatuses()));
            }

            if (criteria.getMinParticipants() != null && criteria.getMinParticipants() > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxParticipants"), criteria.getMinParticipants()));
            }

            if (criteria.getMaxParticipants() != null && criteria.getMaxParticipants() < 100) {
                predicates.add(cb.lessThanOrEqualTo(root.get("maxParticipants"), criteria.getMaxParticipants()));
            }

            if (StringUtils.hasText(criteria.getOrganizerName())) {
                predicates.add(buildOrganizerPredicate(root, query, cb, criteria.getOrganizerName()));
            }

            if (criteria.getCurrentUserId() != null && StringUtils.hasText(criteria.getMyParticipation())) {
                predicates.add(buildMyParticipationPredicate(root, query, cb,
                        criteria.getCurrentUserId(), criteria.getMyParticipation()));
            }

            predicates.add(buildVisibilityPredicate(root, query, cb,
                    criteria.getCurrentUserId(), criteria.getVisibility()));


            if (criteria.hasRecurringFilter()) {
                predicates.add(cb.isTrue(root.get("recurring")));
            }


            if (criteria.hasTemplatesFilter()) {
                predicates.add(cb.isTrue(root.get("template")));
            }

            if (criteria.hasCategoryFilter()) {
                predicates.add(buildCategoriesPredicate(root, cb, criteria.getCategoryIds()));
            }

            if (criteria.hasAttachmentsFilter()) {
                predicates.add(cb.isNotEmpty(root.get("attachments")));
            }

            query.distinct(true);

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate buildCategoriesPredicate(Root<Meeting> root, CriteriaBuilder cb,
                                                      List<Long> categoryIds) {
        Join<Meeting, ?> categoryJoin = root.join("categories", JoinType.INNER);
        return categoryJoin.get("id").in(categoryIds);
    }

}