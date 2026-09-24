package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.Chute;
import com.capstone.smart_parcel.domain.Device;
import com.capstone.smart_parcel.domain.DeviceEvent;
import com.capstone.smart_parcel.domain.EventImage;
import com.capstone.smart_parcel.domain.SortingAttempt;
import com.capstone.smart_parcel.domain.SortingRule;
import com.capstone.smart_parcel.domain.enums.DecisionStatus;
import com.capstone.smart_parcel.domain.enums.DischargeStatus;
import com.capstone.smart_parcel.domain.enums.EventType;
import com.capstone.smart_parcel.domain.enums.VersionStatus;
import com.capstone.smart_parcel.repository.ChuteRepository;
import com.capstone.smart_parcel.repository.DeviceEventRepository;
import com.capstone.smart_parcel.repository.DeviceRepository;
import com.capstone.smart_parcel.repository.EventImageRepository;
import com.capstone.smart_parcel.repository.EventLockRepository;
import com.capstone.smart_parcel.repository.RuleVersionRepository;
import com.capstone.smart_parcel.repository.SortingAttemptRepository;
import com.capstone.smart_parcel.repository.SortingRuleRepository;
import com.capstone.smart_parcel.repository.VersionChuteRepository;
import com.capstone.smart_parcel.config.security.DeviceIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Objects;
import java.util.HexFormat;
import static com.capstone.smart_parcel.service.SortingContextService.*;
import static com.capstone.smart_parcel.dto.device.DeviceDtos.*;

@Service
@RequiredArgsConstructor
public class DeviceEventService {
    private final EventLockRepository locks;
    private final SortingAttemptRepository attempts;
    private final DeviceEventRepository events;
    private final EventImageRepository images;
    private final RuleVersionRepository versions;
    private final SortingRuleRepository rules;
    private final VersionChuteRepository versionChutes;
    private final ChuteRepository chutes;
    private final DeviceRepository devices;
    private final NotificationService notifications;
    private final ObjectMapper mapper;
    private record ImageData(byte[] bytes, String type, String hash) {}

    @Transactional
    public EventAck ingest(DeviceIdentity identity, EventInput in, MultipartFile file) {
        var image = readImage(file);
        String payload = json(in);
        String hash = sha(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // Same order on every path, including IDs whose rows do not exist yet.
        locks.lock("event:" + in.eventId());
        var prior = events.findById(in.eventId()).orElse(null);
        if (prior != null) {
            if (!prior.getDevice().getId().equals(identity.id()) ||
                    prior.getOrganization().getId() != identity.organizationId() ||
                    !prior.getPayloadSha256().equals(hash) ||
                    !Objects.equals(prior.getImageSha256(), image == null ? null : image.hash()))
                throw conflict("Event ID already exists with different content or owner");
            return new EventAck(in.eventId(), in.attemptId(), true);
        }
        var device = devices.findByOrganization_IdAndId(identity.organizationId(), identity.id())
                .orElseThrow(SortingContextService::notFound);
        require(device.getBelt().getId() == identity.beltId() && !device.isCredentialRevoked(), "Device credentials revoked");
        if (in.eventType() != EventType.DECISION) {
            require(in.ruleVersionId() == null && in.capturedAt() == null && in.decidedAt() == null &&
                    in.decisionStatus() == null && in.recognizedInputType() == null && in.recognizedValue() == null &&
                    in.ruleId() == null && in.chuteId() == null, "Decision fields are not accepted on other event types");
            if (in.eventType() == EventType.DEVICE_ERROR)
                require(in.observedChuteId() == null, "Observed chute belongs in a discharge event");
        }
        if (in.eventType() != EventType.DEVICE_ERROR) require(in.attemptId() != null, "attemptId is required");
        if (in.attemptId() != null) locks.lock("attempt:" + in.attemptId());
        SortingAttempt attempt;
        if (in.eventType() == EventType.DECISION) {
            require(image != null, "DECISION requires a PNG or JPEG image");
            attempt = createAttempt(device, in);
        } else if (in.eventType() == EventType.DEVICE_ERROR) {
            require(in.errorCode() != null && !in.errorCode().isBlank(), "errorCode is required");
            attempt = in.attemptId() == null ? null : attempt(identity, in.attemptId());
        } else {
            attempt = discharge(identity, in);
        }
        String uri = image == null ? null : "/api/events/" + in.eventId() + "/image";
        var event = events.saveAndFlush(DeviceEvent.builder().eventId(in.eventId()).organization(device.getOrganization())
                .belt(device.getBelt()).device(device).attempt(attempt).eventType(in.eventType())
                .errorCode(in.errorCode()).occurredAt(in.occurredAt()).payloadSha256(hash)
                .imageSha256(image == null ? null : image.hash()).imageUri(uri).payload(payload).build());
        if (image != null) {
            images.save(EventImage.builder().eventId(in.eventId()).contentType(image.type()).content(image.bytes()).build());
            if (in.eventType() == EventType.DECISION) attempt.setImageUri(uri);
        }
        if (in.eventType() == EventType.DEVICE_ERROR || in.eventType() == EventType.DISCHARGE_FAILED ||
                (in.eventType() == EventType.DECISION && in.decisionStatus() == DecisionStatus.ERROR))
            notifications.record(event);
        // Update only last_seen_at: never overwrite a concurrent credential revocation with a stale entity.
        devices.touch(device.getId(), OffsetDateTime.now());
        return new EventAck(in.eventId(), in.attemptId(), false);
    }

    private SortingAttempt createAttempt(Device device, EventInput in) {
        require(in.ruleVersionId() != null && in.capturedAt() != null && in.decidedAt() != null && in.decisionStatus() != null,
                "DECISION requires ruleVersionId, capturedAt, decidedAt and decisionStatus");
        require(!in.decidedAt().isBefore(in.capturedAt()), "decidedAt precedes capturedAt");
        if (attempts.existsById(in.attemptId())) throw conflict("Attempt already has a decision; retransmit its original eventId");
        long org = device.getOrganization().getId(), belt = device.getBelt().getId();
        var version = versions.findByOrganization_IdAndBelt_IdAndId(org, belt, in.ruleVersionId())
                .filter(v -> v.getStatus() == VersionStatus.PUBLISHED).orElseThrow(SortingContextService::notFound);
        require((in.recognizedInputType() == null) == (in.recognizedValue() == null), "Input type and value must be provided together");
        String input = in.recognizedValue() == null ? null : SortingRuleService.normalize(in.recognizedValue());
        SortingRule rule = null;
        String item = null, chuteName = null;
        if (in.decisionStatus() == DecisionStatus.MATCHED) {
            require(in.ruleId() != null && in.chuteId() != null && in.recognizedInputType() != null && in.errorCode() == null,
                    "MATCHED requires a rule, chute, recognized input and no error");
            rule = rules.findByOrganization_IdAndBelt_IdAndVersion_IdAndIdAndChute_Id(org, belt, in.ruleVersionId(), in.ruleId(), in.chuteId())
                    .orElseThrow(SortingContextService::notFound);
            require(rule.getInputType() == in.recognizedInputType() && rule.getInputValue().equals(input),
                    "Recognized input does not match the selected rule");
            item = rule.getItemName();
            chuteName = versionChutes.findByVersionIdAndChuteId(in.ruleVersionId(), in.chuteId())
                    .orElseThrow(SortingContextService::notFound).getChuteNameSnapshot();
        } else {
            require(in.ruleId() == null && in.chuteId() == null, "Unmatched/error decisions cannot have a destination");
            require(in.decisionStatus() == DecisionStatus.ERROR
                    ? in.errorCode() != null && !in.errorCode().isBlank() : in.errorCode() == null, "Invalid decision errorCode");
        }
        require(in.observedChuteId() == null, "Observed chute belongs in a discharge event");
        return attempts.saveAndFlush(SortingAttempt.builder().attemptId(in.attemptId()).organization(device.getOrganization())
                .belt(device.getBelt()).device(device).ruleVersion(version).capturedAt(in.capturedAt())
                .decidedAt(in.decidedAt()).decisionStatus(in.decisionStatus()).decisionErrorCode(in.errorCode())
                .recognizedInputType(in.recognizedInputType()).recognizedValue(input).rule(rule)
                .chute(rule == null ? null : rule.getChute()).groupNameSnapshot(version.getGroupNameSnapshot())
                .itemNameSnapshot(item).chuteNameSnapshot(chuteName).build());
    }

    private SortingAttempt attempt(DeviceIdentity d, UUID id) {
        return attempts.lockByScope(d.organizationId(), d.beltId(), d.id(), id).orElseThrow(SortingContextService::notFound);
    }
    private SortingAttempt discharge(DeviceIdentity d, EventInput in) {
        var attempt = attempt(d, in.attemptId());
        require(attempt.getDecisionStatus() == DecisionStatus.MATCHED, "Only matched attempts accept discharge events");
        boolean failed = in.eventType() == EventType.DISCHARGE_FAILED;
        require(failed ? in.errorCode() != null && !in.errorCode().isBlank() : in.errorCode() == null, "Invalid discharge errorCode");
        Chute observed = null;
        if (in.observedChuteId() != null) {
            observed = chutes.findByOrganization_IdAndBelt_IdAndId(d.organizationId(), d.beltId(), in.observedChuteId())
                    .orElseThrow(SortingContextService::notFound);
            require(failed || attempt.getChute().getId().equals(in.observedChuteId()), "Confirmed destination differs from target");
        }
        var previous = attempt.getDischargeStatus();
        var incoming = failed ? DischargeStatus.FAILED : DischargeStatus.CONFIRMED;
        attempt.setDischargeConflict(attempt.isDischargeConflict() ||
                (previous != DischargeStatus.UNCONFIRMED && incoming != previous));
        attempt.setDischargeStatus(failed || previous == DischargeStatus.FAILED ? DischargeStatus.FAILED : DischargeStatus.CONFIRMED);
        OffsetDateTime at = in.occurredAt();
        // Arrival order does not change the final projection; retain both raw events.
        if (previous != DischargeStatus.UNCONFIRMED && (previous == incoming || previous == DischargeStatus.FAILED)) {
            var old = attempt.getDischargeAt();
            if (old.isBefore(at) || (previous == DischargeStatus.FAILED && !failed)) {
                at = old;
                observed = attempt.getObservedChute();
            } else if (old.isEqual(at) && attempt.getObservedChute() != null) {
                if (observed == null || attempt.getObservedChute().getId() < observed.getId())
                    observed = attempt.getObservedChute();
            }
        }
        attempt.setDischargeAt(at);
        attempt.setObservedChute(observed);
        return attempt;
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid event payload",e); }
    }
    private static String sha(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private ImageData readImage(MultipartFile file) {
        if(file==null || file.isEmpty()) return null;
        require(file.getSize()<=5*1024*1024,"Image limit is 5 MiB");
        try {
            byte[] bytes=file.getBytes(); String type;
            try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(stream);
                require(readers.hasNext(),"Invalid image");
                var reader=readers.next();
                try {
                    reader.setInput(stream);
                    String format=reader.getFormatName();
                    require(format.equalsIgnoreCase("png") || format.equalsIgnoreCase("jpeg"),"Only PNG/JPEG supported");
                    require((long)reader.getWidth(0)*reader.getHeight(0)<=16_000_000,"Image dimensions too large");
                    require(reader.read(0)!=null,"Invalid image");
                    type=format.equalsIgnoreCase("png")?"image/png":"image/jpeg";
                } finally { reader.dispose(); }
            }
            return new ImageData(bytes,type,sha(bytes));
        } catch(IOException e) { throw new IllegalArgumentException("Cannot decode image",e); }
    }
}
