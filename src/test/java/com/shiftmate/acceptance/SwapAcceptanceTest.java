package com.shiftmate.acceptance;

import com.shiftmate.entity.Employee;
import com.shiftmate.entity.Shift;
import com.shiftmate.entity.ShiftAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance tests for the shift swap workflow.
 *
 * Scenarios covered:
 *   - Employee requests a swap → 201, status PENDING
 *   - Manager approves → status becomes APPROVED
 *   - Manager rejects → status becomes REJECTED
 *   - Employee sees their swaps in /my list
 *   - Only manager can access the approval queue
 */
@DisplayName("Shift Swap — acceptance tests")
class SwapAcceptanceTest extends AcceptanceTestBase {

    private Employee secondEmployee;
    private ShiftAssignment assignmentA;  // belongs to employee
    private ShiftAssignment assignmentB;  // belongs to secondEmployee

    @BeforeEach
    void setUpSwapData() {
        // Create a second employee to swap with
        secondEmployee = Employee.builder()
                .restaurant(restaurant).firstName("Second").lastName("Employee")
                .email("employee2@test.hr")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .isManager(false)
                .build();
        secondEmployee.setActive(true);
        secondEmployee = employeeRepo.save(secondEmployee);

        // Two separate shifts on different days
        Shift shiftA = shiftRepo.save(Shift.builder()
                .department(department)
                .shiftDate(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .isPublished(true)
                .createdBy(manager)
                .build());

        Shift shiftB = shiftRepo.save(Shift.builder()
                .department(department)
                .shiftDate(LocalDate.now().plusDays(8))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .isPublished(true)
                .createdBy(manager)
                .build());

        assignmentA = assignmentRepo.save(ShiftAssignment.builder()
                .shift(shiftA).employee(employee).role(role).build());

        assignmentB = assignmentRepo.save(ShiftAssignment.builder()
                .shift(shiftB).employee(secondEmployee).role(role).build());
    }

    private String swapBody() {
        return """
                {
                    "requesterAssignmentId": %d,
                    "targetAssignmentId":    %d
                }
                """.formatted(assignmentA.getId(), assignmentB.getId());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Employee requests a swap → 201 with PENDING status")
    void request_asEmployee_returns201AndPending() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());

        mockMvc.perform(post("/api/swap-requests")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swapBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requesterName").isNotEmpty())
                .andExpect(jsonPath("$.targetName").isNotEmpty());
    }

    @Test
    @DisplayName("After requesting, swap appears in the employee's own list")
    void request_thenGetMy_returnsTheSwap() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());

        mockMvc.perform(post("/api/swap-requests")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swapBody()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/swap-requests/my").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Manager sees the pending swap in the approval queue")
    void request_managerSeesItInPendingQueue() throws Exception {
        MockHttpSession empSession = loginAs(employee.getEmail());
        mockMvc.perform(post("/api/swap-requests")
                        .session(empSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swapBody()))
                .andExpect(status().isCreated());

        MockHttpSession mgrSession = loginAs(manager.getEmail());
        mockMvc.perform(get("/api/swap-requests/pending").session(mgrSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Manager approves swap → status becomes APPROVED")
    void approve_asManager_changesStatusToApproved() throws Exception {
        MockHttpSession empSession = loginAs(employee.getEmail());
        String createResponse = mockMvc.perform(post("/api/swap-requests")
                        .session(empSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swapBody()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long swapId = objectMapper.readTree(createResponse).get("id").asLong();

        MockHttpSession mgrSession = loginAs(manager.getEmail());
        mockMvc.perform(post("/api/swap-requests/{id}/approve", swapId).session(mgrSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Manager rejects swap → status becomes REJECTED")
    void reject_asManager_changesStatusToRejected() throws Exception {
        MockHttpSession empSession = loginAs(employee.getEmail());
        String createResponse = mockMvc.perform(post("/api/swap-requests")
                        .session(empSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swapBody()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long swapId = objectMapper.readTree(createResponse).get("id").asLong();

        MockHttpSession mgrSession = loginAs(manager.getEmail());
        mockMvc.perform(post("/api/swap-requests/{id}/reject", swapId).session(mgrSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // ── Role protection ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Employee cannot access the manager approval queue → 403")
    void pendingQueue_asEmployee_returns403() throws Exception {
        MockHttpSession session = loginAs(employee.getEmail());
        mockMvc.perform(get("/api/swap-requests/pending").session(session))
                .andExpect(status().isForbidden());
    }
}
