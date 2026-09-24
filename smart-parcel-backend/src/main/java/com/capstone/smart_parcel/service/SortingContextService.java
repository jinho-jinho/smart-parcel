package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.ConveyorBelt;
import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.enums.Role;
import com.capstone.smart_parcel.repository.ConveyorBeltRepository;
import com.capstone.smart_parcel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SortingContextService {
    private final UserRepository users;
    private final ConveyorBeltRepository belts;

    public User actor(String email, boolean write) {
        if (email == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var actor = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (write && actor.getRole() != Role.MANAGER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager role required");
        return actor;
    }
    public ConveyorBelt belt(long organization, long belt) {
        return belts.findByOrganization_IdAndId(organization, belt).orElseThrow(SortingContextService::notFound);
    }
    public static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found in this organization");
    }
    public static void require(boolean condition, String message) {
        if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    public static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
