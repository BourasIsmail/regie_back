package ma.entraide.regie.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions_regie")
public class TransactionRegie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(name = "compte_code", nullable = false, length = 20)
    private String compteCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "montant_valide", precision = 15, scale = 2)
    private BigDecimal montantValide;

    @Column(nullable = false, length = 20)
    private String statut = "EN_ATTENTE";

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(length = 255)
    private String fournisseur;

    @Column(name = "adresse_fournisseur", length = 500)
    private String adresseFournisseur;

    @Column(name = "facture_numero", length = 50)
    private String factureNumero;

    @Column(name = "facture_date")
    private LocalDate factureDate;

    @Column(name = "numero_ap", length = 50)
    private String numeroAp;

    @Column(name = "date_ap")
    private LocalDate dateAp;

    @Column(name = "mois_annee", length = 10)
    private String moisAnnee;

    @Column(name = "type_transaction", length = 50)
    private String typeTransaction;

    @Column(length = 500)
    private String description;

    @Column(name = "date_transaction", nullable = false)
    private LocalDateTime dateTransaction;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TransactionRegie() {
    }

    public TransactionRegie(Long id, Province province, String compteCode, BigDecimal montant,
                            String fournisseur, String adresseFournisseur, String factureNumero,
                            LocalDate factureDate, String numeroAp, LocalDate dateAp,
                            String moisAnnee, String typeTransaction, String description,
                            LocalDateTime dateTransaction, String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.province = province;
        this.compteCode = compteCode;
        this.montant = montant;
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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.dateTransaction == null) {
            this.dateTransaction = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Province getProvince() {
        return province;
    }

    public void setProvince(Province province) {
        this.province = province;
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
