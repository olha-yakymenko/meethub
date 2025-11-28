//package com.meethub.controller.web;
//
//import com.meethub.domain.model.enums.LocationType;
//import com.meethub.domain.model.response.LocationListResponse;
//import com.meethub.domain.model.response.LocationResponse;
//import com.meethub.domain.service.LocationService;
//import com.meethub.security.JwtAuthenticationFilter;
//import com.meethub.security.JwtUtil;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.FilterType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(controllers = LocationController.class,
//        excludeFilters = @ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                classes = {JwtAuthenticationFilter.class}
//        )
//)
//class LocationControllerSecurityTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private LocationService locationService;
//
//    @MockBean
//    private JwtUtil jwtUtil;
//
//    @Test
//    @WithMockUser(username = "test@example.com", roles = {"USER"})
//    void shouldReturnLocationsPage() throws Exception {
//        // Przygotuj mockowane dane
//        LocationResponse location = LocationResponse.builder()
//                .id(1L)
//                .name("Test Location")
//                .type(LocationType.PHYSICAL)
//                .address("Test Address 123")
//                .city("Warsaw")
//                .country("Poland")
//                .latitude(new BigDecimal("52.2297"))
//                .longitude(new BigDecimal("21.0122"))
//                .build();
//
//        LocationListResponse response = LocationListResponse.builder()
//                .locations(List.of(location))
//                .currentPage(0)
//                .totalPages(1)
//                .totalItems(1L)
//                .hasNext(false)
//                .hasPrevious(false)
//                .build();
//
//        // Mockuj serwis
//        when(locationService.searchLocations(any())).thenReturn(response);
//
//        mockMvc.perform(get("/locations"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("locations/list"))
//                .andExpect(model().attributeExists("locations"))
//                .andExpect(model().attributeExists("totalItems"));
//    }
//
//    @Test
//    @WithMockUser(username = "test@example.com", roles = {"USER"})
//    void shouldReturnEmptyLocationsPage() throws Exception {
//        // Mockuj pustą odpowiedź
//        LocationListResponse emptyResponse = LocationListResponse.builder()
//                .locations(List.of())
//                .currentPage(0)
//                .totalPages(0)
//                .totalItems(0L)
//                .hasNext(false)
//                .hasPrevious(false)
//                .build();
//
//        when(locationService.searchLocations(any())).thenReturn(emptyResponse);
//
//        mockMvc.perform(get("/locations"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("locations/list"))
//                .andExpect(model().attribute("totalItems", 0L));
//    }
//}