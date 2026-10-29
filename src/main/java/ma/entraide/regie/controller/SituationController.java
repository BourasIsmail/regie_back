package ma.entraide.regie.controller;

import ma.entraide.regie.dto.SituationResponse;
import ma.entraide.regie.service.SituationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/situation")
public class SituationController {

    private final SituationService situationService;

    public SituationController(SituationService situationService) {
        this.situationService = situationService;
    }

    @GetMapping
    public ResponseEntity<SituationResponse> getSituation(
            @RequestParam(required = false) Integer exercice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arreteAu,
            Authentication authentication) {
        return ResponseEntity.ok(situationService.getSituation(authentication.getName(), exercice, arreteAu));
    }
}
