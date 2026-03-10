package ma.entraide.regie.repository;

import ma.entraide.regie.entity.HistoriqueAlimentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueAlimentationRepository extends JpaRepository<HistoriqueAlimentation, Long> {

    List<HistoriqueAlimentation> findByPlafondId(Long plafondId);

    List<HistoriqueAlimentation> findByProvinceId(Long provinceId);

    List<HistoriqueAlimentation> findByProvinceRegionId(Long regionId);
}
