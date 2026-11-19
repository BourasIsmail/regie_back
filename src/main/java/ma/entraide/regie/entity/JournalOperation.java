package ma.entraide.regie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Audit trail of every operation (plafonds, depenses, versements, exercices, users, regions,
 * provinces). Fully denormalised (no foreign keys) so that entries survive deletions.
 */
@Entity
@Getter
@Setter
@Table(name = "journal_operations", indexes = {
        @Index(name = "idx_journal_exercice", columnList = "exercice"),
        @Index(name = "idx_journal_province", columnList = "province_id")
})
public class JournalOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PLAFOND, TRANSACTION, VERSEMENT, EXERCICE, UTILISATEUR, REGION, PROVINCE */
    @Column(nullable = false, length = 30)
    private String entite;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "entite_id")
    private Long entiteId;

    private Integer exercice;

    @Column(name = "province_id")
    private Long provinceId;
    @Column(name = "province_name")
    private String provinceName;
    @Column(name = "region_id")
    private Long regionId;
    @Column(name = "region_name")
    private String regionName;

    @Column(name = "plafond_id")
    private Long plafondId;
    @Column(name = "compte_code", length = 20)
    private String compteCode;
    private String libelle;

    @Column(precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "ancien_encaissement", precision = 15, scale = 2)
    private BigDecimal ancienEncaissement;
    @Column(name = "nouveau_encaissement", precision = 15, scale = 2)
    private BigDecimal nouveauEncaissement;
    @Column(name = "ancien_plafond_annuel", precision = 15, scale = 2)
    private BigDecimal ancienPlafondAnnuel;
    @Column(name = "nouveau_plafond_annuel", precision = 15, scale = 2)
    private BigDecimal nouveauPlafondAnnuel;

    private String op;
    @Column(name = "date_op")
    private LocalDate dateOp;
    @Column(name = "num_cheque")
    private String numCheque;
    @Column(name = "date_cheque")
    private LocalDate dateCheque;

    @Column(length = 1000)
    private String commentaire;

    /** JSON array of {champ, avant, apres} describing what changed. */
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
