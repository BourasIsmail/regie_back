package ma.entraide.regie.service;

import ma.entraide.regie.dto.SituationResponse;
import ma.entraide.regie.dto.SituationRowResponse;
import ma.entraide.regie.entity.PlafondExercice;
import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.Province;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.repository.PlafondExerciceRepository;
import ma.entraide.regie.repository.PlafondRegieRepository;
import ma.entraide.regie.repository.ProvinceRepository;
import ma.entraide.regie.repository.TransactionRegieRepository;
import ma.entraide.regie.repository.VersementRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Builds the "Situation des regies de depenses" statement: one line per regie (province + compte).
 * (I) plafond global, (II) avance, (III) disponible par rubrique = I - II, (IV) depenses confirmees
 * de l'exercice arretees a une date, versements/annulations, (V) solde = II - IV - versements.
 *
 * An open exercice is computed from the live plafonds; a closed exercice from the frozen values
 * stored at closing time.
 */
@Service
public class SituationService {

    private final PlafondRegieRepository plafondRepository;
    private final PlafondExerciceRepository plafondExerciceRepository;
    private final TransactionRegieRepository transactionRepository;
    private final VersementRepository versementRepository;
    private final ProvinceRepository provinceRepository;
    private final CurrentUserScope scope;
    private final ExerciceService exerciceService;

    public SituationService(PlafondRegieRepository plafondRepository,
                            PlafondExerciceRepository plafondExerciceRepository,
                            TransactionRegieRepository transactionRepository,
                            VersementRepository versementRepository,
                            ProvinceRepository provinceRepository,
                            CurrentUserScope scope,
                            ExerciceService exerciceService) {
        this.plafondRepository = plafondRepository;
        this.plafondExerciceRepository = plafondExerciceRepository;
        this.transactionRepository = transactionRepository;
        this.versementRepository = versementRepository;
        this.provinceRepository = provinceRepository;
        this.scope = scope;
        this.exerciceService = exerciceService;
    }

    /**
     * Single source of truth for the solde of a regie on its current exercice:
     * avance - depenses confirmees - versements/annulations.
     */
    @Transactional(readOnly = true)
    public BigDecimal soldeCourant(PlafondRegie p) {
        int exercice = exerciceService.currentYear(p.getProvince().getId());
        BigDecimal dep = transactionRepository.sumConfirmed(p.getProvince().getId(), p.getCompteCode(), exercice);
        BigDecimal ver = versementRepository.sumMontant(p.getId(), exercice);
        return avance(p.getBudgetAnnuelInitial(), p.getPlafondAnnuel()).subtract(dep).subtract(ver);
    }

    static BigDecimal avance(BigDecimal budgetInitial, BigDecimal plafondAnnuel) {
        BigDecimal global = budgetInitial != null ? budgetInitial : plafondAnnuel;
        return global.subtract(plafondAnnuel);
    }

    @Transactional(readOnly = true)
    public SituationResponse getSituation(String email, Integer exerciceParam, LocalDate arreteAuParam) {
        User user = scope.load(email);
        Map<Long, Integer> currents = exerciceService.currentYears();

        int exercice;
        if (exerciceParam != null) {
            exercice = exerciceParam;
        } else if (arreteAuParam != null) {
            exercice = arreteAuParam.getYear();
        } else {
            exercice = defaultExercice(user, currents);
        }
        exerciceService.checkViewable(user, exercice);

        LocalDate debut = LocalDate.of(exercice, 1, 1);
        LocalDate finExercice = LocalDate.of(exercice, 12, 31);
        LocalDate arreteAu = arreteAuParam != null ? arreteAuParam : LocalDate.now();
        if (arreteAu.isAfter(finExercice)) arreteAu = finExercice;
        if (arreteAu.isBefore(debut)) arreteAu = debut;

        Map<String, BigDecimal> depenses = new HashMap<>();
        for (Object[] r : transactionRepository.sumConfirmedByRubrique(exercice, arreteAu.plusDays(1).atStartOfDay())) {
            depenses.put(r[0] + "|" + r[1], (BigDecimal) r[2]);
        }
        Map<Long, BigDecimal> versements = new HashMap<>();
        for (Object[] r : versementRepository.sumByPlafond(exercice, arreteAu)) {
            versements.put((Long) r[0], (BigDecimal) r[1]);
        }

        List<SituationRowResponse> rows = new ArrayList<>();
        Map<Long, Province> provinces = new HashMap<>();
        provinceRepository.findAll().forEach(p -> provinces.put(p.getId(), p));

        // Open exercice: live plafonds of the provinces currently on this exercice
        for (PlafondRegie p : visiblePlafonds(user)) {
            if (exerciceService.currentYear(currents, p.getProvince().getId()) == exercice) {
                rows.add(toRow(p.getId(), p.getProvince(), p.getCompteCode(), p.getLibelle(),
                        p.getBudgetAnnuelInitial(), p.getPlafondAnnuel(), p.getPlafondEncaissement(),
                        depenses, versements, false));
            }
        }
        // Closed exercice: frozen values
        for (PlafondExercice s : plafondExerciceRepository.findByAnnee(exercice)) {
            Province province = provinces.get(s.getProvinceId());
            if (province == null || !scope.canReadProvince(user, province)) continue;
            if (exerciceService.currentYear(currents, s.getProvinceId()) <= exercice) continue;
            rows.add(toRow(s.getPlafondId(), province, s.getCompteCode(), s.getLibelle(),
                    s.getBudgetAnnuelInitial(), s.getPlafondAnnuel(), s.getPlafondEncaissement(),
                    depenses, versements, true));
        }

        rows.sort(Comparator.comparing(SituationRowResponse::dp, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(SituationRowResponse::compteCode));
        return new SituationResponse(exercice, arreteAu, rows, totals(rows));
    }

    /** Oldest exercice still open for at least one visible province (or the initial year). */
    private int defaultExercice(User user, Map<Long, Integer> currents) {
        int best = Integer.MAX_VALUE;
        for (PlafondRegie p : visiblePlafonds(user)) {
            best = Math.min(best, exerciceService.currentYear(currents, p.getProvince().getId()));
        }
        if (best == Integer.MAX_VALUE) {
            if (user.getProvince() != null) {
                return exerciceService.currentYear(currents, user.getProvince().getId());
            }
            return exerciceService.initialYear();
        }
        return best;
    }

    private List<PlafondRegie> visiblePlafonds(User user) {
        return switch (user.getRole()) {
            case ADMIN, ADMIN_VIEW -> plafondRepository.findAll();
            case REGION, VIEW_REGION -> user.getRegion() == null ? List.of()
                    : plafondRepository.findByProvinceRegionId(user.getRegion().getId());
            case PROV -> user.getProvince() == null ? List.of()
                    : plafondRepository.findByProvinceId(user.getProvince().getId());
            default -> throw new AccessDeniedException("Acces refuse a la situation des regies");
        };
    }

    private SituationRowResponse toRow(Long plafondId, Province province, String compteCode, String libelle,
                                       BigDecimal budgetInitial, BigDecimal plafondAnnuel, BigDecimal encaissement,
                                       Map<String, BigDecimal> depenses, Map<Long, BigDecimal> versements,
                                       boolean cloture) {
        BigDecimal plafondGlobal = budgetInitial != null ? budgetInitial : plafondAnnuel;
        BigDecimal avance = plafondGlobal.subtract(plafondAnnuel);
        BigDecimal dep = depenses.getOrDefault(province.getId() + "|" + compteCode, BigDecimal.ZERO);
        BigDecimal ver = plafondId == null ? BigDecimal.ZERO : versements.getOrDefault(plafondId, BigDecimal.ZERO);
        return new SituationRowResponse(
                plafondId,
                province.getId(),
                province.getName(),
                province.getRegion().getId(),
                province.getRegion().getName(),
                compteCode,
                libelle,
                plafondGlobal,
                avance,
                plafondAnnuel,
                encaissement,
                dep,
                ver,
                avance.subtract(dep).subtract(ver),
                cloture);
    }

    private SituationResponse.Totals totals(List<SituationRowResponse> rows) {
        return new SituationResponse.Totals(
                sum(rows, SituationRowResponse::plafondGlobal),
                sum(rows, SituationRowResponse::avance),
                sum(rows, SituationRowResponse::disponibleRubrique),
                sum(rows, SituationRowResponse::disponibleEncaisse),
                sum(rows, SituationRowResponse::depenses),
                sum(rows, SituationRowResponse::versement),
                sum(rows, SituationRowResponse::solde));
    }

    private BigDecimal sum(List<SituationRowResponse> rows, Function<SituationRowResponse, BigDecimal> f) {
        return rows.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
