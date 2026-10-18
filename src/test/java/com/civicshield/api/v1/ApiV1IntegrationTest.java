package com.civicshield.api.v1;

import com.civicshield.api.v1.auth.TokenService;
import com.civicshield.model.User;
import com.civicshield.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ApiV1IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    private String adminToken;
    private String authorityToken;
    private String hospitalToken;

    @BeforeEach
    void setUp() {
        User admin = userRepository.findByEmailIgnoreCase("admin@civicshield.in").orElseThrow();
        User authority = userRepository.findByEmailIgnoreCase("authority@civicshield.in").orElseThrow();
        User hospital = userRepository.findByEmailIgnoreCase("hospital@civicshield.in").orElseThrow();

        adminToken = tokenService.createToken(admin).rawToken();
        authorityToken = tokenService.createToken(authority).rawToken();
        hospitalToken = tokenService.createToken(hospital).rawToken();
    }

    @Test
    void testSystemStatusIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("civicshield-backend"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.riskEngine").value("JAVA_CASCADE_ENGINE"));
    }

    @Test
    void testLoginSuccessAndFailure() throws Exception {
        // Success
        String validPayload = "{\"email\":\"admin@civicshield.in\",\"password\":\"Admin@123\"}";
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@civicshield.in"));

        // Failure
        String invalidPayload = "{\"email\":\"admin@civicshield.in\",\"password\":\"WrongPassword\"}";
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void testAuthMeWithBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@civicshield.in"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void testDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataProvenance").value("SIMULATED"))
                .andExpect(jsonPath("$.activeIncidents").isNumber())
                .andExpect(jsonPath("$.areas").isArray())
                .andExpect(jsonPath("$.riskDistribution").isMap());
    }

    @Test
    void testDashboardTrendAndActivity() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/trend?days=7")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray());

        mockMvc.perform(get("/api/v1/dashboard/activity?limit=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testIncidentsEndpoints() throws Exception {
        // List
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());

        // Create
        String newIncident = "{\"type\":\"WATERLOGGING\",\"location\":\"Greenways Road, Adyar\",\"latitude\":13.018,\"longitude\":80.252,\"severity\":\"HIGH\"}";
        String response = mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + authorityToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newIncident))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.latestRisk").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        long id = node.get("id").asLong();

        // Get by ID
        mockMvc.perform(get("/api/v1/incidents/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // Update status
        mockMvc.perform(patch("/api/v1/incidents/" + id + "/status")
                        .header("Authorization", "Bearer " + authorityToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Cascade risk
        mockMvc.perform(get("/api/v1/incidents/" + id + "/cascade")
                        .header("Authorization", "Bearer " + authorityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome.riskScore").isNumber())
                .andExpect(jsonPath("$.outcome.nodes").isArray());
    }

    @Test
    void testAlertsAndRecommendations() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/counts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ACTIVE").isNumber());

        mockMvc.perform(get("/api/v1/recommendations/counts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PENDING").isNumber());
    }

    @Test
    void testSimulations() throws Exception {
        mockMvc.perform(get("/api/v1/simulations/parameters")
                        .header("Authorization", "Bearer " + authorityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parameters").isArray())
                .andExpect(jsonPath("$.interventions").isArray())
                .andExpect(jsonPath("$.zones").isArray());

        String simReq = "{\"zone\":\"Velachery\",\"rainfall\":110,\"traffic\":75,\"drainage\":35,\"pumps\":true,\"diversion\":false,\"greenCorridor\":false,\"hospitalSurge\":false}";
        mockMvc.perform(post("/api/v1/simulations/run")
                        .header("Authorization", "Bearer " + authorityToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataProvenance").value("HYPOTHETICAL"))
                .andExpect(jsonPath("$.baseline.riskScore").isNumber())
                .andExpect(jsonPath("$.scenario.riskScore").isNumber())
                .andExpect(jsonPath("$.curve.rainfall").isArray());
    }

    @Test
    void testZonesAndMap() throws Exception {
        mockMvc.perform(get("/api/v1/zones")
                        .header("Authorization", "Bearer " + hospitalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/map/incidents")
                        .header("Authorization", "Bearer " + hospitalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary")
                        .header("Authorization", "Bearer " + authorityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataProvenance").value("SIMULATED"))
                .andExpect(jsonPath("$.byType").isMap());

        mockMvc.perform(get("/api/v1/reports/incidents.csv")
                        .header("Authorization", "Bearer " + authorityToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Incident ID,Type,Location")));
    }

    @Test
    void testUsersAdminAccess() throws Exception {
        // Admin can list users
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Hospital role cannot access /api/v1/users (403 Forbidden)
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + hospitalToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }
}
