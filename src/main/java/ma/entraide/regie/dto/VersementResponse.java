package ma.entraide.regie.dto;

import ma.entraide.regie.entity.Versement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VersementResponse(
        Long id,
        Long plafondId,
        Long provinceId,
        String provinceName,
        String compteCode,
        BigDecimal montant,
        LocalDate dateVersement,
        String reference,
        String commentaire,
        String createdBy,
        LocalDateTime createdAt
) {
    public static VersementResponse from(Versement v) {
        return new VersementResponse(
                v.getId(),
                v.getPlafond().getId(),
                v.getProvince().getId(),
                v.getProvince().getName(),
                v.getCompteCode(),
                v.getMontant(),
                v.getDateVersement(),
                v.getReference(),
                v.getCommentaire(),
                v.getCreatedBy(),
                v.getCreatedAt());
    }
}
