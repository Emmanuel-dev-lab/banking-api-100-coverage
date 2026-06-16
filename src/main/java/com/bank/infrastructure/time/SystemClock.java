package com.bank.infrastructure.time;

import com.bank.domain.port.Clock;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SystemClock implements Clock {
    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}
