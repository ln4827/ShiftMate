package com.shiftmate.acceptance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance tests for authentication and access control.
 *
 * Scenarios covered:
 *   - Unauthenticated access to any protected endpoint → 401
 *   - Wrong credentials → 401
 *   - Inactive account → 401
 *   - Employee accessing a manager-only endpoint → 403
 *   - Successful login and logout
 */
@DisplayName("Security — acceptance tests")
class SecurityAcceptanceTest extends AcceptanceTestBase {

    // ── Unauthenticated access ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/shifts without a session → 401")
    void shiftsEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/shifts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/time-off/my without a session → 401")
    void timeOffEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/time-off/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/swap-requests/my without a session → 401")
    void swapEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/swap-requests/my"))
                .andExpect(status().isUnauthorized());
    }

    // ── Bad credentials ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Login with wrong password → 401")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "WrongPassword!"}
                                """.formatted(employee.getEmail())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login with non-existent email → 401")
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nobody@test.hr", "password": "Password1!"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Inactive account ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Inactive employee cannot log in → 401")
    void login_inactiveEmployee_returns401() throws Exception {
        employee.setActive(false);
        employeeRepo.save(employee);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(employee.getEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    // ── Role-based access control ─────────────────────────────────────────────

    @Test
    @DisplayName("Employee accessing manager-only schedule endpoint → 403")
    void managerSchedule_asEmployee_returns403() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());
        mockMvc.perform(get("/api/shifts").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employee cannot approve a time-off request → 403")
    void approveTimeOff_asEmployee_returns403() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());
        mockMvc.perform(post("/api/time-off/999/approve").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employee cannot access the swap approval queue → 403")
    void swapApprovalQueue_asEmployee_returns403() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());
        mockMvc.perform(get("/api/swap-requests/pending").session(session))
                .andExpect(status().isForbidden());
    }

    // ── Successful auth ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Successful login returns employee info")
    void login_validCredentials_returnsEmployeeInfo() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(manager.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manager").value(true))
                .andExpect(jsonPath("$.email").value(manager.getEmail()))
                .andExpect(jsonPath("$.employeeId").isNumber());
    }

    @Test
    @DisplayName("After logout, the session is invalidated → 401 on next request")
    void logout_thenRequest_returns401() throws Exception {
        MockHttpSession session = loginAs(manager.getEmail());

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/shifts").session(session))
                .andExpect(status().isUnauthorized());
    }
}
