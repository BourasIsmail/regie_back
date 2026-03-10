package ma.entraide.regie.repository;

import ma.entraide.regie.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    List<Province> findByRegionId(Long regionId);
}
