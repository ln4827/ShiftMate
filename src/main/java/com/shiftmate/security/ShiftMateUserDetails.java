package com.shiftmate.security;

import com.shiftmate.entity.Employee;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security {@link UserDetails} wrapper around an authenticated {@link Employee}.
 *
 * <p>Stored in the security context after successful login and injected into
 * controllers via {@code @AuthenticationPrincipal}. Carries the {@code restaurantId}
 * so controllers can scope every query to the correct tenant without an extra
 * database round-trip.
 *
 * <p>Managers receive both {@code ROLE_MANAGER} and {@code ROLE_EMPLOYEE} authorities
 * so they can access all employee-accessible endpoints in addition to manager-only ones.
 */
@Getter
public class ShiftMateUserDetails implements UserDetails {

    private final Long employeeId;
    private final Long restaurantId;
    private final String email;
    private final String passwordHash;
    private final boolean isManager;
    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructs a {@code ShiftMateUserDetails} from the given employee. The
     * restaurant association must be initialised before this constructor is called
     * so that {@link #getRestaurantId()} is available outside a transaction.
     *
     * @param employee the authenticated employee with an initialised restaurant
     */
    public ShiftMateUserDetails(Employee employee) {
        this.employeeId   = employee.getId();
        this.restaurantId = employee.getRestaurant().getId();
        this.email        = employee.getEmail();
        this.passwordHash = employee.getPasswordHash();
        this.isManager    = employee.isManager();
        this.isActive     = employee.isActive();

        this.authorities = isManager
                ? List.of(new SimpleGrantedAuthority("ROLE_MANAGER"),
                          new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
                : List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
