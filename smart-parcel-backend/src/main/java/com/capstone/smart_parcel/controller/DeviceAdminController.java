package com.capstone.smart_parcel.controller;
import com.capstone.smart_parcel.service.DeviceService;
import com.capstone.smart_parcel.dto.belt.BeltDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api")
public class DeviceAdminController {
    private final DeviceService devices;
    @GetMapping("/belts/{belt}/devices")
    public List<DeviceResponse> list(Authentication a, @PathVariable long belt) { return devices.list(a.getName(), belt); }
    @PostMapping("/belts/{belt}/devices")
    public DeviceCredential create(Authentication a, @PathVariable long belt, @Valid @RequestBody DeviceInput in) {
        return devices.registerDevice(a.getName(), belt, in);
    }
    @PostMapping("/devices/{id}/deactivate")
    public void deactivate(Authentication a, @PathVariable UUID id, @RequestParam(defaultValue="false") boolean revoke) {
        devices.deactivateDevice(a.getName(), id, revoke);
    }
}
