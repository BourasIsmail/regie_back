package ma.entraide.regie.service;

import ma.entraide.regie.entity.ExerciceProvince;
import ma.entraide.regie.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ma.entraide.regie.repository.ExerciceProvinceRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exercice (fiscal year) rules. Each province closes its exercice independently. Live data
 * (plafonds_regie) always belongs to the province's current exercice; closed exercices are
 * read-only and only consultable by admins and regional profiles.
 */
@Service
public class ExerciceService {

    private final ExerciceProvinceRepository exerciceRepository;
    private final CurrentUserScope scope;
    private final int initialYear;

    public ExerciceService(ExerciceProvinceRepository exerciceRepository,
                           CurrentUserScope scope,
                           @Value("${app.exercice.initial:2026}") int initialYear) {
        this.exerciceRepository = exerciceRepository;
        this.scope = scope;
        this.initialYear = initialYear;
    }

    public int initialYear() {
        return initialYear;
    }

    /** provinceId -> current exercice, for provinces that closed at least one exercice. */
    public Map<Long, Integer> currentYears() {
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] r : exerciceRepository.lastClosedByProvince()) {
            map.put((Long) r[0], ((Integer) r[1]) + 1);
        }
        return map;
    }

    public int currentYear(Map<Long, Integer> years, Long provinceId) {
        return years.getOrDefault(provinceId, initialYear);
    }

    public int currentYear(Long provinceId) {
        return currentYear(currentYears(), provinceId);
    }

    public boolean isClosed(Long provinceId, int annee) {
        return exerciceRepository.existsByProvinceIdAndAnnee(provinceId, annee);
    }

    public List<ExerciceProvince> closures(Long provinceId) {
        return exerciceRepository.findByProvinceIdOrderByAnneeDesc(provinceId);
    }

    /** Throws if the record belongs to an exercice that is already closed. */
    public void assertEditable(Long provinceId, Integer recordExercice) {
        if (recordExercice != null && recordExercice < currentYear(provinceId)) {
            throw new IllegalArgumentException(
                    "L'exercice " + recordExercice + " est cloture pour cette province: modification impossible");
        }
    }

    /** Consulting an exercice other than the current one is limited to admins / regional profiles. */
    public void checkViewable(User user, Integer requested) {
        if (requested == null) return;
        if (scope.canViewClosedExercices(user)) return;
        Long provinceId = user.getProvince() != null ? user.getProvince().getId() : null;
        if (provinceId == null || requested != currentYear(provinceId)) {
            throw new AccessDeniedException("La consultation des exercices clotures est reservee a l'administration");
        }
    }

    /** True when a record (with this exercice) should be shown for the requested exercice. */
    public boolean matches(Integer recordExercice, Long provinceId, Integer requested, Map<Long, Integer> years) {
        int current = currentYear(years, provinceId);
        int wanted = requested != null ? requested : current;
        int actual = recordExercice != null ? recordExercice : current;
        return actual == wanted;
    }
}
