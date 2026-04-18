package com.shiftmate;

import com.shiftmate.dto.CreateEmployeeRequest;
import com.shiftmate.dto.EmployeeResponse;
import com.shiftmate.entity.Employee;
import com.shiftmate.entity.Restaurant;
import com.shiftmate.exception.BusinessRuleException;
import com.shiftmate.repository.EmployeeRepository;
import com.shiftmate.repository.EmployeeRoleRepository;
import com.shiftmate.repository.RestaurantRepository;
import com.shiftmate.repository.RoleRepository;
import com.shiftmate.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmployeeServiceImpl}.
 *
 * All dependencies are mocked — no database or Spring context required.
 * Tests verify business logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeRoleRepository employeeRoleRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private RoleRepository roleRepository;

    // Use real BCrypt so password hashing behaviour is tested accurately
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    // We need to inject the real passwordEncoder since @InjectMocks won't use the field above
    @BeforeEach
    void injectPasswordEncoder() throws Exception {
        var field = EmployeeServiceImpl.class.getDeclaredField("passwordEncoder");
        field.setAccessible(true);
        field.set(employeeService, passwordEncoder);
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    private Restaurant sampleRestaurant() {
        Restaurant r = new Restaurant();
        try {
            var idField = Restaurant.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(r, 1L);
        } catch (Exception ignored) {}
        r.setName("Test Restaurant");
        r.setEmail("test@restaurant.hr");
        r.setAddress("Test Street 1");
        r.setCity("Zagreb");
        r.setPhone("+385 1 000 0000");
        return r;
    }

    private Employee sampleEmployee(Restaurant restaurant) {
        return Employee.builder()
                .restaurant(restaurant)
                .firstName("Ana")
                .lastName("Kovač")
                .email("ana@dalmatino.hr")
                .passwordHash(passwordEncoder.encode("Password1!"))
                .isManager(true)
                .build();
    }

    // =========================================================================
    // create()
    // =========================================================================

    @Test
    @DisplayName("create() — saves employee with hashed password and returns DTO")
    void create_validRequest_savesAndReturnsResponse() {
        // Arrange
        Restaurant restaurant = sampleRestaurant();
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Ana", "Kovač", "ana@dalmatino.hr",
                "Password1!", false, List.of());

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(employeeRepository.existsByEmail("ana@dalmatino.hr")).thenReturn(false);

        Employee savedEmployee = sampleEmployee(restaurant);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);
        when(employeeRepository.findByIdWithRoles(any())).thenReturn(Optional.of(savedEmployee));

        // Act
        EmployeeResponse response = employeeService.create(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("Ana");
        assertThat(response.getLastName()).isEqualTo("Kovač");
        assertThat(response.getEmail()).isEqualTo("ana@dalmatino.hr");

        // Verify password was hashed — the response should never expose the raw hash
        verify(employeeRepository).save(argThat(e ->
                e.getPasswordHash() != null
                && !e.getPasswordHash().equals("Password1!")
                && e.getPasswordHash().startsWith("$2a$")
        ));
    }

    @Test
    @DisplayName("create() — throws BusinessRuleException when email already exists")
    void create_duplicateEmail_throwsBusinessRuleException() {
        // Arrange
        Restaurant restaurant = sampleRestaurant();
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Ana", "Kovač", "ana@dalmatino.hr",
                "Password1!", false, List.of());

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(employeeRepository.existsByEmail("ana@dalmatino.hr")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> employeeService.create(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ana@dalmatino.hr");

        verify(employeeRepository, never()).save(any());
    }

    // =========================================================================
    // deactivate()
    // =========================================================================

    @Test
    @DisplayName("deactivate() — sets isActive=false for an active employee")
    void deactivate_activeEmployee_setsInactive() {
        // Arrange
        Restaurant restaurant = sampleRestaurant();
        Employee employee = sampleEmployee(restaurant);
        employee.setActive(true);

        when(employeeRepository.findByIdWithRoles(42L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        // Act
        employeeService.deactivate(1L, 42L);

        // Assert
        assertThat(employee.isActive()).isFalse();
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("deactivate() — throws BusinessRuleException if already inactive")
    void deactivate_alreadyInactive_throwsException() {
        // Arrange
        Restaurant restaurant = sampleRestaurant();
        Employee employee = sampleEmployee(restaurant);
        employee.setActive(false);

        when(employeeRepository.findByIdWithRoles(42L)).thenReturn(Optional.of(employee));

        // Act + Assert
        assertThatThrownBy(() -> employeeService.deactivate(1L, 42L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already inactive");

        verify(employeeRepository, never()).save(any());
    }
}
