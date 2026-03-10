package ma.entraide.regie.repository;

import ma.entraide.regie.entity.TransactionRegie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRegieRepository extends JpaRepository<TransactionRegie, Long> {

    List<TransactionRegie> findByProvinceId(Long provinceId);

    List<TransactionRegie> findByProvinceRegionId(Long regionId);

    List<TransactionRegie> findByProvinceIdAndCompteCode(Long provinceId, String compteCode);
}
