package ma.entraide.regie.dto;

import java.math.BigDecimal;

/**
 * One line of the "Situation des regies de depenses" statement.
 * avance (II) = plafondGlobal (I) - disponibleRubrique (III); solde (V) = II - IV - versement.
 */
public record SituationRowResponse(
        Long plafondId,
        Long provinceId,
        String dp,
        Long regionId,
        String dr,
        String compteCode,
        String libelle,
        BigDecimal plafondGlobal,
        BigDecimal avance,
        BigDecimal disponibleRubrique,
        BigDecimal disponibleEncaisse,
        BigDecimal depenses,
        BigDecimal versement,
        BigDecimal solde
) {
}
