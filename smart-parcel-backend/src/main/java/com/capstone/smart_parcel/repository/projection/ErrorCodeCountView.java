package com.capstone.smart_parcel.repository.projection;

public interface ErrorCodeCountView {
    String getErrorCode(); // alias: errorCode
    Long   getTotal();     // alias: total
}
