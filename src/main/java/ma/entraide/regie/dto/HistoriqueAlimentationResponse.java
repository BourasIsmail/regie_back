package ma.entraide.regie.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class HistoriqueAlimentationResponse {

    private Long id;
    private Long plafondId;
    private String compteCode;
    private String libelle;
    private Long provinceId;
    private String provinceName;
    private Long regionId;
    private String regionName;
    private BigDecimal montantAlimentation;
    private BigDecimal ancienDisponible;
    private BigDecimal nouveauDisponible;
    private BigDecimal ancienAvance;
    private BigDecimal nouveauAvance;
    private BigDecimal ancienEncaissement;
    private BigDecimal nouveauEncaissement;
    private BigDecimal ancienPlafondFixe;
    private BigDecimal nouveauPlafondFixe;
    private BigDecimal ancienPlafondAnnuel;
    private BigDecimal nouveauPlafondAnnuel;
    private String typeOperation;
    private String op;
    private LocalDate dateOp;
    private String numCheque;
    private LocalDate dateCheque;
    private String commentaire;
    private String createdBy;
    private LocalDateTime createdAt;

    public HistoriqueAlimentationResponse() {
    }

    public HistoriqueAlimentationResponse(Long id, Long plafondId, String compteCode, String libelle,
                                          Long provinceId, String provinceName, Long regionId, String regionName,
                                          BigDecimal montantAlimentation,
                                          BigDecimal ancienDisponible, BigDecimal nouveauDisponible,
                                          BigDecimal ancienAvance, BigDecimal nouveauAvance,
                                          BigDecimal ancienEncaissement, BigDecimal nouveauEncaissement,
                                          BigDecimal ancienPlafondFixe, BigDecimal nouveauPlafondFixe,
                                          BigDecimal ancienPlafondAnnuel, BigDecimal nouveauPlafondAnnuel,
                                          String typeOperation, String op, LocalDate dateOp,
                                          String numCheque, LocalDate dateCheque, String commentaire,
                                          String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.plafondId = plafondId;
        this.compteCode = compteCode;
        this.libelle = libelle;
        this.provinceId = provinceId;
        this.provinceName = provinceName;
        this.regionId = regionId;
        this.regionName = regionName;
        this.montantAlimentation = montantAlimentation;
        this.ancienDisponible = ancienDisponible;
        this.nouveauDisponible = nouveauDisponible;
        this.ancienAvance = ancienAvance;
        this.nouveauAvance = nouveauAvance;
        this.ancienEncaissement = ancienEncaissement;
        this.nouveauEncaissement = nouveauEncaissement;
        this.ancienPlafondFixe = ancienPlafondFixe;
        this.nouveauPlafondFixe = nouveauPlafondFixe;
        this.ancienPlafondAnnuel = ancienPlafondAnnuel;
        this.nouveauPlafondAnnuel = nouveauPlafondAnnuel;
        this.typeOperation = typeOperation;
        this.op = op;
        this.dateOp = dateOp;
        this.numCheque = numCheque;
        this.dateCheque = dateCheque;
        this.commentaire = commentaire;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlafondId() {
        return plafondId;
    }

    public void setPlafondId(Long plafondId) {
        this.plafondId = plafondId;
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

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public BigDecimal getMontantAlimentation() {
        return montantAlimentation;
    }

    public void setMontantAlimentation(BigDecimal montantAlimentation) {
        this.montantAlimentation = montantAlimentation;
    }

    public BigDecimal getAncienDisponible() {
        return ancienDisponible;
    }

    public void setAncienDisponible(BigDecimal ancienDisponible) {
        this.ancienDisponible = ancienDisponible;
    }

    public BigDecimal getNouveauDisponible() {
        return nouveauDisponible;
    }

    public void setNouveauDisponible(BigDecimal nouveauDisponible) {
        this.nouveauDisponible = nouveauDisponible;
    }

    public BigDecimal getAncienAvance() {
        return ancienAvance;
    }

    public void setAncienAvance(BigDecimal ancienAvance) {
        this.ancienAvance = ancienAvance;
    }

    public BigDecimal getNouveauAvance() {
        return nouveauAvance;
    }

    public void setNouveauAvance(BigDecimal nouveauAvance) {
        this.nouveauAvance = nouveauAvance;
    }

    public BigDecimal getAncienEncaissement() {
        return ancienEncaissement;
    }

    public void setAncienEncaissement(BigDecimal ancienEncaissement) {
        this.ancienEncaissement = ancienEncaissement;
    }

    public BigDecimal getNouveauEncaissement() {
        return nouveauEncaissement;
    }

    public void setNouveauEncaissement(BigDecimal nouveauEncaissement) {
        this.nouveauEncaissement = nouveauEncaissement;
    }

    public BigDecimal getAncienPlafondFixe() {
        return ancienPlafondFixe;
    }

    public void setAncienPlafondFixe(BigDecimal ancienPlafondFixe) {
        this.ancienPlafondFixe = ancienPlafondFixe;
    }

    public BigDecimal getNouveauPlafondFixe() {
        return nouveauPlafondFixe;
    }

    public void setNouveauPlafondFixe(BigDecimal nouveauPlafondFixe) {
        this.nouveauPlafondFixe = nouveauPlafondFixe;
    }

    public BigDecimal getAncienPlafondAnnuel() {
        return ancienPlafondAnnuel;
    }

    public void setAncienPlafondAnnuel(BigDecimal ancienPlafondAnnuel) {
        this.ancienPlafondAnnuel = ancienPlafondAnnuel;
    }

    public BigDecimal getNouveauPlafondAnnuel() {
        return nouveauPlafondAnnuel;
    }

    public void setNouveauPlafondAnnuel(BigDecimal nouveauPlafondAnnuel) {
        this.nouveauPlafondAnnuel = nouveauPlafondAnnuel;
    }

    public String getTypeOperation() {
        return typeOperation;
    }

    public void setTypeOperation(String typeOperation) {
        this.typeOperation = typeOperation;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public LocalDate getDateOp() {
        return dateOp;
    }

    public void setDateOp(LocalDate dateOp) {
        this.dateOp = dateOp;
    }

    public String getNumCheque() {
        return numCheque;
    }

    public void setNumCheque(String numCheque) {
        this.numCheque = numCheque;
    }

    public LocalDate getDateCheque() {
        return dateCheque;
    }

    public void setDateCheque(LocalDate dateCheque) {
        this.dateCheque = dateCheque;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
