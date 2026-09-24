package com.capstone.smart_parcel.service;
import com.capstone.smart_parcel.dto.stats.AttemptStatsResponse;
import com.capstone.smart_parcel.repository.SortingAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import static com.capstone.smart_parcel.service.SortingContextService.require;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final SortingContextService access;
    private final SortingAttemptRepository attempts;
    @Transactional(readOnly = true)
    public AttemptStatsResponse stats(String email, long belt, OffsetDateTime from, OffsetDateTime to) {
        long org = access.actor(email, false).getOrganizationId();
        access.belt(org, belt);
        require(from.isBefore(to), "from must precede to");
        var c = attempts.counts(org, belt, from, to);
        return new AttemptStatsResponse(c.getAttempts(), c.getMatched(), c.getDecisionErrors(), c.getDischargeFailed(), c.getDischargeConflicts());
    }
}
