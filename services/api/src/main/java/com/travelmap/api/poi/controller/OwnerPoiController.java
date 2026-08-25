package com.travelmap.api.poi.controller;

import com.travelmap.api.poi.dto.PoiResponse;
import com.travelmap.api.poi.dto.PoiUpsertRequest;
import com.travelmap.api.poi.service.PoiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner/pois")
public class OwnerPoiController {
    private final PoiService poiService;

    public OwnerPoiController(PoiService poiService) {
        this.poiService = poiService;
    }

    @PostMapping
    ResponseEntity<PoiResponse> create(Authentication authentication, @Valid @RequestBody PoiUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(poiService.create(authentication.getName(), request));
    }

    @PutMapping("/{poiId}")
    PoiResponse update(Authentication authentication, @PathVariable UUID poiId,
                       @Valid @RequestBody PoiUpsertRequest request) {
        return poiService.update(authentication.getName(), poiId, request);
    }

    @GetMapping
    List<PoiResponse> list(Authentication authentication) {
        return poiService.listOwned(authentication.getName());
    }
}
