package com.meethub.domain.model.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class LocationListResponse {
    private List<LocationResponse> locations;
    private int currentPage;
    private int totalPages;
    private long totalItems;
    private boolean hasNext;
    private boolean hasPrevious;
}