package ma.entraide.regie.repository;

import ma.entraide.regie.entity.PlafondExercice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlafondExerciceRepository extends JpaRepository<PlafondExercice, Long> {

    List<PlafondExercice> findByAnnee(Integer annee);

    Optional<PlafondExercice> findByProvinceIdAndCompteCodeAndAnnee(Long provinceId, String compteCode, Integer annee);
}
