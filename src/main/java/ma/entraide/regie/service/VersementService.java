package ma.entraide.regie.service;

import ma.entraide.regie.dto.VersementRequest;
import ma.entraide.regie.dto.VersementResponse;
import ma.entraide.regie.entity.JournalOperation;
import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.entity.Versement;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.PlafondRegieRepository;
import ma.entraide.regie.repository.VersementRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class VersementService {

    private final VersementRepository versementRepository;
    private final PlafondRegieRepository plafondRepository;
    private final JournalService journal;
    private final CurrentUserScope scope;
    private final ExerciceService exerciceService;
    private final SituationService situationService;

    public VersementService(VersementRepository versementRepository,
                            PlafondRegieRepository plafondRepository,
                            JournalService journal,
                            CurrentUserScope scope,
                            ExerciceService exerciceService,
                            SituationService situationService) {
        this.versementRepository = versementRepository;
        this.plafondRepository = plafondRepository;
        this.journal = journal;
        this.scope = scope;
        this.exerciceService = exerciceService;
        this.situationService = situationService;
    }

    @Transactional(readOnly = true)
    public List<VersementResponse> list(String email, Long plafondId, Integer exercice) {
        User user = scope.load(email);
        exerciceService.checkViewable(user, exercice);
        List<Versement> versements;
        if (plafondId != null) {
            PlafondRegie plafond = findPlafond(plafondId);
            if (!scope.canRead(user, plafond)) {
                throw new AccessDeniedException("Acces refuse a cette regie");
            }
            versements = versementRepository.findByPlafondIdOrderByDateVersementDescIdDesc(plafondId);
        } else {
            versements = switch (user.getRole()) {
                case ADMIN, ADMIN_VIEW -> versementRepository.findAllByOrderByDateVersementDescIdDesc();
                case REGION, VIEW_REGION -> user.getRegion() == null ? List.of()
                        : versementRepository.findByProvinceRegionIdOrderByDateVersementDescIdDesc(user.getRegion().getId());
                case PROV -> user.getProvince() == null ? List.of()
                        : versementRepository.findByProvinceIdOrderByDateVersementDescIdDesc(user.getProvince().getId());
                default -> throw new AccessDeniedException("Acces refuse");
            };
        }
        Map<Long, Integer> years = exerciceService.currentYears();
        return versements.stream()
                .filter(v -> exerciceService.matches(v.getExercice(), v.getProvince().getId(), exercice, years))
                .map(v -> VersementResponse.from(v).withCloture(
                        v.getExercice() != null && v.getExercice() < exerciceService.currentYear(years, v.getProvince().getId())))
                .toList();
    }

    /**
     * Records a versement or an annulation. Both follow the same principle: the amount is limited to
     * the solde of the regie and leaves its encaissement.
     */
    @Transactional
    public VersementResponse create(Long plafondId, VersementRequest request, String email) {
        User user = scope.load(email);
        PlafondRegie plafond = findPlafond(plafondId);
        if (!scope.canWrite(user) || !scope.canRead(user, plafond)) {
            throw new AccessDeniedException("Vous ne pouvez pas enregistrer de versement pour cette regie");
        }
        String type = request.type() == null ? "VERSEMENT" : request.type();
        BigDecimal montant = request.montant();
        BigDecimal solde = situationService.soldeCourant(plafond);
        if (montant.compareTo(solde) > 0) {
            throw new IllegalArgumentException(
                    "Le montant depasse le solde de la regie. Solde disponible: " + solde);
        }
        int exercice = exerciceService.currentYear(plafond.getProvince().getId());
        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();
        plafond.setPlafondEncaissement(ancienEncaissement.subtract(montant));
        plafondRepository.save(plafond);

        Versement versement = new Versement();
        versement.setPlafond(plafond);
        versement.setProvince(plafond.getProvince());
        versement.setCompteCode(plafond.getCompteCode());
        versement.setMontant(montant);
        versement.setType(type);
        versement.setExercice(exercice);
        versement.setDateVersement(request.dateVersement() != null ? request.dateVersement() : LocalDate.now());
        versement.setReference(request.reference());
        versement.setCommentaire(request.commentaire());
        versement.setCreatedBy(email);
        Versement saved = versementRepository.save(versement);

        JournalOperation j = journal.entry("VERSEMENT", type, saved.getId(), plafond.getProvince(), plafond, exercice);
        j.setMontant(montant);
        j.setAncienEncaissement(ancienEncaissement);
        j.setNouveauEncaissement(plafond.getPlafondEncaissement());
        j.setOp(request.reference());
        j.setCommentaire(request.commentaire());
        journal.save(j, new JournalService.Diff()
                .set("Type", null, type)
                .set("Montant", null, montant)
                .set("Date", null, saved.getDateVersement())
                .set("Solde avant", solde, solde.subtract(montant))
                .set("Encaissement", ancienEncaissement, plafond.getPlafondEncaissement()));
        return VersementResponse.from(saved);
    }

    /** Deletes a versement of the current exercice and puts the amount back in the encaissement. */
    @Transactional
    public void delete(Long versementId, String email) {
        User user = scope.load(email);
        Versement versement = versementRepository.findById(versementId)
                .orElseThrow(() -> new ResourceNotFoundException("Versement not found with id: " + versementId));
        PlafondRegie plafond = versement.getPlafond();
        if (!scope.canWrite(user) || !scope.canRead(user, plafond)) {
            throw new AccessDeniedException("Vous ne pouvez pas supprimer ce versement");
        }
        exerciceService.assertEditable(versement.getProvince().getId(), versement.getExercice());

        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();
        plafond.setPlafondEncaissement(ancienEncaissement.add(versement.getMontant()));
        plafondRepository.save(plafond);
        versementRepository.delete(versement);

        JournalOperation j = journal.entry("VERSEMENT", "SUPPRESSION_VERSEMENT", versementId,
                plafond.getProvince(), plafond, versement.getExercice());
        j.setMontant(versement.getMontant());
        j.setAncienEncaissement(ancienEncaissement);
        j.setNouveauEncaissement(plafond.getPlafondEncaissement());
        j.setOp(versement.getReference());
        j.setCommentaire("Suppression du " + versement.getType().toLowerCase() + " #" + versementId);
        journal.save(j, new JournalService.Diff()
                .set("Type", versement.getType(), null)
                .set("Montant", versement.getMontant(), null)
                .set("Date", versement.getDateVersement(), null)
                .set("Encaissement", ancienEncaissement, plafond.getPlafondEncaissement()));
    }

    private PlafondRegie findPlafond(Long id) {
        return plafondRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));
    }
}
