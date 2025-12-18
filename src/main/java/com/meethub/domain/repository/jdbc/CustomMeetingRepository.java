package com.meethub.domain.repository.jdbc;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.response.StatisticsResponse;
import com.meethub.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor

public class CustomMeetingRepository {

    private final JdbcTemplate jdbcTemplate;

    // ✅ Własny wyjątek dla błędów repozytorium
    public static class RepositoryException extends RuntimeException {
        private final String operation;
        private final String sql;

        public RepositoryException(String operation, String sql, Throwable cause) {
            super(String.format("Repository operation failed: %s [SQL: %s]", operation, sql), cause);
            this.operation = operation;
            this.sql = sql;
        }

        public String getOperation() { return operation; }
        public String getSql() { return sql; }
    }

    // ✅ GŁÓWNA METODA WYSZUKIWANIA Z FILTRAMI
    public Page<Meeting> findFilteredMeetings(String search, String type, String status, Pageable pageable) {
        log.info("🔍 JDBC Filtering - search: '{}', type: '{}', status: '{}', page: {}, size: {}",
                search, type, status, pageable.getPageNumber(), pageable.getPageSize());

        try {
            // ✅ BUDUJ PODSTAWOWE ZAPYTANIE
            StringBuilder sqlBuilder = new StringBuilder("""
                SELECT m.*,
                       u.id as organizer_id, u.first_name, u.last_name, u.email, u.phone_number,
                       u.created_at as user_created_at, u.updated_at as user_updated_at
                FROM meetings m
                LEFT JOIN users u ON m.organizer_id = u.id
                WHERE 1=1
                """);

            List<Object> params = new ArrayList<>();

            // ✅ DODAJ FILTRY
            addSearchFilter(sqlBuilder, params, search);
            addTypeFilter(sqlBuilder, params, type);
            addStatusFilter(sqlBuilder, params, status);

            // ✅ POBRZ LICZBĘ REKORDÓW (COUNT) - PRZED DODANIEM PAGINACJI
            String countSql = buildCountSql(sqlBuilder.toString());
            log.debug("📊 Count SQL: {}", countSql);

            Long total = 0L;
            try {
                total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            } catch (Exception e) {
                log.error("❌ Error executing count query: {}", e.getMessage());
                total = 0L;
            }

            if (total == null || total == 0) {
                log.info("⚠️ No meetings found for criteria");
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            // ✅ DODAJ SORTOWANIE I PAGINACJĘ
            sqlBuilder.append(" ORDER BY m.start_date DESC");
            sqlBuilder.append(" LIMIT ? OFFSET ?");

            params.add(pageable.getPageSize());
            params.add(pageable.getOffset());

            String finalSql = sqlBuilder.toString();
            log.debug("📋 Final SQL: {}", finalSql);
            log.debug("📋 SQL params: {}", params);

            // ✅ WYKONAJ ZAPYTANIE
            List<Meeting> meetings = jdbcTemplate.query(
                    finalSql,
                    new MeetingWithOrganizerRowMapper(),
                    params.toArray()
            );

            log.info("✅ Found {} filtered meetings (total: {})", meetings.size(), total);
            return new PageImpl<>(meetings, pageable, total);

        } catch (EmptyResultDataAccessException e) {
            log.info("📭 No meetings found for filters - returning empty page");
            return new PageImpl<>(Collections.emptyList(), pageable, 0);

        } catch (org.springframework.dao.DataAccessException e) {
            log.error("❌ Database error in findFilteredMeetings: {}", e.getMessage(), e);
            throw new BusinessException("Nie można pobrać spotkań. Proszę spróbować później.");

        } catch (Exception e) {
            log.error("💥 Unexpected error in findFilteredMeetings: {}", e.getMessage(), e);
            throw new RepositoryException("findFilteredMeetings", "filtered query", e);
        }
    }

    // ✅ METODA WYSZUKIWANIA SPOTKAŃ W POBLIŻU
    public List<Meeting> findNearbyMeetings(double latitude, double longitude, double radius, int limit) {
        log.info("📍 Finding nearby meetings - lat: {}, lng: {}, radius: {}, limit: {}",
                latitude, longitude, radius, limit);

        try {
            // ✅ UPROSZCZONE DLA TESTÓW (bez geolokalizacji)
            String sql = """
                SELECT m.*,
                       u.id as organizer_id, u.first_name, u.last_name, u.email, u.phone_number,
                       u.created_at as user_created_at, u.updated_at as user_updated_at
                FROM meetings m
                LEFT JOIN users u ON m.organizer_id = u.id
                WHERE m.visibility = 'PUBLIC'
                AND m.status = 'PLANNED'  -- W data.sql spotkania są PLANNED, nie CONFIRMED
                AND m.start_date > CURRENT_TIMESTAMP
                ORDER BY m.start_date ASC
                LIMIT ?
                """;

            List<Meeting> meetings = jdbcTemplate.query(
                    sql,
                    new MeetingWithOrganizerRowMapper(),
                    limit
            );

            log.info("✅ Found {} nearby meetings", meetings.size());
            return meetings;

        } catch (EmptyResultDataAccessException e) {
            log.info("📭 No nearby meetings found");
            return Collections.emptyList();

        } catch (org.springframework.dao.DataAccessException e) {
            log.error("❌ Database error finding nearby meetings: {}", e.getMessage(), e);
            throw new BusinessException("Nie można pobrać spotkań w pobliżu. Proszę spróbować później.");

        } catch (Exception e) {
            log.error("💥 Unexpected error finding nearby meetings: {}", e.getMessage(), e);
            throw new RepositoryException("findNearbyMeetings", "nearby query", e);
        }
    }

    // ✅ METODA POBRANIA STATYSTYK
    public List<StatisticsResponse> getMeetingStatistics(Long organizerId) {
        log.info("📊 Getting statistics for organizer: {}", organizerId);

        try {
            String sql = """
                SELECT
                    COUNT(*) as total_meetings,
                    COUNT(CASE WHEN m.status = 'COMPLETED' THEN 1 END) as completed_meetings,
                    COUNT(CASE WHEN m.status = 'CANCELLED' THEN 1 END) as cancelled_meetings,
                    COALESCE(AVG(EXTRACT(EPOCH FROM (m.end_date - m.start_date))/3600), 0) as avg_duration_hours,
                    COALESCE(SUM(CASE WHEN mp.status = 'CONFIRMED' THEN 1 ELSE 0 END), 0) as total_participants,
                    COUNT(CASE WHEN m.is_recurring = true THEN 1 END) as recurring_meetings  -- ZMIENIONE: is_recurring
                FROM meetings m
                LEFT JOIN meeting_participants mp ON m.id = mp.meeting_id
                WHERE m.organizer_id = ?
                GROUP BY m.organizer_id
                """;

            List<StatisticsResponse> statistics = jdbcTemplate.query(
                    sql,
                    new StatisticsRowMapper(),
                    organizerId
            );

            log.info("✅ Found {} statistics records", statistics.size());
            return statistics;

        } catch (EmptyResultDataAccessException e) {
            log.info("📭 No statistics found for organizer: {}", organizerId);
            return Collections.emptyList();

        } catch (org.springframework.dao.DataAccessException e) {
            log.error("❌ Database error getting statistics: {}", e.getMessage(), e);
            throw new BusinessException("Nie można pobrać statystyk. Proszę spróbować później.");

        } catch (Exception e) {
            log.error("💥 Unexpected error getting statistics: {}", e.getMessage(), e);
            throw new RepositoryException("getMeetingStatistics", "statistics query", e);
        }
    }

    // ✅ MASOWA AKTUALIZACJA STATUSU
    public int bulkUpdateMeetingStatus(List<Long> meetingIds, String newStatus) {
        log.info("🔄 Bulk updating {} meetings to status: {}", meetingIds.size(), newStatus);

        if (meetingIds == null || meetingIds.isEmpty()) {
            log.warn("⚠️ Empty meeting IDs list for bulk update");
            return 0;
        }

        try {
            // ✅ BEZPIECZNE ZAPYTANIE DLA LISTY ID (PostgreSQL syntax)
            String sql = "UPDATE meetings SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id IN (" +
                    String.join(",", Collections.nCopies(meetingIds.size(), "?")) + ")";

            List<Object> params = new ArrayList<>();
            params.add(newStatus);
            params.addAll(meetingIds);

            int updated = jdbcTemplate.update(sql, params.toArray());
            log.info("✅ Updated {} meetings", updated);
            return updated;

        } catch (org.springframework.dao.DataAccessException e) {
            log.error("❌ Database error in bulkUpdateMeetingStatus: {}", e.getMessage(), e);
            throw new BusinessException("Nie można zaktualizować statusu spotkań. Proszę spróbować później.");

        } catch (Exception e) {
            log.error("💥 Unexpected error in bulkUpdateMeetingStatus: {}", e.getMessage(), e);
            throw new RepositoryException("bulkUpdateMeetingStatus", "UPDATE meetings SET status", e);
        }
    }

    // ✅ USUWANIE STARYCH ANULOWANYCH SPOTKAŃ
    public int deleteOldCancelledMeetings(LocalDateTime cutoffDate) {
        log.info("🗑️ Deleting cancelled meetings older than: {}", cutoffDate);

        try {
            String sql = """
                DELETE FROM meetings 
                WHERE status = 'CANCELLED' 
                AND updated_at < ?
                AND id NOT IN (
                    SELECT DISTINCT original_meeting_id 
                    FROM meetings 
                    WHERE original_meeting_id IS NOT NULL
                )
                """;

            int deleted = jdbcTemplate.update(sql, Timestamp.valueOf(cutoffDate));
            log.info("✅ Deleted {} old cancelled meetings", deleted);
            return deleted;

        } catch (org.springframework.dao.DataAccessException e) {
            log.error("❌ Database error deleting old meetings: {}", e.getMessage(), e);
            throw new BusinessException("Nie można usunąć starych spotkań. Proszę spróbować później.");

        } catch (Exception e) {
            log.error("💥 Unexpected error deleting old meetings: {}", e.getMessage(), e);
            throw new RepositoryException("deleteOldCancelledMeetings", "DELETE FROM meetings", e);
        }
    }

    // ✅ MASOWE TWORZENIE Z SZABLONU
    public int bulkInsertFromTemplate(Long templateId, List<LocalDateTime> dates) {
        log.info("📄 Creating meetings from template {} for {} dates", templateId, dates.size());

        if (dates == null || dates.isEmpty()) {
            log.warn("⚠️ No dates provided for template creation");
            return 0;
        }

        try {
            // ✅ Dopasowane do schematu: is_recurring zamiast recurring, is_template
            String sql = """
                INSERT INTO meetings (
                    title, description, agenda, type, visibility, 
                    organizer_id, start_date, end_date, max_participants,
                    status, created_at, updated_at, original_meeting_id, is_template, is_recurring
                )
                SELECT 
                    title, description, agenda, type, visibility,
                    organizer_id, ?, 
                    ? + (end_date - start_date),
                    max_participants, 'PLANNED', CURRENT_TIMESTAMP, 
                    CURRENT_TIMESTAMP, id, false, false
                FROM meetings 
                WHERE id = ? AND is_template = true
                """;

            int totalInserted = 0;
            for (LocalDateTime date : dates) {
                int inserted = jdbcTemplate.update(sql,
                        Timestamp.valueOf(date),
                        Timestamp.valueOf(date),
                        templateId
                );
                totalInserted += inserted;
            }

            log.info("✅ Created {} meetings from template", totalInserted);
            return totalInserted;

        } catch (org.springframework.dao.DataAccessException e) {
            log.error("❌ Database error creating from template: {}", e.getMessage(), e);
            throw new BusinessException("Nie można utworzyć spotkań z szablonu. Proszę spróbować później.");

        } catch (Exception e) {
            log.error("💥 Unexpected error creating from template: {}", e.getMessage(), e);
            throw new RepositoryException("bulkInsertFromTemplate", "INSERT FROM template", e);
        }
    }

    // ✅ METODA POMOCNICZA DO POBRANIA LICZBY UCZESTNIKÓW (bez aktualizacji tabeli meetings)
    public int getMeetingParticipantsCount(Long meetingId) {
        log.info("👥 Getting participants count for meeting: {}", meetingId);

        try {
            String sql = """
                SELECT COUNT(*) 
                FROM meeting_participants mp 
                WHERE mp.meeting_id = ? 
                AND mp.status = 'CONFIRMED'
                """;

            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, meetingId);
            int result = count != null ? count : 0;
            log.info("✅ Meeting {} has {} confirmed participants", meetingId, result);
            return result;

        } catch (Exception e) {
            log.error("❌ Error getting participants count: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ✅ METODY POMOCNICZE

    private void addSearchFilter(StringBuilder sqlBuilder, List<Object> params, String search) {
        if (StringUtils.hasText(search)) {
            sqlBuilder.append(" AND (LOWER(m.title) LIKE LOWER(?) ");
            sqlBuilder.append(" OR LOWER(m.description) LIKE LOWER(?))");
            String searchPattern = "%" + search.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
    }

    private void addTypeFilter(StringBuilder sqlBuilder, List<Object> params, String type) {
        if (StringUtils.hasText(type)) {
            sqlBuilder.append(" AND m.type = ?");
            params.add(type.toUpperCase());
        }
    }

    private void addStatusFilter(StringBuilder sqlBuilder, List<Object> params, String status) {
        if (StringUtils.hasText(status)) {
            sqlBuilder.append(" AND m.status = ?");
            params.add(status.toUpperCase());
        }
    }

    private String buildCountSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            throw new IllegalArgumentException("SQL nie może być pusty");
        }

        // Usuń ORDER BY
        sql = sql.replaceAll("(?i)ORDER BY[\\s\\S]*$", "");

        // Usuń LIMIT i OFFSET
        sql = sql.replaceAll("(?i)LIMIT\\s+\\?", "");
        sql = sql.replaceAll("(?i)OFFSET\\s+\\?", "");

        // Znajdź FROM (bez wrażliwości na białe znaki i nowe linie)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)FROM\\s+");
        java.util.regex.Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Nie znaleziono FROM w zapytaniu SQL");
        }
        String fromClause = sql.substring(matcher.start());

        // Liczymy tylko m.id
        return "SELECT COUNT(m.id) " + fromClause;
    }




    // ✅ ROWMAPPER DLA SPOTKAŃ Z ORGANIZATOREM
    private static class MeetingWithOrganizerRowMapper implements RowMapper<Meeting> {
        @Override
        public Meeting mapRow(ResultSet rs, int rowNum) throws SQLException {
            Meeting meeting = new Meeting();

            // ✅ PODSTAWOWE POLA SPOTKANIA
            meeting.setId(rs.getLong("id"));
            meeting.setTitle(rs.getString("title"));
            meeting.setDescription(rs.getString("description"));
            meeting.setAgenda(rs.getString("agenda"));
            meeting.setMaxParticipants(rs.getObject("max_participants") != null ?
                    rs.getInt("max_participants") : null);
            meeting.setRecurring(rs.getBoolean("is_recurring")); // ZMIENIONE: is_recurring
            meeting.setTemplate(rs.getBoolean("is_template"));
            meeting.setRecurrencePattern(rs.getString("recurrence_pattern"));
            meeting.setRecurrenceExceptionsJson(rs.getString("recurrence_exceptions"));
            meeting.setOriginalMeetingId(rs.getObject("original_meeting_id") != null ?
                    rs.getLong("original_meeting_id") : null);

            // ✅ MAPUJ ORGANIZATORA
            User organizer = new User();
            organizer.setId(rs.getLong("organizer_id"));
            organizer.setFirstName(rs.getString("first_name"));
            organizer.setLastName(rs.getString("last_name"));
            organizer.setEmail(rs.getString("email"));
            organizer.setPhoneNumber(rs.getString("phone_number"));

            if (rs.getTimestamp("user_created_at") != null) {
                organizer.setCreatedAt(rs.getTimestamp("user_created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("user_updated_at") != null) {
                organizer.setUpdatedAt(rs.getTimestamp("user_updated_at").toLocalDateTime());
            }

            meeting.setOrganizer(organizer);

            // ✅ MAPOWANIE ENUMÓW
            mapEnums(rs, meeting);

            // ✅ DATY
            if (rs.getTimestamp("start_date") != null) {
                meeting.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
            }
            if (rs.getTimestamp("end_date") != null) {
                meeting.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
            }
            if (rs.getTimestamp("created_at") != null) {
                meeting.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                meeting.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }

            return meeting;
        }

        private void mapEnums(ResultSet rs, Meeting meeting) throws SQLException {
            // ✅ TYP SPOTKANIA
            String type = rs.getString("type");
            if (type != null) {
                try {
                    meeting.setType(MeetingType.valueOf(type));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid meeting type: {}, defaulting to ONLINE", type);
                    meeting.setType(MeetingType.ONLINE);
                }
            }

            // ✅ STATUS SPOTKANIA
            String status = rs.getString("status");
            if (status != null) {
                try {
                    meeting.setStatus(MeetingStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid meeting status: {}, defaulting to PLANNED", status);
                    meeting.setStatus(MeetingStatus.PLANNED);
                }
            }

            // ✅ WIDOCZNOŚĆ
            String visibility = rs.getString("visibility");
            if (visibility != null) {
                try {
                    meeting.setVisibility(MeetingVisibility.valueOf(visibility));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid meeting visibility: {}, defaulting to PRIVATE", visibility);
                    meeting.setVisibility(MeetingVisibility.PRIVATE);
                }
            }
        }
    }

    // ✅ ROWMAPPER DLA STATYSTYK
    private static class StatisticsRowMapper implements RowMapper<StatisticsResponse> {
        @Override
        public StatisticsResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            return StatisticsResponse.builder()
                    .totalMeetings(rs.getLong("total_meetings"))
                    .completedMeetings(rs.getLong("completed_meetings"))
                    .cancelledMeetings(rs.getLong("cancelled_meetings"))
                    .averageDuration(rs.getDouble("avg_duration_hours"))
                    .totalParticipants(rs.getLong("total_participants"))
                    .build();
        }
    }
}







//
//
//
//
//
//
//
//
//
//package com.meethub.domain.repository.jdbc;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.MeetingType;
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.response.StatisticsResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.RowMapper;
//import org.springframework.stereotype.Repository;
//import org.springframework.util.StringUtils;
//
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Repository
//@RequiredArgsConstructor
//public class CustomMeetingRepository {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    public Page<Meeting> findFilteredMeetings(String search, String type, String status, Pageable pageable) {
//        log.info("JDBC Filtering - search: '{}', type: '{}', status: '{}'", search, type, status);
//
//        // ✅ BUDUJ ZAPYTANIE GŁÓWNE
//        StringBuilder sql = new StringBuilder("""
//            SELECT m.*,
//                   u.id as organizer_id, u.first_name, u.last_name, u.email, u.phone_number,
//                   u.created_at as user_created_at, u.updated_at as user_updated_at
//            FROM meetings m
//            LEFT JOIN users u ON m.organizer_id = u.id
//            WHERE 1=1
//            """);
//
//        List<Object> params = new ArrayList<>();
//
//        // ✅ FILTR WYSZUKIWANIA
//        if (StringUtils.hasText(search)) {
//            sql.append(" AND (LOWER(m.title) LIKE LOWER(?) OR LOWER(m.description) LIKE LOWER(?))");
//            String searchPattern = "%" + search.trim() + "%";
//            params.add(searchPattern);
//            params.add(searchPattern);
//        }
//
//        // ✅ FILTR TYPU
//        if (StringUtils.hasText(type)) {
//            sql.append(" AND m.type = ?");
//            params.add(type.toUpperCase());
//        }
//
//        // ✅ FILTR STATUSU
//        if (StringUtils.hasText(status)) {
//            sql.append(" AND m.status = ?");
//            params.add(status.toUpperCase());
//        }
//
//        // ✅ SORTOWANIE
//        sql.append(" ORDER BY m.created_at DESC");
//
//        // ✅ COUNT QUERY (OSOBNE - BEZ LIMIT/OFFSET)
//        String countSql = "SELECT COUNT(*) FROM (" +
//                sql.toString()
//                        .replaceFirst("SELECT m.*,.*?FROM", "SELECT 1 FROM")
//                        .replaceFirst("ORDER BY.*", "")
//                        .replaceFirst("LIMIT.*", "")
//                        .replaceFirst("OFFSET.*", "")
//                + ") AS count_table";
//        log.debug("Count SQL: {}", countSql);
//        log.debug("Count params: {}", params);
//
//        Long total = 0L;
//        try {
//            total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
//        } catch (Exception e) {
//            log.error("Error executing count query: {}", e.getMessage());
//            total = 0L;
//        }
//
//        // ✅ DODAJ PAGINACJĘ DO GŁÓWNEGO ZAPYTANIA
//        sql.append(" LIMIT ? OFFSET ?");
//        params.add(pageable.getPageSize());
//        params.add(pageable.getOffset());
//
//        log.debug("Main SQL: {}", sql);
//        log.debug("Main params: {}", params);
//
//        // ✅ WYKONAJ ZAPYTANIE
//        List<Meeting> meetings = new ArrayList<>();
//        try {
//            meetings = jdbcTemplate.query(sql.toString(), new MeetingWithOrganizerRowMapper(), params.toArray());
//        } catch (Exception e) {
//            log.error("Error executing main query: {}", e.getMessage(), e);
//        }
//
//        log.info("Found {} filtered meetings (total: {})", meetings.size(), total);
//        return new PageImpl<>(meetings, pageable, total != null ? total : 0);
//    }
//
//    public List<Meeting> findNearbyMeetings(double latitude, double longitude, double radius, int limit) {
//        log.info("Finding nearby meetings - lat: {}, lng: {}, radius: {}, limit: {}", latitude, longitude, radius, limit);
//
//        // ✅ UPROSZCZONE ZAPYTANIE (bez geolokalizacji)
//        String sql = """
//            SELECT m.*,
//                   u.id as organizer_id, u.first_name, u.last_name, u.email, u.phone_number,
//                   u.created_at as user_created_at, u.updated_at as user_updated_at
//            FROM meetings m
//            LEFT JOIN users u ON m.organizer_id = u.id
//            WHERE m.visibility = 'PUBLIC'
//            AND m.status = 'CONFIRMED'
//            AND m.start_date > CURRENT_TIMESTAMP
//            ORDER BY m.start_date ASC
//            LIMIT ?
//            """;
//
//        try {
//            return jdbcTemplate.query(sql, new MeetingWithOrganizerRowMapper(), limit);
//        } catch (Exception e) {
//            log.error("Error finding nearby meetings: {}", e.getMessage());
//            return new ArrayList<>();
//        }
//    }
//
//    public List<StatisticsResponse> getMeetingStatistics(Long organizerId) {
//        String sql = """
//            SELECT
//                COUNT(*) as total_meetings,
//                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_meetings,
//                COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_meetings,
//                AVG(EXTRACT(EPOCH FROM (end_date - start_date))/3600) as avg_duration_hours,
//                SUM(CASE WHEN mp.status = 'CONFIRMED' THEN 1 ELSE 0 END) as total_participants
//            FROM meetings m
//            LEFT JOIN meeting_participants mp ON m.id = mp.meeting_id
//            WHERE m.organizer_id = ?
//            GROUP BY m.organizer_id
//            """;
//
//        try {
//            return jdbcTemplate.query(sql, new StatisticsRowMapper(), organizerId);
//        } catch (Exception e) {
//            log.error("Error getting meeting statistics: {}", e.getMessage());
//            return new ArrayList<>();
//        }
//    }
//
//    // ✅ NOWY ROWMAPPER Z ORGANIZATOREM
//    private static class MeetingWithOrganizerRowMapper implements RowMapper<Meeting> {
//        @Override
//        public Meeting mapRow(ResultSet rs, int rowNum) throws SQLException {
//            Meeting meeting = new Meeting();
//
//            // ✅ PODSTAWOWE INFORMACJE O SPOTKANIU
//            meeting.setId(rs.getLong("id"));
//            meeting.setTitle(rs.getString("title"));
//            meeting.setDescription(rs.getString("description"));
//            meeting.setAgenda(rs.getString("agenda"));
//
//            // ✅ MAPUJ ORGANIZATORA
//            User organizer = new User();
//            organizer.setId(rs.getLong("organizer_id"));
//            organizer.setFirstName(rs.getString("first_name"));
//            organizer.setLastName(rs.getString("last_name"));
//            organizer.setEmail(rs.getString("email"));
//            organizer.setPhoneNumber(rs.getString("phone_number"));
//            if (rs.getTimestamp("user_created_at") != null) {
//                organizer.setCreatedAt(rs.getTimestamp("user_created_at").toLocalDateTime());
//            }
//            if (rs.getTimestamp("user_updated_at") != null) {
//                organizer.setUpdatedAt(rs.getTimestamp("user_updated_at").toLocalDateTime());
//            }
//            meeting.setOrganizer(organizer);
//
//            // ✅ MAPOWANIE ENUMÓW Z OBSŁUGĄ BŁĘDÓW
//            mapEnums(rs, meeting);
//
//            // ✅ DATY
//            if (rs.getTimestamp("start_date") != null) {
//                meeting.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
//            }
//            if (rs.getTimestamp("end_date") != null) {
//                meeting.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
//            }
//
//            meeting.setMaxParticipants(rs.getObject("max_participants") != null ?
//                    rs.getInt("max_participants") : null);
//
//            if (rs.getTimestamp("created_at") != null) {
//                meeting.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
//            }
//            if (rs.getTimestamp("updated_at") != null) {
//                meeting.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
//            }
//
//            return meeting;
//        }

//        private void mapEnums(ResultSet rs, Meeting meeting) throws SQLException {
//            // ✅ TYP SPOTKANIA
//            String type = rs.getString("type");
//            if (type != null) {
//                try {
//                    meeting.setType(MeetingType.valueOf(type));
//                } catch (IllegalArgumentException e) {
//                    log.warn("Invalid meeting type: {}, defaulting to ONLINE", type);
//                    meeting.setType(MeetingType.ONLINE);
//                }
//            }
//
//            // ✅ STATUS SPOTKANIA
//            String status = rs.getString("status");
//            if (status != null) {
//                try {
//                    meeting.setStatus(MeetingStatus.valueOf(status));
//                } catch (IllegalArgumentException e) {
//                    log.warn("Invalid meeting status: {}, defaulting to PLANNED", status);
//                    meeting.setStatus(MeetingStatus.PLANNED);
//                }
//            }
//
//            // ✅ WIDOCZNOŚĆ
//            String visibility = rs.getString("visibility");
//            if (visibility != null) {
//                try {
//                    meeting.setVisibility(MeetingVisibility.valueOf(visibility));
//                } catch (IllegalArgumentException e) {
//                    log.warn("Invalid meeting visibility: {}, defaulting to PRIVATE", visibility);
//                    meeting.setVisibility(MeetingVisibility.PRIVATE);
//                }
//            }
//        }
//    }
//
//    private static class StatisticsRowMapper implements RowMapper<StatisticsResponse> {
//        @Override
//        public StatisticsResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
//            return StatisticsResponse.builder()
//                    .totalMeetings(rs.getLong("total_meetings"))
//                    .completedMeetings(rs.getLong("completed_meetings"))
//                    .cancelledMeetings(rs.getLong("cancelled_meetings"))
//                    .averageDuration(rs.getDouble("avg_duration_hours"))
//                    .totalParticipants(rs.getLong("total_participants"))
//                    .build();
//        }
//    }
//
//
//    // ✅ OPERACJE INSERT/UPDATE/DELETE z update()
//
//    public int bulkUpdateMeetingStatus(List<Long> meetingIds, String newStatus) {
//        String sql = "UPDATE meetings SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id IN (?)";
//
//        // Konwersja listy ID do stringa z przecinkami
//        String ids = String.join(",", meetingIds.stream()
//                .map(String::valueOf)
//                .collect(Collectors.toList()));
//
//        return jdbcTemplate.update(sql, newStatus, ids);
//    }
//
//    public int deleteOldCancelledMeetings(LocalDateTime cutoffDate) {
//        String sql = "DELETE FROM meetings WHERE status = 'CANCELLED' AND updated_at < ?";
//        return jdbcTemplate.update(sql, cutoffDate);
//    }
//
//    public int bulkInsertFromTemplate(Long templateId, List<LocalDateTime> dates) {
//        String sql = """
//        INSERT INTO meetings (
//            title, description, agenda, type, visibility,
//            organizer_id, start_date, end_date, max_participants,
//            status, created_at, updated_at, template_id
//        )
//        SELECT
//            title, description, agenda, type, visibility,
//            organizer_id, ?, DATE_ADD(?, INTERVAL
//                TIMESTAMPDIFF(MINUTE, start_date, end_date) MINUTE),
//            max_participants, 'PLANNED', CURRENT_TIMESTAMP,
//            CURRENT_TIMESTAMP, id
//        FROM meetings
//        WHERE id = ? AND template = true
//        """;
//
//        int totalInserted = 0;
//        for (LocalDateTime date : dates) {
//            totalInserted += jdbcTemplate.update(sql, date, date, templateId);
//        }
//        return totalInserted;
//    }
//
//    public int updateMeetingParticipantsCount(Long meetingId) {
//        String sql = """
//        UPDATE meetings m
//        SET confirmed_participants = (
//            SELECT COUNT(*)
//            FROM meeting_participants mp
//            WHERE mp.meeting_id = m.id AND mp.status = 'CONFIRMED'
//        )
//        WHERE m.id = ?
//        """;
//        return jdbcTemplate.update(sql, meetingId);
//    }
//}