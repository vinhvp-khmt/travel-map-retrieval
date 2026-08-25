package com.travelmap.api.poi.model;

import com.travelmap.api.auth.model.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "poi")
public class PoiEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id") private UserEntity owner;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id") private CategoryEntity category;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "normalized_name", nullable = false, length = 120) private String normalizedName;
    @Column(length = 3000) private String description;
    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;
    @Column(nullable = false, length = 500) private String address;
    @Column(name = "price_level") private Integer priceLevel;
    private Integer capacity;
    @Column(name = "booking_enabled", nullable = false) private boolean bookingEnabled;
    @Column(name = "external_provider", length = 40) private String externalProvider;
    @Column(name = "external_id", length = 160) private String externalId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30) private PoiStatus status;
    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2) private BigDecimal avgRating;
    @Column(name = "rating_count", nullable = false) private int ratingCount;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC, openTime ASC")
    private List<PoiOpeningHourEntity> openingHours = new ArrayList<>();

    protected PoiEntity() { }

    public PoiEntity(UserEntity owner, CategoryEntity category, String name, String normalizedName,
                     String description, double latitude, double longitude, String address,
                     Integer priceLevel, Integer capacity, boolean bookingEnabled) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.category = category;
        this.avgRating = BigDecimal.ZERO;
        this.ratingCount = 0;
        this.createdAt = Instant.now();
        apply(category, name, normalizedName, description, latitude, longitude, address,
                priceLevel, capacity, bookingEnabled);
        this.status = PoiStatus.PENDING_APPROVAL;
    }

    public void apply(CategoryEntity category, String name, String normalizedName, String description,
                      double latitude, double longitude, String address, Integer priceLevel,
                      Integer capacity, boolean bookingEnabled) {
        this.category = category;
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.priceLevel = priceLevel;
        this.capacity = capacity;
        this.bookingEnabled = bookingEnabled;
        this.updatedAt = Instant.now();
    }

    public void replaceOpeningHours(List<PoiOpeningHourEntity> hours) {
        openingHours.clear();
        hours.forEach(hour -> { hour.attachTo(this); openingHours.add(hour); });
    }

    public void markPendingApproval() { status = PoiStatus.PENDING_APPROVAL; updatedAt = Instant.now(); }
    public void approve() { status = PoiStatus.ACTIVE; updatedAt = Instant.now(); }
    public void reject() { status = PoiStatus.REJECTED; updatedAt = Instant.now(); }
    public void attachExternalReference(String provider, String externalId) {
        this.externalProvider = provider;
        this.externalId = externalId;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UserEntity getOwner() { return owner; }
    public CategoryEntity getCategory() { return category; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public Integer getPriceLevel() { return priceLevel; }
    public Integer getCapacity() { return capacity; }
    public boolean isBookingEnabled() { return bookingEnabled; }
    public PoiStatus getStatus() { return status; }
    public String getExternalProvider() { return externalProvider; }
    public String getExternalId() { return externalId; }
    public BigDecimal getAvgRating() { return avgRating; }
    public int getRatingCount() { return ratingCount; }
    public List<PoiOpeningHourEntity> getOpeningHours() { return List.copyOf(openingHours); }
}
