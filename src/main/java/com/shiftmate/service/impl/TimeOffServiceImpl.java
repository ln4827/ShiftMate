package com.shiftmate.service.impl;

import com.shiftmate.dto.CreateTimeOffRequest;
import com.shiftmate.dto.TimeOffResponse;
import com.shiftmate.entity.Employee;
import com.shiftmate.entity.Notification;
import com.shiftmate.entity.TimeOffRequest;
import com.shiftmate.exception.BusinessRuleException;
import com.shiftmate.exception.ResourceNotFoundException;
import com.shiftmate.repository.EmployeeRepository;
import com.shiftmate.repository.TimeOffRequestRepository;
import com.shiftmate.service.NotificationService;
import com.shiftmate.service.TimeOffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeOffServiceImpl implements TimeOffService {

    private final TimeOffRequestRepository timeOffRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public TimeOffResponse request(Long employeeId, CreateTimeOffRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BusinessRuleException("End date must not be before start date.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        boolean hasOverlap = timeOffRepository.findByEmployeeId(employeeId).stream()
                .filter(r -> r.getStatus() != TimeOffRequest.Status.REJECTED)
                .anyMatch(r -> !req.getStartDate().isAfter(r.getEndDate())
                            && !req.getEndDate().isBefore(r.getStartDate()));
        if (hasOverlap) {
            throw new BusinessRuleException(
                    "A pending or approved time-off request already covers the selected dates.");
        }

        TimeOffRequest tor = TimeOffRequest.builder()
                .employee(employee)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .reason(req.getReason())
                .build();
        timeOffRepository.save(tor);

        notificationService.sendToManagers(employee.getRestaurant().getId(), Notification.Type.TIMEOFF_REQUESTED,
                employee.getFirstName() + " " + employee.getLastName() + " requested time off (" + tor.getStartDate() + " – " + tor.getEndDate() + ").");

        log.info("Employee id={} submitted time-off {} to {}", employeeId,
                req.getStartDate(), req.getEndDate());
        return TimeOffResponse.from(tor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeOffResponse> getMyRequests(Long employeeId) {
        return timeOffRepository.findByEmployeeId(employeeId).stream()
                .map(TimeOffResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeOffResponse> getPendingForManager(Long restaurantId) {
        return timeOffRepository.findPendingByRestaurantId(restaurantId).stream()
                .map(TimeOffResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public TimeOffResponse approve(Long restaurantId, Long requestId, Long managerId) {
        TimeOffRequest tor = resolveForRestaurant(restaurantId, requestId);
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", managerId));

        tor.setStatus(TimeOffRequest.Status.APPROVED);
        tor.setResolvedAt(LocalDateTime.now());
        tor.setResolvedBy(manager);
        timeOffRepository.save(tor);

        notificationService.send(tor.getEmployee().getId(), Notification.Type.TIMEOFF_APPROVED,
                "Your time-off request (" + tor.getStartDate() + " – " + tor.getEndDate() + ") has been approved.");

        log.info("Manager id={} approved time-off request id={}", managerId, requestId);
        return TimeOffResponse.from(tor);
    }

    @Override
    @Transactional
    public TimeOffResponse reject(Long restaurantId, Long requestId, Long managerId) {
        TimeOffRequest tor = resolveForRestaurant(restaurantId, requestId);
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", managerId));

        tor.setStatus(TimeOffRequest.Status.REJECTED);
        tor.setResolvedAt(LocalDateTime.now());
        tor.setResolvedBy(manager);
        timeOffRepository.save(tor);

        notificationService.send(tor.getEmployee().getId(), Notification.Type.TIMEOFF_REJECTED,
                "Your time-off request (" + tor.getStartDate() + " – " + tor.getEndDate() + ") has been rejected.");

        log.info("Manager id={} rejected time-off request id={}", managerId, requestId);
        return TimeOffResponse.from(tor);
    }

    // -------------------------------------------------------------------------

    private TimeOffRequest resolveForRestaurant(Long restaurantId, Long requestId) {
        TimeOffRequest tor = timeOffRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeOffRequest", requestId));

        if (!tor.getEmployee().getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("TimeOffRequest", requestId);
        }
        if (tor.getStatus() != TimeOffRequest.Status.PENDING) {
            throw new BusinessRuleException(
                    "Request has already been " + tor.getStatus().name().toLowerCase() + ".");
        }
        return tor;
    }
}
