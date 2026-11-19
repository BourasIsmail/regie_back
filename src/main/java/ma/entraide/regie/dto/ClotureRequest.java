package ma.entraide.regie.dto;

import jakarta.validation.constraints.NotNull;

public record ClotureRequest(
        @NotNull(message = "La province est obligatoire") Long provinceId,
        @NotNull(message = "L'annee est obligatoire") Integer annee
) {
}
