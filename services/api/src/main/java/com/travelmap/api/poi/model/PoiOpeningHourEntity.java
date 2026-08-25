package com.travelmap.api.poi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "poi_opening_hour")
public class PoiOpeningHourEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poi_id") private PoiEntity poi;
    @Column(name = "day_of_week", nullable = false) private int dayOfWeek;
    @Column(name = "open_time") private LocalTime openTime;
    @Column(name = "close_time") private LocalTime closeTime;
    @Column(nullable = false) private boolean closed;
    @Column(name = "spans_next_day", nullable = false) private boolean spansNextDay;

    protected PoiOpeningHourEntity() { }
    public PoiOpeningHourEntity(int dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {
        this.id = UUID.randomUUID();
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.closed = closed;
        this.spansNextDay = !closed && !closeTime.isAfter(openTime);
    }
    void attachTo(PoiEntity poi) { this.poi = poi; }
    public int getDayOfWeek() { return dayOfWeek; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public boolean isClosed() { return closed; }
    public boolean isSpansNextDay() { return spansNextDay; }
}
