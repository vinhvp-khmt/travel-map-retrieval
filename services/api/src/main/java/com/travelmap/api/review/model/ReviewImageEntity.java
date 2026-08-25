package com.travelmap.api.review.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "review_image")
public class ReviewImageEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "review_id") private ReviewEntity review;
    @Column(nullable = false, length = 1000) private String url;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    protected ReviewImageEntity() { }
    ReviewImageEntity(ReviewEntity review, String url, int sortOrder) {
        this.id = UUID.randomUUID(); this.review = review; this.url = url; this.sortOrder = sortOrder;
    }
    public String getUrl() { return url; }
    public int getSortOrder() { return sortOrder; }
}
