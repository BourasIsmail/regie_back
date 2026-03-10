package ma.entraide.regie.repository;

import ma.entraide.regie.entity.PlafondRegie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlafondRegieRepository extends JpaRepository<PlafondRegie, Long> {

    List<PlafondRegie> findByProvinceId(Long provinceId);

    List<PlafondRegie> findByProvinceRegionId(Long regionId);

    Optional<PlafondRegie> findByProvinceIdAndCompteCode(Long provinceId, String compteCode);
}

