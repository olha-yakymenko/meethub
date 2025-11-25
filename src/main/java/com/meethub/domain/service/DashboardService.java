package com.meethub.domain.service;

import com.meethub.domain.model.response.DashboardStatsResponse;
import java.util.Optional;

public interface DashboardService {
    DashboardStatsResponse getUserDashboardStats(Long userId);
}