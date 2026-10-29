package ma.entraide.regie.repository;

import ma.entraide.regie.entity.TransactionRegie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRegieRepository extends JpaRepository<TransactionRegie, Long> {

    List<TransactionRegie> findByProvinceId(Long provinceId);

    List<TransactionRegie> findByProvinceRegionId(Long regionId);

    List<TransactionRegie> findByProvinceIdAndCompteCode(Long provinceId, String compteCode);

    /** Rows: [provinceId, compteCode, sum(montantValide)] of confirmed transactions in [from, to). */
    @Query("select t.province.id, t.compteCode, sum(t.montantValide) from TransactionRegie t "
            + "where t.statut = 'CONFIRMEE' and t.dateTransaction >= :from and t.dateTransaction < :to "
            + "group by t.province.id, t.compteCode")
    List<Object[]> sumConfirmedByRubrique(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
