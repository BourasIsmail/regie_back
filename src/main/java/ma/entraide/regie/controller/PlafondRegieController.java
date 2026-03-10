package ma.entraide.regie.controller;

import jakarta.validation.Valid;
import ma.entraide.regie.dto.AlimentationRequest;
import ma.entraide.regie.dto.PlafondRegieRequest;
import ma.entraide.regie.dto.PlafondRegieResponse;
import ma.entraide.regie.service.PlafondRegieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plafonds")
public class PlafondRegieController {

    private final PlafondRegieService plafondRegieService;

    public PlafondRegieController(PlafondRegieService plafondRegieService) {
        this.plafondRegieService = plafondRegieService;
    }

    @GetMapping
    public ResponseEntity<List<PlafondRegieResponse>> getAll() {
        return ResponseEntity.ok(plafondRegieService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlafondRegieResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(plafondRegieService.getById(id));
    }

    @GetMapping("/province/{provinceId}")
    public ResponseEntity<List<PlafondRegieResponse>> getByProvince(@PathVariable Long provinceId) {
        return ResponseEntity.ok(plafondRegieService.getByProvinceId(provinceId));
    }

    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<PlafondRegieResponse>> getByRegion(@PathVariable Long regionId) {
        return ResponseEntity.ok(plafondRegieService.getByRegionId(regionId));
    }

    @PostMapping
    public ResponseEntity<PlafondRegieResponse> create(@Valid @RequestBody PlafondRegieRequest request) {
        return new ResponseEntity<>(plafondRegieService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlafondRegieResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody PlafondRegieRequest request) {
        return ResponseEntity.ok(plafondRegieService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        plafondRegieService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/alimenter")
    public ResponseEntity<PlafondRegieResponse> alimenter(@PathVariable Long id,
                                                          @Valid @RequestBody AlimentationRequest request,
                                                          Authentication authentication) {
        String performedBy = authentication.getName();
        return ResponseEntity.ok(plafondRegieService.alimenter(id, request, performedBy));
    }
}

