package ma.entraide.regie.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VersementRequest(
        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit etre superieur a 0")
        BigDecimal montant,
        LocalDate dateVersement,
        @Size(max = 100) String reference,
        @Size(max = 500) String commentaire
) {
}
