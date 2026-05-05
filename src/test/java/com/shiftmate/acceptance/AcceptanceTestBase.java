package com.shiftmate.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftmate.entity.Department;
import com.shiftmate.entity.Employee;
import com.shiftmate.entity.Restaurant;
import com.shiftmate.entity.Role;
import com.shiftmate.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for acceptance tests. Each test class gets a fresh database state:
 * @BeforeEach inserts committed test data, @AfterEach deletes it in FK-safe order.
 *
 * A BCrypt strength-4 encoder is used instead of strength-12 to keep login fast.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AcceptanceTestBase {

    @TestConfiguration
    static class FastPasswordEncoderConfig {
        @Bean
        @Primary
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }

    // ── Shared infrastructure ────────────────────────────────────────────────
    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected PasswordEncoder passwordEncoder;

    // ── Repositories ─────────────────────────────────────────────────────────
    @Autowired protected RestaurantRepository restaurantRepo;
    @Autowired protected EmployeeRepository employeeRepo;
    @Autowired protected DepartmentRepository departmentRepo;
    @Autowired protected RoleRepository roleRepo;
    @Autowired protected ShiftRepository shiftRepo;
    @Autowired protected ShiftAssignmentRepository assignmentRepo;
    @Autowired protected TimeOffRequestRepository timeOffRepo;
    @Autowired protected SwapRequestRepository swapRepo;

    // ── Common test data ─────────────────────────────────────────────────────
    protected static final String PASSWORD = "Password1!";

    protected Restaurant restaurant;
    protected Employee manager;
    protected Employee employee;
    protected Department department;
    protected Role role;

    @BeforeEach
    void setUpBase() {
        restaurant = restaurantRepo.save(Restaurant.builder()
                .name("Test Resto").address("Test Street 1").city("Zagreb")
                .phone("+385 1 000 0000").email("test@resto.hr")
                .build());

        department = new Department();
        department.setRestaurant(restaurant);
        department.setName("Kitchen");
        department = departmentRepo.save(department);

        role = new Role();
        role.setRestaurant(restaurant);
        role.setName("Cook");
        role = roleRepo.save(role);

        String hash = passwordEncoder.encode(PASSWORD);

        manager = Employee.builder()
                .restaurant(restaurant).firstName("Test").lastName("Manager")
                .email("manager@test.hr").passwordHash(hash).isManager(true)
                .build();
        manager.setActive(true);
        manager = employeeRepo.save(manager);

        employee = Employee.builder()
                .restaurant(restaurant).firstName("Test").lastName("Employee")
                .email("employee@test.hr").passwordHash(hash).isManager(false)
                .build();
        employee.setActive(true);
        employee = employeeRepo.save(employee);
    }

    @AfterEach
    void tearDownBase() {
        // Delete in FK-safe order (RESTRICT constraints block deletes otherwise)
        swapRepo.deleteAll();
        assignmentRepo.deleteAll();
        shiftRepo.deleteAll();
        timeOffRepo.deleteAll();
        // Restaurant cascade deletes employees, departments, roles, notifications
        restaurantRepo.deleteAll();
    }

    /**
     * Logs in via the real /api/auth/login endpoint and returns the resulting session.
     * Pass the returned session to every subsequent MockMvc request via .session(session).
     */
    protected MockHttpSession loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
