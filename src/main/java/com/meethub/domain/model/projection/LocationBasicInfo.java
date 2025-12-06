package com.meethub.domain.model.projection;

public interface LocationBasicInfo {
    Long getId();
    String getName();
    String getCity();
    String getAddress();
    String getType(); // lub com.meethub.domain.model.enums.LocationType jeśli chcesz enum
    String getVirtualMeetingUrl();
}
