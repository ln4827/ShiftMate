package com.shiftmate.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance tests for the time-off request workflow.
 *
 * Scenarios covered:
 *   - Employee submits a request → 201, status PENDING
 *   - Manager sees it in the approval queue
 *   - Manager approves → status becomes APPROVED
 *   - Manager rejects → status becomes REJECTED
 */
@DisplayName("Time Off — acceptance tests")
class TimeOffAcceptanceTest extends AcceptanceTestBase {

    private static final String SUBMIT_BODY = """
            {
                "startDate": "2027-06-01",
                "endDate":   "2027-06-03",
                "reason":    "Family vacation"
            }
            """;

    // ── Submit ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Employee submits a time-off request → 201 with PENDING status")
    void submit_asEmployee_returns201AndPendingStatus() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());

        mockMvc.perform(post("/api/time-off")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBMIT_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("Family vacation"))
                .andExpect(jsonPath("$.startDate").value("2027-06-01"))
                .andExpect(jsonPath("$.endDate").value("2027-06-03"));
    }

    @Test
    @DisplayName("Employee's submitted request appears in their own request list")
    void submit_thenGetMy_returnsTheRequest() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());

        mockMvc.perform(post("/api/time-off")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBMIT_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/time-off/my").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Manager sees a pending request in the approval queue")
    void submit_managerSeesItInPendingQueue() throws Exception {
        MockHttpSession empSession = loginAs(employee.getEmail());
        mockMvc.perform(post("/api/time-off")
                        .session(empSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBMIT_BODY))
                .andExpect(status().isCreated());

        MockHttpSession mgrSession = loginAs(manager.getEmail());
        mockMvc.perform(get("/api/time-off/pending").session(mgrSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].employeeName").isNotEmpty());
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Manager approves a pending request → status becomes APPROVED")
    void approve_asManager_changesStatusToApproved() throws Exception {
        MockHttpSession empSession = loginAs(employee.getEmail());
        String createResponse = mockMvc.perform(post("/api/time-off")
                        .session(empSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBMIT_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long requestId = objectMapper.readTree(createResponse).get("id").asLong();

        MockHttpSession mgrSession = loginAs(manager.getEmail());
        mockMvc.perform(post("/api/time-off/{id}/approve", requestId).session(mgrSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.resolvedByName").isNotEmpty());
    }

    @Test
    @DisplayName("After approval, the request no longer appears in the pending queue")
    void approve_requestRemovedFromPendingQueue() throws Exception {
        MockHttpSession empSession = loginAs(employee.getEmail());
        String createResponse = mockMvc.perform(post("/api/time-off")
                        .session(empSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBMIT_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long requestId = objectMapper.readTree(createResponse).get("id").asLong();

        MockHttpSession mgrSession = loginAs(manager.getEmail());
        mockMvc.perform(post("/api/time-off/{id}/approve", requestId).session(mgrSession))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/time-off/pending").session(mgrSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Manager rejects a pending request → status becomes REJECTED")
    void reject_asManager_changesStatusToRejected() throws Exception {
        MockHttpSession empSession = loginAs(employee.getEmail());
        String createResponse = mockMvc.perform(post("/api/time-off")
                        .session(empSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBMIT_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long requestId = objectMapper.readTree(createResponse).get("id").asLong();

        MockHttpSession mgrSession = loginAs(manager.getEmail());
        mockMvc.perform(post("/api/time-off/{id}/reject", requestId).session(mgrSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // ── Role protection ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Employee cannot access the manager approval queue → 403")
    void pendingQueue_asEmployee_returns403() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());
        mockMvc.perform(get("/api/time-off/pending").session(session))
                .andExpect(status().isForbidden());
    }
}
