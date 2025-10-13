package com.capstone.smart_parcel.repository.projection;

import java.time.LocalDate;

public interface DailyCountView {
    LocalDate getDay(); // alias: day
    Long getTotal();     // alias: total
}