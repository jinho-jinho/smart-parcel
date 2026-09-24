package com.capstone.smart_parcel.controller;
import com.capstone.smart_parcel.service.DeviceService;
import com.capstone.smart_parcel.service.DeviceEventService;
import com.capstone.smart_parcel.config.security.DeviceIdentity;
import com.capstone.smart_parcel.dto.belt.BeltDtos.ConfigurationResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static com.capstone.smart_parcel.dto.device.DeviceDtos.*;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService configurations;
    private final DeviceEventService events;
    @GetMapping("/setup")
    public ConfigurationResponse setup(@AuthenticationPrincipal DeviceIdentity device) { return configurations.setup(device); }
    @PutMapping("/applied-version")
    public void applied(@AuthenticationPrincipal DeviceIdentity device,@Valid @RequestBody AppliedInput body) {
        configurations.applied(device,body.versionId());
    }
    @PostMapping(value="/events",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public EventAck event(@AuthenticationPrincipal DeviceIdentity device,
                          @Valid @RequestPart("payload") EventInput payload,
                          @RequestPart(value="image",required=false) MultipartFile image) {
        return events.ingest(device,payload,image);
    }
}
