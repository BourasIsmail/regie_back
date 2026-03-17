package ma.entraide.regie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class PlafondRegieRequest {

    @NotNull(message = "Province ID is required")
    private Long provinceId;

    @NotBlank(message = "Compte code is required")
    private String compteCode;

    @NotBlank(message = "Libelle is required")
    private String libelle;

    @NotNull(message = "Plafond annuel is required")
    @PositiveOrZero(message = "Plafond annuel must be zero or positive")
    private BigDecimal plafondAnnuel;

    @NotNull(message = "Plafond encaissement is required")
    @PositiveOrZero(message = "Plafond encaissement must be zero or positive")
    private BigDecimal plafondEncaissement;

    @NotNull(message = "Plafond max facture is required")
    @PositiveOrZero(message = "Plafond max facture must be zero or positive")
    private BigDecimal plafondMaxFacture;

    public PlafondRegieRequest() {
    }

    public Long getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(Long provinceId) {
        this.provinceId = provinceId;
    }

    public String getCompteCode() {
        return compteCode;
    }

    public void setCompteCode(String compteCode) {
        this.compteCode = compteCode;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public BigDecimal getPlafondAnnuel() {
        return plafondAnnuel;
    }

    public void setPlafondAnnuel(BigDecimal plafondAnnuel) {
        this.plafondAnnuel = plafondAnnuel;
    }

    public BigDecimal getPlafondEncaissement() {
        return plafondEncaissement;
    }

    public void setPlafondEncaissement(BigDecimal plafondEncaissement) {
        this.plafondEncaissement = plafondEncaissement;
    }

    public BigDecimal getPlafondMaxFacture() {
        return plafondMaxFacture;
    }

    public void setPlafondMaxFacture(BigDecimal plafondMaxFacture) {
        this.plafondMaxFacture = plafondMaxFacture;
    }
}
