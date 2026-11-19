package ma.entraide.regie.service;

import ma.entraide.regie.dto.CloturePrecheckResponse;
import ma.entraide.regie.dto.ExerciceInfoResponse;
import ma.entraide.regie.entity.*;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Closing of an exercice, province by province. */
@Service
public class ClotureService {

    private final ProvinceRepository provinceRepository;
    private final PlafondRegieRepository plafondRepository;
    private final PlafondExerciceRepository plafondExerciceRepository;
    private final ExerciceProvinceRepository exerciceRepository;
    private final TransactionRegieRepository transactionRepository;
    private final SituationService situationService;
    private final ExerciceService exerciceService;
    private final JournalService journal;
    private final CurrentUserScope scope;

    public ClotureService(ProvinceRepository provinceRepository,
                          PlafondRegieRepository plafondRepository,
                          PlafondExerciceRepository plafondExerciceRepository,
                          ExerciceProvinceRepository exerciceRepository,
                          TransactionRegieRepository transactionRepository,
                          SituationService situationService,
                          ExerciceService exerciceService,
                          JournalService journal,
                          CurrentUserScope scope) {
        this.provinceRepository = provinceRepository;
        this.plafondRepository = plafondRepository;
        this.plafondExerciceRepository = plafondExerciceRepository;
        this.exerciceRepository = exerciceRepository;
        this.transactionRepository = transactionRepository;
        this.situationService = situationService;
        this.exerciceService = exerciceService;
        this.journal = journal;
        this.scope = scope;
    }

    @Transactional(readOnly = true)
    public ExerciceInfoResponse info() {
        User user = scope.current();
        Map<Long, Integer> years = exerciceService.currentYears();
        List<ExerciceInfoResponse.ProvinceExercice> provinces = new ArrayList<>();
        TreeSet<Integer> annees = new TreeSet<>();
        int min = Integer.MAX_VALUE;
        for (Province p : provinceRepository.findAll()) {
            if (!scope.canReadProvince(user, p)) continue;
            int current = exerciceService.currentYear(years, p.getId());
            List<ExerciceInfoResponse.Cloture> clotures = exerciceService.closures(p.getId()).stream()
                    .map(c -> new ExerciceInfoResponse.Cloture(c.getAnnee(), c.getCloturePar(), c.getClotureLe()))
                    .toList();
            provinces.add(new ExerciceInfoResponse.ProvinceExercice(
                    p.getId(), p.getName(), p.getRegion() != null ? p.getRegion().getId() : null, current, clotures));
            annees.add(current);
            if (scope.canViewClosedExercices(user)) {
                clotures.forEach(c -> annees.add(c.annee()));
            }
            min = Math.min(min, current);
        }
        if (annees.isEmpty()) {
            annees.add(exerciceService.initialYear());
            min = exerciceService.initialYear();
        }
        boolean canView = scope.canViewClosedExercices(user);
        // Non-viewers only keep their own current exercice
        List<Integer> selectable = canView ? new ArrayList<>(annees).reversed() : List.of(min);
        return new ExerciceInfoResponse(min, selectable, canView, user.getRole() == Role.ADMIN, provinces);
    }

    @Transactional(readOnly = true)
    public CloturePrecheckResponse precheck(Long provinceId, Integer annee) {
        requireAdmin();
        Province province = findProvince(provinceId);
        return new CloturePrecheckResponse(provinceId, province.getName(), annee,
                blocages(province, annee).isEmpty(), blocages(province, annee));
    }

    @Transactional
    public CloturePrecheckResponse cloturer(Long provinceId, Integer annee) {
        User admin = requireAdmin();
        Province province = findProvince(provinceId);
        List<String> blocages = blocages(province, annee);
        if (!blocages.isEmpty()) {
            throw new IllegalArgumentException("Cloture impossible: " + String.join(" ; ", blocages));
        }

        List<PlafondRegie> plafonds = plafondRepository.findByProvinceId(provinceId);
        LocalDateTime now = LocalDateTime.now();
        for (PlafondRegie p : plafonds) {
            // 1. freeze the closing values
            PlafondExercice snap = new PlafondExercice();
            snap.setPlafondId(p.getId());
            snap.setProvinceId(provinceId);
            snap.setProvinceName(province.getName());
            snap.setCompteCode(p.getCompteCode());
            snap.setLibelle(p.getLibelle());
            snap.setAnnee(annee);
            snap.setPlafondAnnuel(p.getPlafondAnnuel());
            snap.setBudgetAnnuelInitial(p.getBudgetAnnuelInitial());
            snap.setPlafondEncaissement(p.getPlafondEncaissement());
            snap.setPlafondMaxFacture(p.getPlafondMaxFacture());
            snap.setCreeLe(now);
            plafondExerciceRepository.save(snap);

            // 2. budget of the new exercice: disponible rubrique = plafond, encaissement = 0
            BigDecimal ancienAnnuel = p.getPlafondAnnuel();
            BigDecimal ancienEncaissement = p.getPlafondEncaissement();
            if (p.getBudgetAnnuelInitial() != null) {
                p.setPlafondAnnuel(p.getBudgetAnnuelInitial());
            }
            p.setPlafondEncaissement(BigDecimal.ZERO);
            plafondRepository.save(p);

            JournalOperation j = journal.entry("PLAFOND", "CLOTURE_EXERCICE", p.getId(), province, p, annee);
            j.setAncienPlafondAnnuel(ancienAnnuel);
            j.setNouveauPlafondAnnuel(p.getPlafondAnnuel());
            j.setAncienEncaissement(ancienEncaissement);
            j.setNouveauEncaissement(p.getPlafondEncaissement());
            j.setCommentaire("Report vers l'exercice " + (annee + 1));
            journal.save(j, new JournalService.Diff()
                    .add("Disponible rubrique (plafond annuel)", ancienAnnuel, p.getPlafondAnnuel())
                    .add("Encaissement", ancienEncaissement, p.getPlafondEncaissement()));
        }

        // 3. mark the exercice closed -> the province moves on to annee + 1
        ExerciceProvince closed = new ExerciceProvince();
        closed.setProvinceId(provinceId);
        closed.setProvinceName(province.getName());
        closed.setAnnee(annee);
        closed.setStatut("CLOTURE");
        closed.setCloturePar(admin.getEmail());
        closed.setClotureLe(now);
        exerciceRepository.save(closed);

        JournalOperation j = journal.entry("EXERCICE", "CLOTURE", closed.getId(), province, null, annee);
        j.setCommentaire("Cloture de l'exercice " + annee + " - " + province.getName()
                + " (" + plafonds.size() + " regie(s))");
        journal.save(j, new JournalService.Diff()
                .set("Exercice", annee + " (ouvert)", annee + " (cloture)")
                .set("Exercice courant", annee, annee + 1));

        return new CloturePrecheckResponse(provinceId, province.getName(), annee, true, List.of());
    }

    private List<String> blocages(Province province, int annee) {
        List<String> b = new ArrayList<>();
        int current = exerciceService.currentYear(province.getId());
        if (annee != current) {
            b.add("seul l'exercice courant (" + current + ") peut etre cloture");
            return b;
        }
        for (PlafondRegie p : plafondRepository.findByProvinceId(province.getId())) {
            BigDecimal solde = situationService.soldeCourant(p);
            if (solde.signum() != 0) {
                b.add("compte " + p.getCompteCode() + ": solde non nul (" + solde + " DH)");
            }
            long enAttente = transactionRepository.countByProvinceIdAndCompteCodeAndExerciceAndStatut(
                    province.getId(), p.getCompteCode(), annee, "EN_ATTENTE");
            if (enAttente > 0) {
                b.add("compte " + p.getCompteCode() + ": " + enAttente + " depense(s) en attente");
            }
        }
        return b;
    }

    private User requireAdmin() {
        User user = scope.current();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seul l'administrateur peut cloturer un exercice");
        }
        return user;
    }

    private Province findProvince(Long id) {
        return provinceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + id));
    }
}
