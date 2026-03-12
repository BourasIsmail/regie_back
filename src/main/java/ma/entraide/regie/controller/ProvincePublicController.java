package ma.entraide.regie.controller;

import ma.entraide.regie.dto.ProvinceResponse;
import ma.entraide.regie.service.ProvinceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public controller for provinces - accessible by all authenticated users
 */
@RestController
@RequestMapping("/api/provinces")
public class ProvincePublicController {

    private final ProvinceService provinceService;

    public ProvincePublicController(ProvinceService provinceService) {
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
}

