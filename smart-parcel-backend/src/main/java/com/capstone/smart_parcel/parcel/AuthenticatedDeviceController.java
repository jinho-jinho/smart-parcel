package com.capstone.smart_parcel.parcel;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static com.capstone.smart_parcel.parcel.ParcelDtos.*;

@RestController
@RequestMapping("/api/v2/device")
@RequiredArgsConstructor
public class AuthenticatedDeviceController {
    private final BeltConfigurationService configurations;
    private final DeviceEventIngestService events;
    @GetMapping("/setup")
    public Object setup(@AuthenticationPrincipal DeviceIdentity device) { return configurations.setup(device); }
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
