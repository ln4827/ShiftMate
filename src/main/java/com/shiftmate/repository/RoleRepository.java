package com.shiftmate.repository;

import com.shiftmate.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Role} entities.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Returns all roles defined for a restaurant.
     *
     * @param restaurantId the restaurant's ID
     * @return list of roles, may be empty
     */
    List<Role> findByRestaurantId(Long restaurantId);

    /**
     * Finds a role by restaurant and name (case-sensitive).
     *
     * @param restaurantId the restaurant's ID
     * @param name         the role name to search for
     * @return an {@link Optional} containing the role, or empty if not found
     */
    Optional<Role> findByRestaurantIdAndName(Long restaurantId, String name);

    /**
     * Checks whether a role with the given name already exists in a restaurant.
     *
     * @param restaurantId the restaurant's ID
     * @param name         the role name to check
     * @return {@code true} if the role already exists
     */
    boolean existsByRestaurantIdAndName(Long restaurantId, String name);
}
