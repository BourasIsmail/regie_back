package ma.entraide.regie.controller;

import jakarta.validation.Valid;
import ma.entraide.regie.dto.VersementRequest;
import ma.entraide.regie.dto.VersementResponse;
import ma.entraide.regie.service.VersementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/versements")
public class VersementController {

    private final VersementService versementService;

    public VersementController(VersementService versementService) {
        this.versementService = versementService;
    }

    @GetMapping
    public ResponseEntity<List<VersementResponse>> list(@RequestParam(required = false) Long plafondId,
                                                        Authentication authentication) {
        return ResponseEntity.ok(versementService.list(authentication.getName(), plafondId));
    }

    @PostMapping("/plafond/{plafondId}")
    public ResponseEntity<VersementResponse> create(@PathVariable Long plafondId,
                                                    @Valid @RequestBody VersementRequest request,
                                                    Authentication authentication) {
        return new ResponseEntity<>(
                versementService.create(plafondId, request, authentication.getName()), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        versementService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
