package com.capstone.smart_parcel.parcel;

import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice(basePackages="com.capstone.smart_parcel.parcel")
@Order(0)
public class ParcelExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null ? "Request rejected" : e.getReason()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> integrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("message","Conflicting identity, configuration or ownership"));
    }
    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<?> retry(CannotAcquireLockException e) {
        return ResponseEntity.status(503).body(Map.of("message","Concurrent update; retry the same request"));
    }
}
