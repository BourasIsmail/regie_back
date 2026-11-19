package ma.entraide.regie.service;

import ma.entraide.regie.dto.AlimentationRequest;
import ma.entraide.regie.dto.PlafondRegieRequest;
import ma.entraide.regie.dto.PlafondRegieResponse;
import ma.entraide.regie.entity.JournalOperation;
import ma.entraide.regie.entity.PlafondExercice;
import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.Province;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.exception.DuplicateResourceException;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.PlafondExerciceRepository;
import ma.entraide.regie.repository.PlafondRegieRepository;
import ma.entraide.regie.repository.ProvinceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Service
public class PlafondRegieService {

    private final PlafondRegieRepository plafondRegieRepository;
    private final ProvinceRepository provinceRepository;
    private final PlafondExerciceRepository plafondExerciceRepository;
    private final JournalService journal;
    private final CurrentUserScope scope;
    private final ExerciceService exerciceService;

    public PlafondRegieService(PlafondRegieRepository plafondRegieRepository,
                               ProvinceRepository provinceRepository,
                               PlafondExerciceRepository plafondExerciceRepository,
                               JournalService journal,
                               CurrentUserScope scope,
                               ExerciceService exerciceService) {
        this.plafondRegieRepository = plafondRegieRepository;
        this.provinceRepository = provinceRepository;
        this.plafondExerciceRepository = plafondExerciceRepository;
        this.journal = journal;
        this.scope = scope;
        this.exerciceService = exerciceService;
    }

    /**
     * Plafonds visible to the current user for an exercice (default: each province's current one).
     * A closed exercice is served from the frozen snapshots.
     */
    @Transactional(readOnly = true)
    public List<PlafondRegieResponse> list(Integer exercice, Predicate<Province> provinceFilter) {
        User user = scope.current();
        exerciceService.checkViewable(user, exercice);
        Map<Long, Integer> years = exerciceService.currentYears();
        List<PlafondRegieResponse> result = new ArrayList<>();

        for (PlafondRegie p : plafondRegieRepository.findAll()) {
            Province province = p.getProvince();
            if (!scope.canReadProvince(user, province) || !provinceFilter.test(province)) continue;
            int current = exerciceService.currentYear(years, province.getId());
            if (exercice == null || exercice == current) {
                PlafondRegieResponse r = toResponse(p);
                r.setExercice(current);
                result.add(r);
            }
        }
        if (exercice != null) {
            Map<Long, Province> provinces = new HashMap<>();
            provinceRepository.findAll().forEach(pr -> provinces.put(pr.getId(), pr));
            for (PlafondExercice s : plafondExerciceRepository.findByAnnee(exercice)) {
                Province province = provinces.get(s.getProvinceId());
                if (province == null || !scope.canReadProvince(user, province) || !provinceFilter.test(province)) continue;
                if (exerciceService.currentYear(years, province.getId()) <= exercice) continue;
                PlafondRegieResponse r = new PlafondRegieResponse(s.getPlafondId(), province.getId(), province.getName(),
                        s.getCompteCode(), s.getLibelle(), s.getPlafondAnnuel(), s.getBudgetAnnuelInitial(),
                        s.getPlafondEncaissement(), s.getPlafondMaxFacture());
                r.setExercice(exercice);
                r.setCloture(true);
                result.add(r);
            }
        }
        return result;
    }

    public List<PlafondRegieResponse> getAll(Integer exercice) {
        return list(exercice, p -> true);
    }

    @Transactional(readOnly = true)
    public PlafondRegieResponse getById(Long id) {
        PlafondRegie plafond = plafondRegieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));
        return toResponse(plafond);
    }

    public List<PlafondRegieResponse> getByProvinceId(Long provinceId, Integer exercice) {
        if (!provinceRepository.existsById(provinceId)) {
            throw new ResourceNotFoundException("Province not found with id: " + provinceId);
        }
        return list(exercice, p -> p.getId().equals(provinceId));
    }

    public List<PlafondRegieResponse> getByRegionId(Long regionId, Integer exercice) {
        return list(exercice, p -> p.getRegion() != null && p.getRegion().getId().equals(regionId));
    }

    @Transactional
    public PlafondRegieResponse create(PlafondRegieRequest request) {
        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + request.getProvinceId()));

        plafondRegieRepository.findByProvinceIdAndCompteCode(request.getProvinceId(), request.getCompteCode())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Plafond already exists for province " + request.getProvinceId()
                                    + " and compte code " + request.getCompteCode());
                });

        PlafondRegie plafond = new PlafondRegie();
        plafond.setProvince(province);
        plafond.setCompteCode(request.getCompteCode());
        plafond.setLibelle(request.getLibelle());
        plafond.setPlafondAnnuel(request.getPlafondAnnuel());
        plafond.setBudgetAnnuelInitial(
                request.getBudgetAnnuelInitial() != null
                        ? request.getBudgetAnnuelInitial()
                        : request.getPlafondAnnuel()
        );
        plafond.setPlafondEncaissement(request.getPlafondEncaissement());
        plafond.setPlafondMaxFacture(request.getPlafondMaxFacture());

        PlafondRegie saved = plafondRegieRepository.save(plafond);

        JournalOperation j = journal.entry("PLAFOND", "CREATION", saved.getId(), province, saved,
                exerciceService.currentYear(province.getId()));
        j.setNouveauPlafondAnnuel(saved.getPlafondAnnuel());
        j.setNouveauEncaissement(saved.getPlafondEncaissement());
        j.setCommentaire("Creation du plafond " + saved.getCompteCode());
        journal.save(j, new JournalService.Diff()
                .set("Compte", null, saved.getCompteCode())
                .set("Libelle", null, saved.getLibelle())
                .set("Disponible rubrique (plafond annuel)", null, saved.getPlafondAnnuel())
                .set("Budget annuel initial", null, saved.getBudgetAnnuelInitial())
                .set("Encaissement", null, saved.getPlafondEncaissement())
                .set("Plafond max facture", null, saved.getPlafondMaxFacture()));
        return toResponse(saved);
    }

    @Transactional
    public PlafondRegieResponse update(Long id, PlafondRegieRequest request) {
        PlafondRegie plafond = plafondRegieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));
        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + request.getProvinceId()));

        JournalService.Diff diff = new JournalService.Diff()
                .add("Province", plafond.getProvince().getName(), province.getName())
                .add("Compte", plafond.getCompteCode(), request.getCompteCode())
                .add("Libelle", plafond.getLibelle(), request.getLibelle())
                .add("Disponible rubrique (plafond annuel)", plafond.getPlafondAnnuel(), request.getPlafondAnnuel())
                .add("Encaissement", plafond.getPlafondEncaissement(), request.getPlafondEncaissement())
                .add("Plafond max facture", plafond.getPlafondMaxFacture(), request.getPlafondMaxFacture());
        if (request.getBudgetAnnuelInitial() != null) {
            diff.add("Budget annuel initial", plafond.getBudgetAnnuelInitial(), request.getBudgetAnnuelInitial());
        }
        BigDecimal ancienAnnuel = plafond.getPlafondAnnuel();
        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();

        plafond.setProvince(province);
        plafond.setCompteCode(request.getCompteCode());
        plafond.setLibelle(request.getLibelle());
        plafond.setPlafondAnnuel(request.getPlafondAnnuel());
        if (request.getBudgetAnnuelInitial() != null) {
            plafond.setBudgetAnnuelInitial(request.getBudgetAnnuelInitial());
        }
        plafond.setPlafondEncaissement(request.getPlafondEncaissement());
        plafond.setPlafondMaxFacture(request.getPlafondMaxFacture());

        PlafondRegie updated = plafondRegieRepository.save(plafond);

        JournalOperation j = journal.entry("PLAFOND", "MODIFICATION", id, province, updated,
                exerciceService.currentYear(province.getId()));
        j.setAncienPlafondAnnuel(ancienAnnuel);
        j.setNouveauPlafondAnnuel(updated.getPlafondAnnuel());
        j.setAncienEncaissement(ancienEncaissement);
        j.setNouveauEncaissement(updated.getPlafondEncaissement());
        j.setCommentaire("Modification du plafond " + updated.getCompteCode());
        journal.save(j, diff);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        PlafondRegie plafond = plafondRegieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));
        JournalOperation j = journal.entry("PLAFOND", "SUPPRESSION", id, plafond.getProvince(), plafond,
                exerciceService.currentYear(plafond.getProvince().getId()));
        j.setAncienPlafondAnnuel(plafond.getPlafondAnnuel());
        j.setAncienEncaissement(plafond.getPlafondEncaissement());
        j.setCommentaire("Suppression du plafond " + plafond.getCompteCode());
        JournalService.Diff diff = new JournalService.Diff()
                .set("Compte", plafond.getCompteCode(), null)
                .set("Libelle", plafond.getLibelle(), null)
                .set("Disponible rubrique (plafond annuel)", plafond.getPlafondAnnuel(), null)
                .set("Budget annuel initial", plafond.getBudgetAnnuelInitial(), null)
                .set("Encaissement", plafond.getPlafondEncaissement(), null)
                .set("Plafond max facture", plafond.getPlafondMaxFacture(), null);
        plafondRegieRepository.deleteById(id);
        journal.save(j, diff);
    }

    /**
     * Alimenter (top-up) a plafond: transfers from plafondAnnuel to plafondEncaissement.
     */
    @Transactional
    public PlafondRegieResponse alimenter(Long id, AlimentationRequest request, String performedBy) {
        PlafondRegie plafond = plafondRegieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));

        BigDecimal montant = request.getMontant();
        if (montant.compareTo(plafond.getPlafondAnnuel()) > 0) {
            throw new IllegalArgumentException(
                    "Montant d'alimentation depasse le disponible annuel. Disponible: " + plafond.getPlafondAnnuel());
        }

        BigDecimal ancienAnnuel = plafond.getPlafondAnnuel();
        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();

        plafond.setPlafondAnnuel(plafond.getPlafondAnnuel().subtract(montant));
        plafond.setPlafondEncaissement(plafond.getPlafondEncaissement().add(montant));
        PlafondRegie updated = plafondRegieRepository.save(plafond);

        JournalOperation j = journal.entry("PLAFOND", "ALIMENTATION", id, updated.getProvince(), updated,
                exerciceService.currentYear(updated.getProvince().getId()));
        j.setMontant(montant);
        j.setAncienPlafondAnnuel(ancienAnnuel);
        j.setNouveauPlafondAnnuel(updated.getPlafondAnnuel());
        j.setAncienEncaissement(ancienEncaissement);
        j.setNouveauEncaissement(updated.getPlafondEncaissement());
        j.setOp(request.getOp());
        j.setDateOp(request.getDateOp());
        j.setNumCheque(request.getNumCheque());
        j.setDateCheque(request.getDateCheque());
        j.setCommentaire(request.getCommentaire());
        journal.save(j, new JournalService.Diff()
                .set("Montant alimente", null, montant)
                .set("Disponible rubrique", ancienAnnuel, updated.getPlafondAnnuel())
                .set("Encaissement", ancienEncaissement, updated.getPlafondEncaissement()));
        return toResponse(updated);
    }

    /**
     * Corriger (adjust) the encaissement value in case of a data entry error.
     */
    @Transactional
    public PlafondRegieResponse corriger(Long id, BigDecimal nouveauEncaissement, String motif, String performedBy) {
        PlafondRegie plafond = plafondRegieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));

        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();
        BigDecimal difference = nouveauEncaissement.subtract(ancienEncaissement);

        if (nouveauEncaissement.compareTo(ancienEncaissement) > 0) {
            throw new IllegalArgumentException(
                    "La correction ne peut pas augmenter l'encaissement. Montant actuel: " + ancienEncaissement
                            + " DH. Vous ne pouvez corriger que vers le bas pour rectifier une erreur de saisie.");
        }

        BigDecimal ancienAnnuel = plafond.getPlafondAnnuel();
        plafond.setPlafondEncaissement(nouveauEncaissement);
        plafond.setPlafondAnnuel(plafond.getPlafondAnnuel().subtract(difference));
        PlafondRegie updated = plafondRegieRepository.save(plafond);

        JournalOperation j = journal.entry("PLAFOND", "CORRECTION", id, updated.getProvince(), updated,
                exerciceService.currentYear(updated.getProvince().getId()));
        j.setMontant(difference.abs());
        j.setAncienPlafondAnnuel(ancienAnnuel);
        j.setNouveauPlafondAnnuel(updated.getPlafondAnnuel());
        j.setAncienEncaissement(ancienEncaissement);
        j.setNouveauEncaissement(updated.getPlafondEncaissement());
        j.setCommentaire("Correction: " + motif);
        journal.save(j, new JournalService.Diff()
                .set("Encaissement", ancienEncaissement, updated.getPlafondEncaissement())
                .set("Disponible rubrique", ancienAnnuel, updated.getPlafondAnnuel())
                .set("Motif", null, motif));
        return toResponse(updated);
    }

    private PlafondRegieResponse toResponse(PlafondRegie plafond) {
        return new PlafondRegieResponse(
                plafond.getId(),
                plafond.getProvince().getId(),
                plafond.getProvince().getName(),
                plafond.getCompteCode(),
                plafond.getLibelle(),
                plafond.getPlafondAnnuel(),
                plafond.getBudgetAnnuelInitial(),
                plafond.getPlafondEncaissement(),
                plafond.getPlafondMaxFacture()
        );
    }
}
