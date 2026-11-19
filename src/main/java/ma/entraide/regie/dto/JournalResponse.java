package ma.entraide.regie.dto;

import ma.entraide.regie.entity.JournalOperation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JournalResponse(
        Long id,
        String entite,
        String action,
        Long entiteId,
        Integer exercice,
        Long provinceId,
        String provinceName,
        Long regionId,
        String regionName,
        Long plafondId,
        String compteCode,
        String libelle,
        BigDecimal montant,
        BigDecimal ancienEncaissement,
        BigDecimal nouveauEncaissement,
        BigDecimal ancienPlafondAnnuel,
        BigDecimal nouveauPlafondAnnuel,
        String op,
        LocalDate dateOp,
        String numCheque,
        LocalDate dateCheque,
        String commentaire,
        String details,
        String createdBy,
        LocalDateTime createdAt
) {
    public static JournalResponse from(JournalOperation j) {
        return new JournalResponse(j.getId(), j.getEntite(), j.getAction(), j.getEntiteId(), j.getExercice(),
                j.getProvinceId(), j.getProvinceName(), j.getRegionId(), j.getRegionName(), j.getPlafondId(),
                j.getCompteCode(), j.getLibelle(), j.getMontant(), j.getAncienEncaissement(),
                j.getNouveauEncaissement(), j.getAncienPlafondAnnuel(), j.getNouveauPlafondAnnuel(), j.getOp(),
                j.getDateOp(), j.getNumCheque(), j.getDateCheque(), j.getCommentaire(), j.getDetails(),
                j.getCreatedBy(), j.getCreatedAt());
    }
}
