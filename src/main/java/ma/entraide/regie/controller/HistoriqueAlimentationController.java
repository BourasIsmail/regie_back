package ma.entraide.regie.controller;

import ma.entraide.regie.dto.JournalResponse;
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
    public ResponseEntity<List<JournalResponse>> getAll(@RequestParam(required = false) Integer exercice) {
        return ResponseEntity.ok(historiqueService.getAll(exercice));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(historiqueService.getById(id));
    }

    @GetMapping("/plafond/{plafondId}")
    public ResponseEntity<List<JournalResponse>> getByPlafond(@PathVariable Long plafondId,
                                                              @RequestParam(required = false) Integer exercice) {
        return ResponseEntity.ok(historiqueService.getByPlafondId(plafondId, exercice));
    }

    @GetMapping("/province/{provinceId}")
    public ResponseEntity<List<JournalResponse>> getByProvince(@PathVariable Long provinceId,
                                                               @RequestParam(required = false) Integer exercice) {
        return ResponseEntity.ok(historiqueService.getByProvinceId(provinceId, exercice));
    }

    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<JournalResponse>> getByRegion(@PathVariable Long regionId,
                                                             @RequestParam(required = false) Integer exercice) {
        return ResponseEntity.ok(historiqueService.getByRegionId(regionId, exercice));
    }
}
