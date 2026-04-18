package com.shiftmate.repository;

import com.shiftmate.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for {@link Restaurant} entities.
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /**
     * Finds a restaurant by its contact email address.
     *
     * @param email the restaurant's email address
     * @return an {@link Optional} containing the restaurant, or empty if not found
     */
    Optional<Restaurant> findByEmail(String email);

    /**
     * Checks whether a restaurant with the given email already exists.
     *
     * @param email the email address to check
     * @return {@code true} if a restaurant with that email exists
     */
    boolean existsByEmail(String email);
}
