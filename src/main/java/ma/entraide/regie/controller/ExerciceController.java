package ma.entraide.regie.controller;

import jakarta.validation.Valid;
import ma.entraide.regie.dto.ClotureRequest;
import ma.entraide.regie.dto.CloturePrecheckResponse;
import ma.entraide.regie.dto.ExerciceInfoResponse;
import ma.entraide.regie.service.ClotureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ExerciceController {

    private final ClotureService clotureService;

    public ExerciceController(ClotureService clotureService) {
        this.clotureService = clotureService;
    }

    /** Years available in the selector + per-province current exercice (any authenticated role). */
    @GetMapping("/api/exercices")
    public ResponseEntity<ExerciceInfoResponse> info() {
        return ResponseEntity.ok(clotureService.info());
    }

    @GetMapping("/api/admin/exercices/precheck")
    public ResponseEntity<CloturePrecheckResponse> precheck(@RequestParam Long provinceId, @RequestParam Integer annee) {
        return ResponseEntity.ok(clotureService.precheck(provinceId, annee));
    }

    @PostMapping("/api/admin/exercices/cloturer")
    public ResponseEntity<CloturePrecheckResponse> cloturer(@Valid @RequestBody ClotureRequest request) {
        return ResponseEntity.ok(clotureService.cloturer(request.provinceId(), request.annee()));
    }
}
