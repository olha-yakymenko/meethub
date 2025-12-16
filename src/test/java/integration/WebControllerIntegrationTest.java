//package integration;
//
//import com.meethub.MeetHubApplication;
//import com.meethub.domain.service.MeetingService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
//
//@SpringBootTest(classes = MeetHubApplication.class)
//@AutoConfigureMockMvc
//class WebControlerIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private MeetingService meetingService; // mockujemy serwis
//
//    @Test
//    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
//    void joinMeeting_ShouldRedirectToDetails_WhenAuthenticated() throws Exception {
//        mockMvc.perform(post("/meetings/1/join"))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/meetings/1"));
//    }
//
//}
