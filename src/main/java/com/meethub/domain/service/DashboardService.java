//package com.meethub.domain.service;
//
//import com.meethub.domain.model.response.DashboardStatsResponse;
//import java.util.Optional;
//
//public interface DashboardService {
//    DashboardStatsResponse getUserDashboardStats(Long userId);
//}




package com.meethub.domain.service;

import com.meethub.domain.model.response.DashboardStatsResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DashboardService {

    DashboardStatsResponse getUserDashboardStats(
            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId
    );
}
