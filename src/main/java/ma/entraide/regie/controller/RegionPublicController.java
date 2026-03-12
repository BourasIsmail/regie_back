package ma.entraide.regie.controller;

import ma.entraide.regie.dto.RegionResponse;
import ma.entraide.regie.service.RegionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public controller for regions - accessible by all authenticated users
 */
@RestController
@RequestMapping("/api/regions")
public class RegionPublicController {

    private final RegionService regionService;

    public RegionPublicController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    public ResponseEntity<List<RegionResponse>> getAllRegions() {
        return ResponseEntity.ok(regionService.getAllRegions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegionResponse> getRegionById(@PathVariable Long id) {
        return ResponseEntity.ok(regionService.getRegionById(id));
    }
}

