package ma.entraide.regie.service;

import ma.entraide.regie.dto.VersementRequest;
import ma.entraide.regie.dto.VersementResponse;
import ma.entraide.regie.entity.HistoriqueAlimentation;
import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.entity.Versement;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.HistoriqueAlimentationRepository;
import ma.entraide.regie.repository.PlafondRegieRepository;
import ma.entraide.regie.repository.VersementRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VersementService {

    private final VersementRepository versementRepository;
    private final PlafondRegieRepository plafondRepository;
    private final HistoriqueAlimentationRepository historiqueRepository;
    private final CurrentUserScope scope;

    public VersementService(VersementRepository versementRepository,
                            PlafondRegieRepository plafondRepository,
                            HistoriqueAlimentationRepository historiqueRepository,
                            CurrentUserScope scope) {
        this.versementRepository = versementRepository;
        this.plafondRepository = plafondRepository;
        this.historiqueRepository = historiqueRepository;
        this.scope = scope;
    }

    @Transactional(readOnly = true)
    public List<VersementResponse> list(String email, Long plafondId) {
        User user = scope.load(email);
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
        return versements.stream().map(VersementResponse::from).toList();
    }

    /**
     * Records a versement: the cash returned to the Direction leaves the regie's encaissement.
     */
    @Transactional
    public VersementResponse create(Long plafondId, VersementRequest request, String email) {
        User user = scope.load(email);
        PlafondRegie plafond = findPlafond(plafondId);
        if (!scope.canWrite(user) || !scope.canRead(user, plafond)) {
            throw new AccessDeniedException("Vous ne pouvez pas enregistrer de versement pour cette regie");
        }

        BigDecimal montant = request.montant();
        if (montant.compareTo(plafond.getPlafondEncaissement()) > 0) {
            throw new IllegalArgumentException(
                    "Le versement depasse l'encaissement disponible. Disponible: " + plafond.getPlafondEncaissement());
        }

        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();
        plafond.setPlafondEncaissement(ancienEncaissement.subtract(montant));
        plafondRepository.save(plafond);

        Versement versement = new Versement();
        versement.setPlafond(plafond);
        versement.setProvince(plafond.getProvince());
        versement.setCompteCode(plafond.getCompteCode());
        versement.setMontant(montant);
        versement.setDateVersement(request.dateVersement());
        versement.setReference(request.reference());
        versement.setCommentaire(request.commentaire());
        versement.setCreatedBy(email);
        Versement saved = versementRepository.save(versement);

        logHistorique(plafond, montant, ancienEncaissement, "VERSEMENT",
                request.commentaire(), request.reference(), email);
        return VersementResponse.from(saved);
    }

    /** Cancels a versement and puts the amount back in the regie's encaissement. */
    @Transactional
    public void delete(Long versementId, String email) {
        User user = scope.load(email);
        Versement versement = versementRepository.findById(versementId)
                .orElseThrow(() -> new ResourceNotFoundException("Versement not found with id: " + versementId));
        PlafondRegie plafond = versement.getPlafond();
        if (!scope.canWrite(user) || !scope.canRead(user, plafond)) {
            throw new AccessDeniedException("Vous ne pouvez pas annuler ce versement");
        }

        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();
        plafond.setPlafondEncaissement(ancienEncaissement.add(versement.getMontant()));
        plafondRepository.save(plafond);
        versementRepository.delete(versement);

        logHistorique(plafond, versement.getMontant(), ancienEncaissement, "ANNULATION_VERSEMENT",
                "Annulation du versement #" + versementId, versement.getReference(), email);
    }

    private PlafondRegie findPlafond(Long id) {
        return plafondRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));
    }

    private void logHistorique(PlafondRegie plafond, BigDecimal montant, BigDecimal ancienEncaissement,
                               String type, String commentaire, String reference, String email) {
        HistoriqueAlimentation h = new HistoriqueAlimentation();
        h.setPlafond(plafond);
        h.setProvince(plafond.getProvince());
        h.setMontantAlimentation(montant);
        h.setAncienPlafondAnnuel(plafond.getPlafondAnnuel());
        h.setNouveauPlafondAnnuel(plafond.getPlafondAnnuel());
        h.setAncienEncaissement(ancienEncaissement);
        h.setNouveauEncaissement(plafond.getPlafondEncaissement());
        h.setTypeOperation(type);
        h.setOp(reference);
        h.setCommentaire(commentaire);
        h.setCreatedBy(email);
        historiqueRepository.save(h);
    }
}
