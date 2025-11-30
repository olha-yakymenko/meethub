//package com.meethub.domain.repository.jdbc;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.response.StatisticsResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.RowMapper;
//import org.springframework.stereotype.Repository;
//
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Repository
//@RequiredArgsConstructor
//public class CustomMeetingRepository {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    private static final String FIND_NEARBY_MEETINGS_SQL = """
//        SELECT m.*, l.latitude, l.longitude,
//               earth_distance(
//                   ll_to_earth(:lat, :lng),
//                   ll_to_earth(l.latitude, l.longitude)
//               ) as distance
//        FROM meetings m
//        JOIN locations l ON m.location_id = l.id
//        WHERE m.visibility = 'PUBLIC'
//          AND m.status = 'CONFIRMED'
//          AND m.start_date > CURRENT_TIMESTAMP
//          AND earth_distance(
//                  ll_to_earth(:lat, :lng),
//                  ll_to_earth(l.latitude, l.longitude)
//              ) < :radius
//        ORDER BY distance, m.start_date
//        LIMIT :limit
//        """;
//
//    public List<Meeting> findNearbyMeetings(double latitude, double longitude, double radius, int limit) {
//        return jdbcTemplate.query(FIND_NEARBY_MEETINGS_SQL, new MeetingRowMapper(), latitude, longitude, radius, limit);
//    }
//
//    private static class MeetingRowMapper implements RowMapper<Meeting> {
//        @Override
//        public Meeting mapRow(ResultSet rs, int rowNum) throws SQLException {
//            Meeting meeting = new Meeting();
//            meeting.setId(rs.getLong("id"));
//            meeting.setTitle(rs.getString("title"));
//            return meeting;
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
//        return jdbcTemplate.query(sql, new StatisticsRowMapper(), organizerId);
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
//}










package com.meethub.domain.repository.jdbc;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.response.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomMeetingRepository {

    private final JdbcTemplate jdbcTemplate;

    public Page<Meeting> findFilteredMeetings(String search, String type, String status, Pageable pageable) {
        log.info("JDBC Filtering - search: '{}', type: '{}', status: '{}'", search, type, status);

        // ✅ BUDUJ ZAPYTANIE GŁÓWNE
        StringBuilder sql = new StringBuilder("""
            SELECT m.*, 
                   u.id as organizer_id, u.first_name, u.last_name, u.email, u.phone_number,
                   u.created_at as user_created_at, u.updated_at as user_updated_at
            FROM meetings m
            LEFT JOIN users u ON m.organizer_id = u.id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        // ✅ FILTR WYSZUKIWANIA
        if (StringUtils.hasText(search)) {
            sql.append(" AND (LOWER(m.title) LIKE LOWER(?) OR LOWER(m.description) LIKE LOWER(?))");
            String searchPattern = "%" + search.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        // ✅ FILTR TYPU
        if (StringUtils.hasText(type)) {
            sql.append(" AND m.type = ?");
            params.add(type.toUpperCase());
        }

        // ✅ FILTR STATUSU
        if (StringUtils.hasText(status)) {
            sql.append(" AND m.status = ?");
            params.add(status.toUpperCase());
        }

        // ✅ SORTOWANIE
        sql.append(" ORDER BY m.created_at DESC");

        // ✅ COUNT QUERY (OSOBNE - BEZ LIMIT/OFFSET)
        String countSql = "SELECT COUNT(*) FROM (" +
                sql.toString()
                        .replaceFirst("SELECT m.*,.*?FROM", "SELECT 1 FROM")
                        .replaceFirst("ORDER BY.*", "")
                        .replaceFirst("LIMIT.*", "")
                        .replaceFirst("OFFSET.*", "")
                + ") AS count_table";
        log.debug("Count SQL: {}", countSql);
        log.debug("Count params: {}", params);

        Long total = 0L;
        try {
            total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        } catch (Exception e) {
            log.error("Error executing count query: {}", e.getMessage());
            total = 0L;
        }

        // ✅ DODAJ PAGINACJĘ DO GŁÓWNEGO ZAPYTANIA
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());

        log.debug("Main SQL: {}", sql);
        log.debug("Main params: {}", params);

        // ✅ WYKONAJ ZAPYTANIE
        List<Meeting> meetings = new ArrayList<>();
        try {
            meetings = jdbcTemplate.query(sql.toString(), new MeetingWithOrganizerRowMapper(), params.toArray());
        } catch (Exception e) {
            log.error("Error executing main query: {}", e.getMessage(), e);
        }

        log.info("Found {} filtered meetings (total: {})", meetings.size(), total);
        return new PageImpl<>(meetings, pageable, total != null ? total : 0);
    }

    public List<Meeting> findNearbyMeetings(double latitude, double longitude, double radius, int limit) {
        log.info("Finding nearby meetings - lat: {}, lng: {}, radius: {}, limit: {}", latitude, longitude, radius, limit);

        // ✅ UPROSZCZONE ZAPYTANIE (bez geolokalizacji)
        String sql = """
            SELECT m.*, 
                   u.id as organizer_id, u.first_name, u.last_name, u.email, u.phone_number,
                   u.created_at as user_created_at, u.updated_at as user_updated_at
            FROM meetings m 
            LEFT JOIN users u ON m.organizer_id = u.id
            WHERE m.visibility = 'PUBLIC' 
            AND m.status = 'CONFIRMED' 
            AND m.start_date > CURRENT_TIMESTAMP
            ORDER BY m.start_date ASC 
            LIMIT ?
            """;

        try {
            return jdbcTemplate.query(sql, new MeetingWithOrganizerRowMapper(), limit);
        } catch (Exception e) {
            log.error("Error finding nearby meetings: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<StatisticsResponse> getMeetingStatistics(Long organizerId) {
        String sql = """
            SELECT 
                COUNT(*) as total_meetings,
                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_meetings,
                COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_meetings,
                AVG(EXTRACT(EPOCH FROM (end_date - start_date))/3600) as avg_duration_hours,
                SUM(CASE WHEN mp.status = 'CONFIRMED' THEN 1 ELSE 0 END) as total_participants
            FROM meetings m
            LEFT JOIN meeting_participants mp ON m.id = mp.meeting_id
            WHERE m.organizer_id = ?
            GROUP BY m.organizer_id
            """;

        try {
            return jdbcTemplate.query(sql, new StatisticsRowMapper(), organizerId);
        } catch (Exception e) {
            log.error("Error getting meeting statistics: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ✅ NOWY ROWMAPPER Z ORGANIZATOREM
    private static class MeetingWithOrganizerRowMapper implements RowMapper<Meeting> {
        @Override
        public Meeting mapRow(ResultSet rs, int rowNum) throws SQLException {
            Meeting meeting = new Meeting();

            // ✅ PODSTAWOWE INFORMACJE O SPOTKANIU
            meeting.setId(rs.getLong("id"));
            meeting.setTitle(rs.getString("title"));
            meeting.setDescription(rs.getString("description"));
            meeting.setAgenda(rs.getString("agenda"));

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

            // ✅ MAPOWANIE ENUMÓW Z OBSŁUGĄ BŁĘDÓW
            mapEnums(rs, meeting);

            // ✅ DATY
            if (rs.getTimestamp("start_date") != null) {
                meeting.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
            }
            if (rs.getTimestamp("end_date") != null) {
                meeting.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
            }

            meeting.setMaxParticipants(rs.getObject("max_participants") != null ?
                    rs.getInt("max_participants") : null);

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