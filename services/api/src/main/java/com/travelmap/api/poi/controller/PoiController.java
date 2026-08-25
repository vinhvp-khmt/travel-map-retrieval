package com.travelmap.api.poi.controller;

import com.travelmap.api.poi.dto.PoiResponse;
import com.travelmap.api.poi.service.PoiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pois")
public class PoiController {
    private final PoiService poiService;

    public PoiController(PoiService poiService) {
        this.poiService = poiService;
    }

    @GetMapping("/{poiId}")
    PoiResponse getActive(@PathVariable UUID poiId) {
        return poiService.getActive(poiId);
    }
}
