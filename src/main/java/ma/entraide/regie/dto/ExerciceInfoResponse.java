package ma.entraide.regie.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * What the front needs to drive the exercice selector.
 * annees: years the user may select (current + closed years when allowed).
 */
public record ExerciceInfoResponse(
        int exerciceCourant,
        List<Integer> annees,
        boolean peutConsulterClotures,
        boolean peutCloturer,
        List<ProvinceExercice> provinces
) {
    public record ProvinceExercice(
            Long provinceId,
            String provinceName,
            Long regionId,
            int exerciceCourant,
            List<Cloture> clotures
    ) {
    }

    public record Cloture(int annee, String cloturePar, LocalDateTime clotureLe) {
    }
}
