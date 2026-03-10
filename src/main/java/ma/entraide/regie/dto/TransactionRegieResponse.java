package ma.entraide.regie.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionRegieResponse {

    private Long id;
    private Long provinceId;
    private String provinceName;
    private String compteCode;
    private BigDecimal montant;
    private BigDecimal montantValide;
    private String statut;
    private String validatedBy;
    private LocalDateTime validatedAt;
    private String fournisseur;
    private String adresseFournisseur;
    private String factureNumero;
    private LocalDate factureDate;
    private String numeroAp;
    private LocalDate dateAp;
    private String moisAnnee;
    private String typeTransaction;
    private String description;
    private LocalDateTime dateTransaction;
    private String createdBy;
    private LocalDateTime createdAt;

    public TransactionRegieResponse() {
    }

    public TransactionRegieResponse(Long id, Long provinceId, String provinceName, String compteCode,
                                    BigDecimal montant, BigDecimal montantValide, String statut,
                                    String validatedBy, LocalDateTime validatedAt,
                                    String fournisseur, String adresseFournisseur,
                                    String factureNumero, LocalDate factureDate, String numeroAp,
                                    LocalDate dateAp, String moisAnnee, String typeTransaction,
                                    String description, LocalDateTime dateTransaction,
                                    String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.provinceId = provinceId;
        this.provinceName = provinceName;
        this.compteCode = compteCode;
        this.montant = montant;
        this.montantValide = montantValide;
        this.statut = statut;
        this.validatedBy = validatedBy;
        this.validatedAt = validatedAt;
        this.fournisseur = fournisseur;
        this.adresseFournisseur = adresseFournisseur;
        this.factureNumero = factureNumero;
        this.factureDate = factureDate;
        this.numeroAp = numeroAp;
        this.dateAp = dateAp;
        this.moisAnnee = moisAnnee;
        this.typeTransaction = typeTransaction;
        this.description = description;
        this.dateTransaction = dateTransaction;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
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

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public BigDecimal getMontantValide() {
        return montantValide;
    }

    public void setMontantValide(BigDecimal montantValide) {
        this.montantValide = montantValide;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(String validatedBy) {
        this.validatedBy = validatedBy;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }

    public String getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(String fournisseur) {
        this.fournisseur = fournisseur;
    }

    public String getAdresseFournisseur() {
        return adresseFournisseur;
    }

    public void setAdresseFournisseur(String adresseFournisseur) {
        this.adresseFournisseur = adresseFournisseur;
    }

    public String getFactureNumero() {
        return factureNumero;
    }

    public void setFactureNumero(String factureNumero) {
        this.factureNumero = factureNumero;
    }

    public LocalDate getFactureDate() {
        return factureDate;
    }

    public void setFactureDate(LocalDate factureDate) {
        this.factureDate = factureDate;
    }

    public String getNumeroAp() {
        return numeroAp;
    }

    public void setNumeroAp(String numeroAp) {
        this.numeroAp = numeroAp;
    }

    public LocalDate getDateAp() {
        return dateAp;
    }

    public void setDateAp(LocalDate dateAp) {
        this.dateAp = dateAp;
    }

    public String getMoisAnnee() {
        return moisAnnee;
    }

    public void setMoisAnnee(String moisAnnee) {
        this.moisAnnee = moisAnnee;
    }

    public String getTypeTransaction() {
        return typeTransaction;
    }

    public void setTypeTransaction(String typeTransaction) {
        this.typeTransaction = typeTransaction;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateTransaction() {
        return dateTransaction;
    }

    public void setDateTransaction(LocalDateTime dateTransaction) {
        this.dateTransaction = dateTransaction;
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

