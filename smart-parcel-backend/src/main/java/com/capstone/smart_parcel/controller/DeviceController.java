package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.dto.device.DeviceErrorEventRequest;
import com.capstone.smart_parcel.dto.device.DeviceEventResponse;
import com.capstone.smart_parcel.dto.device.DeviceSetupResponse;
import com.capstone.smart_parcel.dto.device.DeviceSortingResultRequest;
import com.capstone.smart_parcel.service.DeviceEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceEventService deviceEventService;

    @GetMapping("/setup")
    public ResponseEntity<ApiResponse<DeviceSetupResponse>> fetchSetup(
            @RequestParam("managerId") Long managerId
    ) {
        DeviceSetupResponse response = deviceEventService.fetchSetup(managerId);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Fetched active sorting configuration."));
    }

    @PostMapping(path = "/events/sorting-result", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DeviceEventResponse>> sortingResult(
            @Valid @RequestPart("payload") DeviceSortingResultRequest payload,
            @RequestPart("image") MultipartFile image
    ) {
        Long historyId = deviceEventService.recordSortingResult(payload, image);
        return ResponseEntity.ok(new ApiResponse<>(true, DeviceEventResponse.sorting(historyId), "Sorting result stored."));
    }

    @PostMapping(path = "/events/error", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DeviceEventResponse>> errorEvent(
            @Valid @RequestPart("payload") DeviceErrorEventRequest payload,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        Long errorId = deviceEventService.recordErrorEvent(payload, image);
        return ResponseEntity.ok(new ApiResponse<>(true, DeviceEventResponse.error(errorId), "Error event stored."));
    }
}
