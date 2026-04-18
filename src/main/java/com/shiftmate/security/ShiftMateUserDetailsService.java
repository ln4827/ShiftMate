package com.shiftmate.security;

import com.shiftmate.entity.Employee;
import com.shiftmate.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads an {@link Employee} from the database by email address and wraps it in a
 * {@link ShiftMateUserDetails} for Spring Security to use during authentication.
 *
 * <p>The restaurant association is force-initialised while still inside the transaction
 * so that {@code principal.getRestaurantId()} is always available in controllers
 * without triggering a lazy-load outside a persistence context.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftMateUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    /**
     * Locates an employee by their email address and returns a populated
     * {@link ShiftMateUserDetails} object for Spring Security.
     *
     * @param email the email address submitted during login
     * @return the authenticated user details
     * @throws UsernameNotFoundException if no active account exists for the given email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login attempt with unknown email: {}", email);
                    return new UsernameNotFoundException(
                            "No account found for email: " + email);
                });

        employee.getRestaurant().getId();

        log.debug("Loaded user: email={} manager={} active={}",
                employee.getEmail(), employee.isManager(), employee.isActive());

        return new ShiftMateUserDetails(employee);
    }
}
