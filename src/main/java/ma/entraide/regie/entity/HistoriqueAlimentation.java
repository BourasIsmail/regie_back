package ma.entraide.regie.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_alimentations")
public class HistoriqueAlimentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plafond_id", nullable = false)
    private PlafondRegie plafond;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(name = "montant_alimentation", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantAlimentation;

    @Column(name = "ancien_disponible", precision = 15, scale = 2)
    private BigDecimal ancienDisponible;

    @Column(name = "nouveau_disponible", precision = 15, scale = 2)
    private BigDecimal nouveauDisponible;

    @Column(name = "ancien_avance", precision = 15, scale = 2)
    private BigDecimal ancienAvance;

    @Column(name = "nouveau_avance", precision = 15, scale = 2)
    private BigDecimal nouveauAvance;

    @Column(name = "ancien_encaissement", precision = 15, scale = 2)
    private BigDecimal ancienEncaissement;

    @Column(name = "nouveau_encaissement", precision = 15, scale = 2)
    private BigDecimal nouveauEncaissement;

    @Column(name = "ancien_plafond_fixe", precision = 15, scale = 2)
    private BigDecimal ancienPlafondFixe;

    @Column(name = "nouveau_plafond_fixe", precision = 15, scale = 2)
    private BigDecimal nouveauPlafondFixe;

    @Column(name = "ancien_plafond_annuel", precision = 15, scale = 2)
    private BigDecimal ancienPlafondAnnuel;

    @Column(name = "nouveau_plafond_annuel", precision = 15, scale = 2)
    private BigDecimal nouveauPlafondAnnuel;

    @Column(name = "type_operation", length = 50)
    private String typeOperation;

    @Column(length = 50)
    private String op;

    @Column(name = "date_op")
    private LocalDate dateOp;

    @Column(name = "num_cheque", length = 50)
    private String numCheque;

    @Column(name = "date_cheque")
    private LocalDate dateCheque;

    @Column(length = 500)
    private String commentaire;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public HistoriqueAlimentation() {
    }

    public HistoriqueAlimentation(Long id, PlafondRegie plafond, Province province,
                                  BigDecimal montantAlimentation, BigDecimal ancienDisponible,
                                  BigDecimal nouveauDisponible, BigDecimal ancienAvance,
                                  BigDecimal nouveauAvance, BigDecimal ancienEncaissement,
                                  BigDecimal nouveauEncaissement, BigDecimal ancienPlafondFixe,
                                  BigDecimal nouveauPlafondFixe, BigDecimal ancienPlafondAnnuel,
                                  BigDecimal nouveauPlafondAnnuel, String typeOperation,
                                  String op, LocalDate dateOp, String numCheque,
                                  LocalDate dateCheque, String commentaire,
                                  String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.plafond = plafond;
        this.province = province;
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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlafondRegie getPlafond() {
        return plafond;
    }

    public void setPlafond(PlafondRegie plafond) {
        this.plafond = plafond;
    }

    public Province getProvince() {
        return province;
    }

    public void setProvince(Province province) {
        this.province = province;
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
