package ma.entraide.regie.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "plafonds_regie", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"province_id", "compte_code"})
})
public class PlafondRegie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(name = "compte_code", nullable = false, length = 20)
    private String compteCode;

    @Column(nullable = false)
    private String libelle;

    @Column(name = "plafond_annuel", nullable = false, precision = 15, scale = 2)
    private BigDecimal plafondAnnuel;

    @Column(name = "budget_annuel_initial", precision = 15, scale = 2)
    private BigDecimal budgetAnnuelInitial;

    @Column(name = "plafond_encaissement", nullable = false, precision = 15, scale = 2)
    private BigDecimal plafondEncaissement;

    @Column(name = "plafond_max_facture", nullable = false, precision = 15, scale = 2)
    private BigDecimal plafondMaxFacture;

    public PlafondRegie() {
    }

    public PlafondRegie(Long id, Province province, String compteCode, String libelle,
                        BigDecimal plafondAnnuel, BigDecimal plafondEncaissement, BigDecimal plafondMaxFacture) {
        this.id = id;
        this.province = province;
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

    public BigDecimal getBudgetAnnuelInitial() {
        return budgetAnnuelInitial;
    }

    public void setBudgetAnnuelInitial(BigDecimal budgetAnnuelInitial) {
        this.budgetAnnuelInitial = budgetAnnuelInitial;
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
