package com.shiftmate.service;

import com.shiftmate.dto.CreateSwapRequest;
import com.shiftmate.dto.SwapRequestResponse;

import java.util.List;

public interface SwapService {

    SwapRequestResponse requestSwap(Long employeeId, Long restaurantId, CreateSwapRequest request);

    List<SwapRequestResponse> getMySwaps(Long employeeId);

    List<SwapRequestResponse> getPendingForManager(Long restaurantId);

    SwapRequestResponse approve(Long restaurantId, Long swapId, Long managerId);

    SwapRequestResponse reject(Long restaurantId, Long swapId, Long managerId);
}
