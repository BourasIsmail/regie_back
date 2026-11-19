package ma.entraide.regie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A closed exercice of one province. Only closed exercices are stored: the current exercice of a
 * province is (last closed year + 1), or the configured initial year when nothing was closed yet.
 */
@Entity
@Getter
@Setter
@Table(name = "exercices_province", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"province_id", "annee"})
})
public class ExerciceProvince {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "province_id", nullable = false)
    private Long provinceId;

    @Column(name = "province_name")
    private String provinceName;

    @Column(nullable = false)
    private Integer annee;

    @Column(nullable = false, length = 20)
    private String statut = "CLOTURE";

    @Column(name = "cloture_par")
    private String cloturePar;

    @Column(name = "cloture_le")
    private LocalDateTime clotureLe;
}
