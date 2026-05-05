package com.shiftmate.service.impl;

import com.shiftmate.dto.AvailabilityResponse;
import com.shiftmate.dto.SetAvailabilityRequest;
import com.shiftmate.entity.Availability;
import com.shiftmate.entity.Employee;
import com.shiftmate.exception.ResourceNotFoundException;
import com.shiftmate.repository.AvailabilityRepository;
import com.shiftmate.repository.EmployeeRepository;
import com.shiftmate.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailability(Long restaurantId, Long employeeId) {
        resolveEmployee(restaurantId, employeeId);
        return availabilityRepository.findByEmployeeId(employeeId)
                .stream()
                .map(AvailabilityResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<AvailabilityResponse> setAvailability(Long restaurantId, Long employeeId,
                                                      SetAvailabilityRequest request) {
        Employee employee = resolveEmployee(restaurantId, employeeId);
        availabilityRepository.deleteAllByEmployeeId(employeeId);

        List<Availability> windows = request.getWindows().stream()
                .map(w -> Availability.builder()
                        .employee(employee)
                        .dayOfWeek(w.getDayOfWeek())
                        .startTime(w.getStartTime())
                        .endTime(w.getEndTime())
                        .build())
                .toList();

        return availabilityRepository.saveAll(windows)
                .stream()
                .map(AvailabilityResponse::from)
                .toList();
    }

    private Employee resolveEmployee(Long restaurantId, Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + employeeId));
        if (!employee.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        }
        return employee;
    }
}
