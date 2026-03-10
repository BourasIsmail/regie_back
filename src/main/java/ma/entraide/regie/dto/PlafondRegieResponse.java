package ma.entraide.regie.dto;

import java.math.BigDecimal;

public class PlafondRegieResponse {

    private Long id;
    private Long provinceId;
    private String provinceName;
    private String compteCode;
    private String libelle;
    private BigDecimal plafondAnnuel;
    private BigDecimal plafondEncaissement;
    private BigDecimal plafondMaxFacture;

    public PlafondRegieResponse() {
    }

    public PlafondRegieResponse(Long id, Long provinceId, String provinceName, String compteCode,
                                String libelle, BigDecimal plafondAnnuel, BigDecimal plafondEncaissement,
                                BigDecimal plafondMaxFacture) {
        this.id = id;
        this.provinceId = provinceId;
        this.provinceName = provinceName;
        this.compteCode = compteCode;
        this.libelle = libelle;
        this.plafondAnnuel = plafondAnnuel;
        this.plafondEncaissement = plafondEncaissement;
        this.plafondMaxFacture = plafondMaxFacture;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(Long provinceId) {
        this.provinceId = provinceId;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
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
