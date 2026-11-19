package ma.entraide.regie.repository;

import ma.entraide.regie.entity.Versement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VersementRepository extends JpaRepository<Versement, Long> {

    List<Versement> findByPlafondIdOrderByDateVersementDescIdDesc(Long plafondId);

    List<Versement> findByProvinceIdOrderByDateVersementDescIdDesc(Long provinceId);

    List<Versement> findByProvinceRegionIdOrderByDateVersementDescIdDesc(Long regionId);

    List<Versement> findAllByOrderByDateVersementDescIdDesc();

    /** Rows: [plafondId, sum(montant)] of the exercice, dated up to :to (inclusive). */
    @Query("select v.plafond.id, sum(v.montant) from Versement v "
            + "where v.exercice = :exercice and v.dateVersement <= :to group by v.plafond.id")
    List<Object[]> sumByPlafond(@Param("exercice") Integer exercice, @Param("to") LocalDate to);

    @Query("select coalesce(sum(v.montant), 0) from Versement v where v.plafond.id = :plafondId and v.exercice = :exercice")
    BigDecimal sumMontant(@Param("plafondId") Long plafondId, @Param("exercice") Integer exercice);
}
