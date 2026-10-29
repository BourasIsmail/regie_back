package ma.entraide.regie.service;

import ma.entraide.regie.dto.SituationResponse;
import ma.entraide.regie.dto.SituationRowResponse;
import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.repository.PlafondRegieRepository;
import ma.entraide.regie.repository.TransactionRegieRepository;
import ma.entraide.regie.repository.VersementRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Builds the "Situation des regies de depenses" statement: one line per regie (province + compte).
 * (I) plafond global, (II) avance, (III) disponible par rubrique = I - II, (IV) depenses confirmees
 * de l'exercice arretees a une date, versements, (V) solde = II - IV - versements.
 */
@Service
public class SituationService {

    private final PlafondRegieRepository plafondRepository;
    private final TransactionRegieRepository transactionRepository;
    private final VersementRepository versementRepository;
    private final CurrentUserScope scope;

    public SituationService(PlafondRegieRepository plafondRepository,
                            TransactionRegieRepository transactionRepository,
                            VersementRepository versementRepository,
                            CurrentUserScope scope) {
        this.plafondRepository = plafondRepository;
        this.transactionRepository = transactionRepository;
        this.versementRepository = versementRepository;
        this.scope = scope;
    }

    @Transactional(readOnly = true)
    public SituationResponse getSituation(String email, Integer exerciceParam, LocalDate arreteAuParam) {
        User user = scope.load(email);

        int exercice = exerciceParam != null ? exerciceParam
                : (arreteAuParam != null ? arreteAuParam.getYear() : LocalDate.now().getYear());
        LocalDate debut = LocalDate.of(exercice, 1, 1);
        LocalDate finExercice = LocalDate.of(exercice, 12, 31);
        LocalDate arreteAu = arreteAuParam != null ? arreteAuParam : LocalDate.now();
        if (arreteAu.isAfter(finExercice)) arreteAu = finExercice;

        Map<String, BigDecimal> depenses = new HashMap<>();
        if (!arreteAu.isBefore(debut)) {
            for (Object[] r : transactionRepository.sumConfirmedByRubrique(
                    debut.atStartOfDay(), arreteAu.plusDays(1).atStartOfDay())) {
                depenses.put(r[0] + "|" + r[1], (BigDecimal) r[2]);
            }
        }
        Map<Long, BigDecimal> versements = new HashMap<>();
        if (!arreteAu.isBefore(debut)) {
            for (Object[] r : versementRepository.sumByPlafond(debut, arreteAu)) {
                versements.put((Long) r[0], (BigDecimal) r[1]);
            }
        }

        List<SituationRowResponse> rows = visiblePlafonds(user).stream()
                .map(p -> toRow(p, depenses, versements))
                .sorted(Comparator.comparing(SituationRowResponse::dp, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(SituationRowResponse::compteCode))
                .toList();

        return new SituationResponse(exercice, arreteAu, rows, totals(rows));
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

    private SituationRowResponse toRow(PlafondRegie p, Map<String, BigDecimal> depenses,
                                       Map<Long, BigDecimal> versements) {
        BigDecimal plafondGlobal = p.getBudgetAnnuelInitial() != null
                ? p.getBudgetAnnuelInitial() : p.getPlafondAnnuel();
        BigDecimal avance = plafondGlobal.subtract(p.getPlafondAnnuel());
        BigDecimal dep = depenses.getOrDefault(p.getProvince().getId() + "|" + p.getCompteCode(), BigDecimal.ZERO);
        BigDecimal ver = versements.getOrDefault(p.getId(), BigDecimal.ZERO);
        return new SituationRowResponse(
                p.getId(),
                p.getProvince().getId(),
                p.getProvince().getName(),
                p.getProvince().getRegion().getId(),
                p.getProvince().getRegion().getName(),
                p.getCompteCode(),
                p.getLibelle(),
                plafondGlobal,
                avance,
                p.getPlafondAnnuel(),
                p.getPlafondEncaissement(),
                dep,
                ver,
                avance.subtract(dep).subtract(ver));
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
