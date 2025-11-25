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
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.response.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomMeetingRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String FIND_NEARBY_MEETINGS_SQL = """
        SELECT m.*, l.latitude, l.longitude,
               earth_distance(
                   ll_to_earth(:lat, :lng),
                   ll_to_earth(l.latitude, l.longitude)
               ) as distance
        FROM meetings m
        JOIN locations l ON m.location_id = l.id
        WHERE m.visibility = 'PUBLIC'
          AND m.status = 'CONFIRMED'
          AND m.start_date > CURRENT_TIMESTAMP
          AND earth_distance(
                  ll_to_earth(:lat, :lng),
                  ll_to_earth(l.latitude, l.longitude)
              ) < :radius
        ORDER BY distance, m.start_date
        LIMIT :limit
        """;

    // DODAJ TE METODY:

    public Page<Meeting> findFilteredMeetings(String search, String type, String status, Pageable pageable) {
        StringBuilder sql = new StringBuilder("""
            SELECT m.*, u.first_name, u.last_name, u.email 
            FROM meetings m
            LEFT JOIN users u ON m.organizer_id = u.id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        // Filtry
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (LOWER(m.title) LIKE LOWER(?) OR LOWER(m.description) LIKE LOWER(?))");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (type != null && !type.trim().isEmpty()) {
            sql.append(" AND m.type = ?");
            params.add(type);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND m.status = ?");
            params.add(status);
        }

        // Sortowanie
        sql.append(" ORDER BY m.start_date DESC");

        // Paginacja - najpierw count
        String countSql = "SELECT COUNT(*) FROM (" + sql.toString().replace("m.*, u.first_name, u.last_name, u.email", "1") + ") AS count_table";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // Paginacja - dane
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());

        List<Meeting> meetings = jdbcTemplate.query(sql.toString(), new MeetingRowMapper(), params.toArray());

        return new PageImpl<>(meetings, pageable, total != null ? total : 0);
    }

    public List<Meeting> findNearbyMeetings(double latitude, double longitude, double radius, int limit) {
        // Tymczasowa implementacja - zakładając, że nie masz jeszcze locations
        String sql = """
            SELECT m.*, u.first_name, u.last_name, u.email 
            FROM meetings m 
            LEFT JOIN users u ON m.organizer_id = u.id
            WHERE m.visibility = 'PUBLIC' 
            AND m.status = 'CONFIRMED' 
            AND m.start_date > CURRENT_TIMESTAMP
            ORDER BY m.start_date ASC 
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, new MeetingRowMapper(), limit);
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

        return jdbcTemplate.query(sql, new StatisticsRowMapper(), organizerId);
    }

    private static class MeetingRowMapper implements RowMapper<Meeting> {
        @Override
        public Meeting mapRow(ResultSet rs, int rowNum) throws SQLException {
            Meeting meeting = new Meeting();
            meeting.setId(rs.getLong("id"));
            meeting.setTitle(rs.getString("title"));
            meeting.setDescription(rs.getString("description"));
            meeting.setAgenda(rs.getString("agenda"));

            // Mapowanie enumów - obsługa nulli
            String type = rs.getString("type");
            if (type != null) {
                try {
                    meeting.setType(com.meethub.domain.model.enums.MeetingType.valueOf(type));
                } catch (IllegalArgumentException e) {
                    meeting.setType(MeetingType.ONLINE);
                }
            }

            String status = rs.getString("status");
            if (status != null) {
                try {
                    meeting.setStatus(com.meethub.domain.model.enums.MeetingStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    meeting.setStatus(com.meethub.domain.model.enums.MeetingStatus.PLANNED);
                }
            }

            String visibility = rs.getString("visibility");
            if (visibility != null) {
                try {
                    meeting.setVisibility(com.meethub.domain.model.enums.MeetingVisibility.valueOf(visibility));
                } catch (IllegalArgumentException e) {
                    meeting.setVisibility(com.meethub.domain.model.enums.MeetingVisibility.PRIVATE);
                }
            }

            meeting.setStartDate(rs.getTimestamp("start_date") != null ?
                    rs.getTimestamp("start_date").toLocalDateTime() : null);
            meeting.setEndDate(rs.getTimestamp("end_date") != null ?
                    rs.getTimestamp("end_date").toLocalDateTime() : null);
            meeting.setMaxParticipants(rs.getInt("max_participants"));
            meeting.setCreatedAt(rs.getTimestamp("created_at") != null ?
                    rs.getTimestamp("created_at").toLocalDateTime() : null);
            meeting.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                    rs.getTimestamp("updated_at").toLocalDateTime() : null);

            return meeting;
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