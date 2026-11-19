package ma.entraide.regie.repository;

import ma.entraide.regie.entity.TransactionRegie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionRegieRepository extends JpaRepository<TransactionRegie, Long> {

    List<TransactionRegie> findByProvinceId(Long provinceId);

    List<TransactionRegie> findByProvinceRegionId(Long regionId);

    List<TransactionRegie> findByProvinceIdAndCompteCode(Long provinceId, String compteCode);

    /** Rows: [provinceId, compteCode, sum(montantValide)] of confirmed transactions of the exercice dated before :to. */
    @Query("select t.province.id, t.compteCode, sum(t.montantValide) from TransactionRegie t "
            + "where t.statut = 'CONFIRMEE' and t.exercice = :exercice and t.dateTransaction < :to "
            + "group by t.province.id, t.compteCode")
    List<Object[]> sumConfirmedByRubrique(@Param("exercice") Integer exercice, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(t.montantValide), 0) from TransactionRegie t "
            + "where t.statut = 'CONFIRMEE' and t.province.id = :provinceId and t.compteCode = :compteCode "
            + "and t.exercice = :exercice")
    BigDecimal sumConfirmed(@Param("provinceId") Long provinceId, @Param("compteCode") String compteCode,
                            @Param("exercice") Integer exercice);

    long countByProvinceIdAndCompteCodeAndExerciceAndStatut(Long provinceId, String compteCode,
                                                            Integer exercice, String statut);
}
