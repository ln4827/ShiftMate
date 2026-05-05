package com.shiftmate.acceptance;

import com.shiftmate.entity.Shift;
import com.shiftmate.entity.ShiftAssignment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance tests for shift management workflow.
 *
 * Scenarios covered:
 *   - Manager creates a shift → draft state
 *   - Manager publishes a shift → visible to employees
 *   - Manager unpublishes and deletes a shift that has assignments → no error
 *   - Employee cannot create or delete shifts → 403
 */
@DisplayName("Shift Management — acceptance tests")
class ShiftAcceptanceTest extends AcceptanceTestBase {

    private String shiftBody() {
        return """
                {
                    "departmentId": %d,
                    "shiftDate":    "%s",
                    "startTime":    "09:00",
                    "endTime":      "17:00"
                }
                """.formatted(department.getId(), LocalDate.now().plusDays(7));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Manager creates a shift → 201, shift is in draft state")
    void create_asManager_returns201InDraftState() throws Exception {
        MockHttpSession session = loginAs(manager.getEmail());

        mockMvc.perform(post("/api/shifts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shiftBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.departmentId").value(department.getId()))
                .andExpect(jsonPath("$.startTime").value("09:00:00"));
    }

    @Test
    @DisplayName("Employee cannot create shifts → 403")
    void create_asEmployee_returns403() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());

        mockMvc.perform(post("/api/shifts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shiftBody()))
                .andExpect(status().isForbidden());
    }

    // ── Publish ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Manager publishes a shift → published flag becomes true")
    void publish_asManager_shiftBecomesPublished() throws Exception {
        MockHttpSession session = loginAs(manager.getEmail());

        String createResponse = mockMvc.perform(post("/api/shifts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shiftBody()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long shiftId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(post("/api/shifts/{id}/publish", shiftId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    // ── Unpublish + Delete ────────────────────────────────────────────────────

    @Test
    @DisplayName("Manager unpublishes then deletes a shift with assignments → 204")
    void unpublishThenDelete_withAssignment_returns204() throws Exception {
        // Set up: create shift + assignment directly via repository
        Shift shift = shiftRepo.save(Shift.builder()
                .department(department)
                .shiftDate(LocalDate.now().plusDays(14))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(18, 0))
                .isPublished(true)
                .createdBy(manager)
                .build());

        assignmentRepo.save(ShiftAssignment.builder()
                .shift(shift)
                .employee(employee)
                .role(role)
                .build());

        MockHttpSession session = loginAs(manager.getEmail());

        // Unpublish
        mockMvc.perform(post("/api/shifts/{id}/unpublish", shift.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));

        // Delete — this should succeed (the bug we fixed: FK violation from swap_request)
        mockMvc.perform(delete("/api/shifts/{id}", shift.getId()).session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deleting a published shift with assignments → 409/400 with clear error")
    void delete_publishedShiftWithAssignment_returnsError() throws Exception {
        Shift shift = shiftRepo.save(Shift.builder()
                .department(department)
                .shiftDate(LocalDate.now().plusDays(21))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(18, 0))
                .isPublished(true)
                .createdBy(manager)
                .build());

        assignmentRepo.save(ShiftAssignment.builder()
                .shift(shift).employee(employee).role(role).build());

        MockHttpSession session = loginAs(manager.getEmail());

        // Should be blocked — shift is still published
        mockMvc.perform(delete("/api/shifts/{id}", shift.getId()).session(session))
                .andExpect(status().is4xxClientError());
    }

    // ── Employee schedule visibility ──────────────────────────────────────────

    @Test
    @DisplayName("Employee sees published shifts in their schedule")
    void publishedShift_appearsInEmployeeSchedule() throws Exception {
        Shift shift = shiftRepo.save(Shift.builder()
                .department(department)
                .shiftDate(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .isPublished(true)
                .createdBy(manager)
                .build());

        assignmentRepo.save(ShiftAssignment.builder()
                .shift(shift).employee(employee).role(role).build());

        MockHttpSession session = loginAs(employee.getEmail());

        mockMvc.perform(get("/api/shifts/my")
                        .param("weekStart", LocalDate.now().plusDays(7).toString())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Draft shift is not visible to employees")
    void draftShift_notVisibleToEmployee() throws Exception {
        Shift shift = shiftRepo.save(Shift.builder()
                .department(department)
                .shiftDate(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .isPublished(false)
                .createdBy(manager)
                .build());

        assignmentRepo.save(ShiftAssignment.builder()
                .shift(shift).employee(employee).role(role).build());

        MockHttpSession session = loginAs(employee.getEmail());

        mockMvc.perform(get("/api/shifts/my")
                        .param("weekStart", LocalDate.now().plusDays(7).toString())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
