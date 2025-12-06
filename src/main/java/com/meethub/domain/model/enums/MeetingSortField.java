package com.meethub.domain.model.enums;

public enum MeetingSortField {
    START_DATE_DESC("startDate", false),
    START_DATE_ASC("startDate", true),
    TITLE_ASC("title", true),
    TITLE_DESC("title", false),
    CREATED_AT_DESC("createdAt", false),
    PARTICIPANTS_DESC("confirmedParticipantsCount", false);

    private final String fieldName;
    private final boolean ascending;

    MeetingSortField(String fieldName, boolean ascending) {
        this.fieldName = fieldName;
        this.ascending = ascending;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isAscending() {
        return ascending;
    }

    public static MeetingSortField fromString(String value) {
        if (value == null || value.isEmpty()) {
            return START_DATE_DESC;
        }
        try {
            return MeetingSortField.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return START_DATE_DESC;
        }
    }
}