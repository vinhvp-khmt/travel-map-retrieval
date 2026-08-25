package com.travelmap.api.poi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "category")
public class CategoryEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false, unique = true, length = 80) private String slug;

    protected CategoryEntity() { }
    public CategoryEntity(UUID id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
}
