package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.history.*;
import com.capstone.smart_parcel.repository.DeviceEventRepository;
import com.capstone.smart_parcel.repository.EventImageRepository;
import com.capstone.smart_parcel.repository.SortingAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import static com.capstone.smart_parcel.service.SortingContextService.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SortingHistoryService {
    private final SortingContextService access;
    private final SortingAttemptRepository attempts;
    private final DeviceEventRepository events;
    private final EventImageRepository images;
    public record ImageContent(String contentType, byte[] bytes) {}

    public PageResponse<AttemptResponse> history(String email, long belt, OffsetDateTime from, OffsetDateTime to, int page, int size) {
        long org = scope(email, belt, from, to);
        return PageResponse.of(attempts.findByOrganization_IdAndBelt_IdAndCapturedAtGreaterThanEqualAndCapturedAtLessThan(
                org, belt, from, to, page(page, size, "capturedAt", "attemptId")), AttemptResponse::from);
    }
    public AttemptResponse detail(String email, UUID id) {
        long org = access.actor(email, false).getOrganizationId();
        return AttemptResponse.from(attempts.findByOrganization_IdAndAttemptId(org, id).orElseThrow(SortingContextService::notFound));
    }
    public List<EventResponse> events(String email, UUID id) {
        long org = access.actor(email, false).getOrganizationId();
        attempts.findByOrganization_IdAndAttemptId(org, id).orElseThrow(SortingContextService::notFound);
        return events.findByOrganization_IdAndAttempt_AttemptIdOrderByOccurredAtAscEventIdAsc(org, id)
                .stream().map(EventResponse::from).toList();
    }
    public ImageContent image(String email, UUID eventId) {
        long org = access.actor(email, false).getOrganizationId();
        events.findByOrganization_IdAndEventId(org, eventId).orElseThrow(SortingContextService::notFound);
        var image = images.findById(eventId).orElseThrow(SortingContextService::notFound);
        return new ImageContent(image.getContentType(), image.getContent());
    }
    public PageResponse<EventResponse> errors(String email, long belt, OffsetDateTime from, OffsetDateTime to, int page, int size) {
        long org = scope(email, belt, from, to);
        return PageResponse.of(events.findByOrganization_IdAndBelt_IdAndErrorCodeIsNotNullAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                org, belt, from, to, page(page, size, "occurredAt", "eventId")), EventResponse::from);
    }
    private long scope(String email, long belt, OffsetDateTime from, OffsetDateTime to) {
        long org = access.actor(email, false).getOrganizationId();
        access.belt(org, belt);
        require(from.isBefore(to), "from must precede to");
        return org;
    }
    public static Pageable page(int page, int size, String... fields) {
        require(size >= 1 && size <= 100 && page >= 0 && page <= 1000000, "Invalid page/size");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, fields));
    }
}
