package ma.entraide.regie.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SituationResponse(
        int exercice,
        LocalDate arreteAu,
        List<SituationRowResponse> rows,
        Totals totals
) {
    public record Totals(
            BigDecimal plafondGlobal,
            BigDecimal avance,
            BigDecimal disponibleRubrique,
            BigDecimal disponibleEncaisse,
            BigDecimal depenses,
            BigDecimal versement,
            BigDecimal solde
    ) {
    }
}
