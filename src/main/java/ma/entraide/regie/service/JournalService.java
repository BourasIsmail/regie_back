package ma.entraide.regie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.entraide.regie.entity.JournalOperation;
import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.Province;
import ma.entraide.regie.entity.Region;
import ma.entraide.regie.repository.JournalOperationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Writes the audit trail. Every money-related or administrative operation goes through here. */
@Service
public class JournalService {

    private final JournalOperationRepository repository;
    private final ObjectMapper objectMapper;

    public JournalService(JournalOperationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Collects the fields that changed (before / after). */
    public static class Diff {
        private final List<Map<String, Object>> items = new ArrayList<>();

        public Diff add(String champ, Object avant, Object apres) {
            if (same(avant, apres)) return this;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("champ", champ);
            m.put("avant", avant == null ? null : avant.toString());
            m.put("apres", apres == null ? null : apres.toString());
            items.add(m);
            return this;
        }

        /** Records a field unconditionally (creation / deletion snapshots). */
        public Diff set(String champ, Object avant, Object apres) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("champ", champ);
            m.put("avant", avant == null ? null : avant.toString());
            m.put("apres", apres == null ? null : apres.toString());
            items.add(m);
            return this;
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        private static boolean same(Object a, Object b) {
            if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
            return Objects.equals(a, b);
        }
    }

    public String actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null ? auth.getName() : "system";
    }

    /** Creates an entry pre-filled with the context; the caller completes it and calls save(). */
    public JournalOperation entry(String entite, String action, Long entiteId,
                                  Province province, PlafondRegie plafond, Integer exercice) {
        JournalOperation j = new JournalOperation();
        j.setEntite(entite);
        j.setAction(action);
        j.setEntiteId(entiteId);
        j.setExercice(exercice);
        if (province != null) {
            j.setProvinceId(province.getId());
            j.setProvinceName(province.getName());
            Region region = province.getRegion();
            if (region != null) {
                j.setRegionId(region.getId());
                j.setRegionName(region.getName());
            }
        }
        if (plafond != null) {
            j.setPlafondId(plafond.getId());
            j.setCompteCode(plafond.getCompteCode());
            j.setLibelle(plafond.getLibelle());
        }
        j.setCreatedBy(actor());
        return j;
    }

    public JournalOperation save(JournalOperation j, Diff diff) {
        if (diff != null && !diff.isEmpty()) {
            try {
                j.setDetails(objectMapper.writeValueAsString(diff.items));
            } catch (JsonProcessingException e) {
                j.setDetails(null);
            }
        }
        if (j.getCommentaire() != null && j.getCommentaire().length() > 1000) {
            j.setCommentaire(j.getCommentaire().substring(0, 1000));
        }
        return repository.save(j);
    }
}
