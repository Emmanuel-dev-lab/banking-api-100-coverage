package com.bank.domain.port;

import java.time.LocalDate;

public interface Clock {
    LocalDate today();
}
