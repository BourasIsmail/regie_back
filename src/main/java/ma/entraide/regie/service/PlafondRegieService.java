package ma.entraide.regie.service;

import ma.entraide.regie.dto.AlimentationRequest;
import ma.entraide.regie.dto.PlafondRegieRequest;
import ma.entraide.regie.dto.PlafondRegieResponse;
import ma.entraide.regie.entity.HistoriqueAlimentation;
import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.Province;
import ma.entraide.regie.exception.DuplicateResourceException;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.HistoriqueAlimentationRepository;
import ma.entraide.regie.repository.PlafondRegieRepository;
import ma.entraide.regie.repository.ProvinceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlafondRegieService {

    private final PlafondRegieRepository plafondRegieRepository;
    private final ProvinceRepository provinceRepository;
    private final HistoriqueAlimentationRepository historiqueRepository;

    public PlafondRegieService(PlafondRegieRepository plafondRegieRepository,
                               ProvinceRepository provinceRepository,
                               HistoriqueAlimentationRepository historiqueRepository) {
        this.plafondRegieRepository = plafondRegieRepository;
        this.provinceRepository = provinceRepository;
        this.historiqueRepository = historiqueRepository;
    }

    public List<PlafondRegieResponse> getAll() {
        return plafondRegieRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PlafondRegieResponse getById(Long id) {
        PlafondRegie plafond = plafondRegieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));
        return toResponse(plafond);
    }

    public List<PlafondRegieResponse> getByProvinceId(Long provinceId) {
        if (!provinceRepository.existsById(provinceId)) {
            throw new ResourceNotFoundException("Province not found with id: " + provinceId);
        }
        return plafondRegieRepository.findByProvinceId(provinceId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PlafondRegieResponse> getByRegionId(Long regionId) {
        return plafondRegieRepository.findByProvinceRegionId(regionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
        plafond.setPlafondEncaissement(request.getPlafondEncaissement());
        plafond.setPlafondMaxFacture(request.getPlafondMaxFacture());

        PlafondRegie saved = plafondRegieRepository.save(plafond);
        return toResponse(saved);
    }

    @Transactional
    public PlafondRegieResponse update(Long id, PlafondRegieRequest request) {
        PlafondRegie plafond = plafondRegieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));

        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + request.getProvinceId()));

        plafond.setProvince(province);
        plafond.setCompteCode(request.getCompteCode());
        plafond.setLibelle(request.getLibelle());
        plafond.setPlafondAnnuel(request.getPlafondAnnuel());
        plafond.setPlafondEncaissement(request.getPlafondEncaissement());
        plafond.setPlafondMaxFacture(request.getPlafondMaxFacture());

        PlafondRegie updated = plafondRegieRepository.save(plafond);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!plafondRegieRepository.existsById(id)) {
            throw new ResourceNotFoundException("Plafond not found with id: " + id);
        }
        plafondRegieRepository.deleteById(id);
    }

    /**
     * Alimenter (top-up) a plafond: transfers from plafondAnnuel to plafondEncaissement.
     * - plafondAnnuel decreases (disponible annuel restant)
     * - plafondEncaissement increases (argent en caisse)
     * Logs the operation in historique.
     */
    @Transactional
    public PlafondRegieResponse alimenter(Long id, AlimentationRequest request, String performedBy) {
        PlafondRegie plafond = plafondRegieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found with id: " + id));

        BigDecimal montant = request.getMontant();

        // Validate: montant cannot exceed plafondAnnuel (disponible annuel)
        if (montant.compareTo(plafond.getPlafondAnnuel()) > 0) {
            throw new IllegalArgumentException(
                    "Montant d'alimentation depasse le disponible annuel. Disponible: " + plafond.getPlafondAnnuel());
        }

        // Snapshot before
        BigDecimal ancienAnnuel = plafond.getPlafondAnnuel();
        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();

        // Apply alimentation: transfer from annuel to encaissement
        plafond.setPlafondAnnuel(plafond.getPlafondAnnuel().subtract(montant));
        plafond.setPlafondEncaissement(plafond.getPlafondEncaissement().add(montant));
        PlafondRegie updated = plafondRegieRepository.save(plafond);

        // Log to historique
        HistoriqueAlimentation historique = new HistoriqueAlimentation();
        historique.setPlafond(updated);
        historique.setProvince(updated.getProvince());
        historique.setMontantAlimentation(montant);
        historique.setAncienPlafondAnnuel(ancienAnnuel);
        historique.setNouveauPlafondAnnuel(updated.getPlafondAnnuel());
        historique.setAncienEncaissement(ancienEncaissement);
        historique.setNouveauEncaissement(updated.getPlafondEncaissement());
        historique.setTypeOperation("ALIMENTATION");
        historique.setOp(request.getOp());
        historique.setDateOp(request.getDateOp());
        historique.setNumCheque(request.getNumCheque());
        historique.setDateCheque(request.getDateCheque());
        historique.setCommentaire(request.getCommentaire());
        historique.setCreatedBy(performedBy);
        historiqueRepository.save(historique);

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
                plafond.getPlafondEncaissement(),
                plafond.getPlafondMaxFacture()
        );
    }
}
