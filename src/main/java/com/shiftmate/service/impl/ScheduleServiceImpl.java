package com.shiftmate.service.impl;

import com.shiftmate.dto.ScheduleResponse;
import com.shiftmate.dto.ShiftResponse;
import com.shiftmate.exception.ResourceNotFoundException;
import com.shiftmate.repository.RestaurantRepository;
import com.shiftmate.service.ScheduleService;
import com.shiftmate.service.ShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Default implementation of {@link ScheduleService}.
 *
 * <p>Delegates the actual data fetching to {@link ShiftService#getWeeklyScheduleForManager},
 * which uses JOIN FETCH queries to load shifts with their assignments, coverage
 * requirements, and related entities in two SQL round-trips — no N+1 problem.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ShiftService shiftService;
    private final RestaurantRepository restaurantRepository;

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse getWeeklySchedule(Long restaurantId, String week) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }

        LocalDate monday = parseWeek(week);

        log.debug("Building schedule for restaurant={} week={} (from {})",
                restaurantId, week, monday);

        List<ShiftResponse> shifts =
                shiftService.getWeeklyScheduleForManager(restaurantId, monday);

        log.debug("Schedule built: {} shifts, allCoverageMet={}",
                shifts.size(), shifts.stream().allMatch(ShiftResponse::isCoverageMet));

        return ScheduleResponse.of(week, shifts);
    }

    /**
     * Parses {@code "YYYY-Www"} into the Monday of that ISO week.
     * Appending {@code "-1"} converts it to an ISO week-date string
     * that {@link DateTimeFormatter#ISO_WEEK_DATE} can parse directly.
     */
    private LocalDate parseWeek(String week) {
        try {
            return LocalDate.parse(week + "-1", DateTimeFormatter.ISO_WEEK_DATE);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid week format '" + week + "'. Expected ISO format like '2026-W18'.");
        }
    }
}
