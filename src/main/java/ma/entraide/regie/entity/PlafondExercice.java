package ma.entraide.regie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Frozen values of a plafond at the closing of an exercice (kept forever, read-only). */
@Entity
@Getter
@Setter
@Table(name = "plafonds_exercice", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"province_id", "compte_code", "annee"})
})
public class PlafondExercice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plafond_id")
    private Long plafondId;

    @Column(name = "province_id", nullable = false)
    private Long provinceId;

    @Column(name = "province_name")
    private String provinceName;

    @Column(name = "compte_code", nullable = false, length = 20)
    private String compteCode;

    private String libelle;

    @Column(nullable = false)
    private Integer annee;

    @Column(name = "plafond_annuel", precision = 15, scale = 2)
    private BigDecimal plafondAnnuel;

    @Column(name = "budget_annuel_initial", precision = 15, scale = 2)
    private BigDecimal budgetAnnuelInitial;

    @Column(name = "plafond_encaissement", precision = 15, scale = 2)
    private BigDecimal plafondEncaissement;

    @Column(name = "plafond_max_facture", precision = 15, scale = 2)
    private BigDecimal plafondMaxFacture;

    @Column(name = "cree_le")
    private LocalDateTime creeLe;
}
