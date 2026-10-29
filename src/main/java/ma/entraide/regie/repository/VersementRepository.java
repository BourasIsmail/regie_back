package ma.entraide.regie.repository;

import ma.entraide.regie.entity.Versement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VersementRepository extends JpaRepository<Versement, Long> {

    List<Versement> findByPlafondIdOrderByDateVersementDescIdDesc(Long plafondId);

    List<Versement> findByProvinceIdOrderByDateVersementDescIdDesc(Long provinceId);

    List<Versement> findByProvinceRegionIdOrderByDateVersementDescIdDesc(Long regionId);

    List<Versement> findAllByOrderByDateVersementDescIdDesc();

    /** Rows: [plafondId, sum(montant)] for versements between the two dates (inclusive). */
    @Query("select v.plafond.id, sum(v.montant) from Versement v "
            + "where v.dateVersement >= :from and v.dateVersement <= :to group by v.plafond.id")
    List<Object[]> sumByPlafond(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
