package ma.entraide.regie.controller;

import jakarta.validation.Valid;
import ma.entraide.regie.dto.ProvinceRequest;
import ma.entraide.regie.dto.ProvinceResponse;
import ma.entraide.regie.service.ProvinceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/provinces")
public class ProvinceController {

    private final ProvinceService provinceService;

    public ProvinceController(ProvinceService provinceService) {
        this.provinceService = provinceService;
    }

    @GetMapping
    public ResponseEntity<List<ProvinceResponse>> getAllProvinces() {
        return ResponseEntity.ok(provinceService.getAllProvinces());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProvinceResponse> getProvinceById(@PathVariable Long id) {
        return ResponseEntity.ok(provinceService.getProvinceById(id));
    }

    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<ProvinceResponse>> getProvincesByRegion(@PathVariable Long regionId) {
        return ResponseEntity.ok(provinceService.getProvincesByRegionId(regionId));
    }

    @PostMapping
    public ResponseEntity<ProvinceResponse> createProvince(@Valid @RequestBody ProvinceRequest request) {
        return new ResponseEntity<>(provinceService.createProvince(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProvinceResponse> updateProvince(@PathVariable Long id,
                                                           @Valid @RequestBody ProvinceRequest request) {
        return ResponseEntity.ok(provinceService.updateProvince(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProvince(@PathVariable Long id) {
        provinceService.deleteProvince(id);
        return ResponseEntity.noContent().build();
    }
}

