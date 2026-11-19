package ma.entraide.regie.repository;

import ma.entraide.regie.entity.JournalOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalOperationRepository extends JpaRepository<JournalOperation, Long> {

    List<JournalOperation> findAllByOrderByCreatedAtDescIdDesc();

    List<JournalOperation> findByPlafondIdOrderByCreatedAtDescIdDesc(Long plafondId);
}
