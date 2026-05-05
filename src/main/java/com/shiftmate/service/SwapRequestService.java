package com.shiftmate.service;

import com.shiftmate.dto.CreateSwapRequest;
import com.shiftmate.dto.SwapRequestResponse;

import java.util.List;

public interface SwapRequestService {

    SwapRequestResponse createSwapRequest(Long employeeId, Long restaurantId, CreateSwapRequest request);

    List<SwapRequestResponse> getMySwapRequests(Long employeeId);

    List<SwapRequestResponse> getPendingForManager(Long restaurantId);

    SwapRequestResponse approve(Long restaurantId, Long swapRequestId, Long managerId);

    SwapRequestResponse reject(Long restaurantId, Long swapRequestId, Long managerId);
}
