package ma.entraide.regie.controller;

import jakarta.validation.Valid;
import ma.entraide.regie.dto.RegionRequest;
import ma.entraide.regie.dto.RegionResponse;
import ma.entraide.regie.service.RegionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
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

    @PostMapping
    public ResponseEntity<RegionResponse> createRegion(@Valid @RequestBody RegionRequest request) {
        return new ResponseEntity<>(regionService.createRegion(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RegionResponse> updateRegion(@PathVariable Long id,
                                                       @Valid @RequestBody RegionRequest request) {
        return ResponseEntity.ok(regionService.updateRegion(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegion(@PathVariable Long id) {
        regionService.deleteRegion(id);
        return ResponseEntity.noContent().build();
    }
}

