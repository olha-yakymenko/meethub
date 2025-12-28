package com.meethub.domain.model.id;

import java.io.Serializable;
import java.util.Objects;

public class MeetingMarkId implements Serializable {
    private Long user;
    private Long meeting;

    public MeetingMarkId() {}

    public MeetingMarkId(Long user, Long meeting) {
        this.user = user;
        this.meeting = meeting;
    }

    // Getters & Setters
    public Long getUser() { return user; }
    public void setUser(Long user) { this.user = user; }

    public Long getMeeting() { return meeting; }
    public void setMeeting(Long meeting) { this.meeting = meeting; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeetingMarkId that = (MeetingMarkId) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(meeting, that.meeting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, meeting);
    }
}