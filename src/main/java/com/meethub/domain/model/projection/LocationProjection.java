package com.meethub.domain.model.projection;

public interface LocationProjection {
    Long getId();
    String getName();
    String getAddress();
    String getCity();
    String getCountry();
    String getType();
    String getVirtualMeetingUrl();
    String getAccessCode();
    String getTimezone();
}