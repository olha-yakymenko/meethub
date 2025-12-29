
package com.meethub.domain.service;

import com.meethub.domain.model.response.DashboardStatsResponse;

public interface DashboardService {

    DashboardStatsResponse getUserDashboardStats(Long userId);
}