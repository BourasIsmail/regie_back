package ma.entraide.regie.controller;

import jakarta.validation.Valid;
import ma.entraide.regie.dto.TransactionRegieRequest;
import ma.entraide.regie.dto.TransactionRegieResponse;
import ma.entraide.regie.service.TransactionRegieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionRegieController {

    private final TransactionRegieService transactionService;

    public TransactionRegieController(TransactionRegieService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionRegieResponse>> getAll() {
        return ResponseEntity.ok(transactionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionRegieResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @GetMapping("/province/{provinceId}")
    public ResponseEntity<List<TransactionRegieResponse>> getByProvince(@PathVariable Long provinceId) {
        return ResponseEntity.ok(transactionService.getByProvinceId(provinceId));
    }

    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<TransactionRegieResponse>> getByRegion(@PathVariable Long regionId) {
        return ResponseEntity.ok(transactionService.getByRegionId(regionId));
    }

    @PostMapping
    public ResponseEntity<TransactionRegieResponse> create(@Valid @RequestBody TransactionRegieRequest request,
                                                           Authentication authentication) {
        String createdBy = authentication.getName();
        return new ResponseEntity<>(transactionService.create(request, createdBy), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionRegieResponse> update(@PathVariable Long id,
                                                           @Valid @RequestBody TransactionRegieRequest request,
                                                           Authentication authentication) {
        String updatedBy = authentication.getName();
        return ResponseEntity.ok(transactionService.update(id, request, updatedBy));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        String deletedBy = authentication.getName();
        transactionService.delete(id, deletedBy);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<TransactionRegieResponse> confirm(@PathVariable Long id,
                                                            @RequestParam java.math.BigDecimal montantValide,
                                                            Authentication authentication) {
        String validatedBy = authentication.getName();
        return ResponseEntity.ok(transactionService.confirm(id, montantValide, validatedBy));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<TransactionRegieResponse> reject(@PathVariable Long id,
                                                           @RequestParam(required = false) String motif,
                                                           Authentication authentication) {
        String rejectedBy = authentication.getName();
        return ResponseEntity.ok(transactionService.reject(id, motif, rejectedBy));
    }
}
