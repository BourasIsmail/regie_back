package ma.entraide.regie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionRegieRequest {

    @NotNull(message = "Province ID is required")
    private Long provinceId;

    @NotBlank(message = "Compte code is required")
    private String compteCode;

    @NotNull(message = "Montant is required")
    @Positive(message = "Montant must be positive")
    private BigDecimal montant;

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

    public TransactionRegieRequest() {
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

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
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
}
