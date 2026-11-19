package ma.entraide.regie.repository;

import ma.entraide.regie.entity.ExerciceProvince;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciceProvinceRepository extends JpaRepository<ExerciceProvince, Long> {

    boolean existsByProvinceIdAndAnnee(Long provinceId, Integer annee);

    List<ExerciceProvince> findByProvinceIdOrderByAnneeDesc(Long provinceId);

    /** Rows: [provinceId, max(annee)] */
    @Query("select e.provinceId, max(e.annee) from ExerciceProvince e group by e.provinceId")
    List<Object[]> lastClosedByProvince();
}
