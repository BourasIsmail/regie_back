package ma.entraide.regie.controller;

import ma.entraide.regie.dto.HistoriqueAlimentationResponse;
import ma.entraide.regie.service.HistoriqueAlimentationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historique")
public class HistoriqueAlimentationController {

    private final HistoriqueAlimentationService historiqueService;

    public HistoriqueAlimentationController(HistoriqueAlimentationService historiqueService) {
        this.historiqueService = historiqueService;
    }

    @GetMapping
    public ResponseEntity<List<HistoriqueAlimentationResponse>> getAll() {
        return ResponseEntity.ok(historiqueService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HistoriqueAlimentationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(historiqueService.getById(id));
    }

    @GetMapping("/plafond/{plafondId}")
    public ResponseEntity<List<HistoriqueAlimentationResponse>> getByPlafond(@PathVariable Long plafondId) {
        return ResponseEntity.ok(historiqueService.getByPlafondId(plafondId));
    }

    @GetMapping("/province/{provinceId}")
    public ResponseEntity<List<HistoriqueAlimentationResponse>> getByProvince(@PathVariable Long provinceId) {
        return ResponseEntity.ok(historiqueService.getByProvinceId(provinceId));
    }

    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<HistoriqueAlimentationResponse>> getByRegion(@PathVariable Long regionId) {
        return ResponseEntity.ok(historiqueService.getByRegionId(regionId));
    }
}
