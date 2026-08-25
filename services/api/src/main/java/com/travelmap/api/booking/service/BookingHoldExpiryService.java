package com.travelmap.api.booking.service;

import com.travelmap.api.booking.repository.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class BookingHoldExpiryService {
    private final BookingRepository repository;
    public BookingHoldExpiryService(BookingRepository repository) { this.repository = repository; }
    @Scheduled(fixedDelayString = "${travelmap.booking.expiry-scan-ms:60000}")
    @Transactional public int expireStaleHolds() { return repository.expireStale(Instant.now()); }
}
