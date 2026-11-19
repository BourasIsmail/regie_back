package ma.entraide.regie.service;

import ma.entraide.regie.dto.JournalResponse;
import ma.entraide.regie.entity.JournalOperation;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.JournalOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Read access to the audit trail (journal), scoped by role and filtered by exercice. */
@Service
public class HistoriqueAlimentationService {

    private final JournalOperationRepository journalRepository;
    private final CurrentUserScope scope;
    private final ExerciceService exerciceService;

    public HistoriqueAlimentationService(JournalOperationRepository journalRepository,
                                         CurrentUserScope scope,
                                         ExerciceService exerciceService) {
        this.journalRepository = journalRepository;
        this.scope = scope;
        this.exerciceService = exerciceService;
    }

    @Transactional(readOnly = true)
    public List<JournalResponse> search(Integer exercice, Predicate<JournalOperation> extra) {
        User user = scope.current();
        exerciceService.checkViewable(user, exercice);
        Map<Long, Integer> years = exerciceService.currentYears();
        return journalRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .filter(j -> visible(user, j))
                .filter(j -> matchesExercice(j, exercice, years))
                .filter(extra)
                .map(JournalResponse::from)
                .toList();
    }

    public List<JournalResponse> getAll(Integer exercice) {
        return search(exercice, j -> true);
    }

    public List<JournalResponse> getByPlafondId(Long plafondId, Integer exercice) {
        return search(exercice, j -> plafondId.equals(j.getPlafondId()));
    }

    public List<JournalResponse> getByProvinceId(Long provinceId, Integer exercice) {
        return search(exercice, j -> provinceId.equals(j.getProvinceId()));
    }

    public List<JournalResponse> getByRegionId(Long regionId, Integer exercice) {
        return search(exercice, j -> regionId.equals(j.getRegionId()));
    }

    @Transactional(readOnly = true)
    public JournalResponse getById(Long id) {
        User user = scope.current();
        JournalOperation j = journalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Historique not found with id: " + id));
        if (!visible(user, j)) {
            throw new org.springframework.security.access.AccessDeniedException("Acces refuse");
        }
        return JournalResponse.from(j);
    }

    private boolean visible(User user, JournalOperation j) {
        return switch (user.getRole()) {
            case ADMIN, ADMIN_VIEW -> true;
            case REGION, VIEW_REGION -> user.getRegion() != null && user.getRegion().getId().equals(j.getRegionId());
            case PROV -> user.getProvince() != null && user.getProvince().getId().equals(j.getProvinceId());
            default -> false;
        };
    }

    private boolean matchesExercice(JournalOperation j, Integer requested, Map<Long, Integer> years) {
        if (j.getProvinceId() == null) {
            // administrative entries (users, regions...) are not tied to a province exercice
            return requested == null || j.getExercice() == null || j.getExercice().equals(requested);
        }
        return exerciceService.matches(j.getExercice(), j.getProvinceId(), requested, years);
    }
}
