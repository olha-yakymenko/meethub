package com.meethub.domain.model.dto;

import java.math.BigDecimal;

    public class OrganizerReportStats {

        private final Long totalMeetings;
        private final BigDecimal averageAttendanceRate;
        private final Long totalParticipants;
        private final Long totalAttended;

        public OrganizerReportStats(
                Long totalMeetings,
                BigDecimal averageAttendanceRate,
                Long totalParticipants,
                Long totalAttended) {
            this.totalMeetings = totalMeetings;
            this.averageAttendanceRate = averageAttendanceRate;
            this.totalParticipants = totalParticipants;
            this.totalAttended = totalAttended;
        }

        public Long getTotalMeetings() { return totalMeetings; }
        public BigDecimal getAverageAttendanceRate() { return averageAttendanceRate; }
        public Long getTotalParticipants() { return totalParticipants; }
        public Long getTotalAttended() { return totalAttended; }
    }
