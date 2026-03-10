package ma.entraide.regie.service;

import ma.entraide.regie.dto.HistoriqueAlimentationResponse;
import ma.entraide.regie.entity.HistoriqueAlimentation;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.HistoriqueAlimentationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoriqueAlimentationService {

    private final HistoriqueAlimentationRepository historiqueRepository;

    public HistoriqueAlimentationService(HistoriqueAlimentationRepository historiqueRepository) {
        this.historiqueRepository = historiqueRepository;
    }

    public List<HistoriqueAlimentationResponse> getAll() {
        return historiqueRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public HistoriqueAlimentationResponse getById(Long id) {
        HistoriqueAlimentation historique = historiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Historique not found with id: " + id));
        return toResponse(historique);
    }

    public List<HistoriqueAlimentationResponse> getByPlafondId(Long plafondId) {
        return historiqueRepository.findByPlafondId(plafondId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<HistoriqueAlimentationResponse> getByProvinceId(Long provinceId) {
        return historiqueRepository.findByProvinceId(provinceId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<HistoriqueAlimentationResponse> getByRegionId(Long regionId) {
        return historiqueRepository.findByProvinceRegionId(regionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private HistoriqueAlimentationResponse toResponse(HistoriqueAlimentation h) {
        return new HistoriqueAlimentationResponse(
                h.getId(),
                h.getPlafond().getId(),
                h.getPlafond().getCompteCode(),
                h.getPlafond().getLibelle(),
                h.getProvince().getId(),
                h.getProvince().getName(),
                h.getProvince().getRegion() != null ? h.getProvince().getRegion().getId() : null,
                h.getProvince().getRegion() != null ? h.getProvince().getRegion().getName() : null,
                h.getMontantAlimentation(),
                h.getAncienDisponible(),
                h.getNouveauDisponible(),
                h.getAncienAvance(),
                h.getNouveauAvance(),
                h.getAncienEncaissement(),
                h.getNouveauEncaissement(),
                h.getAncienPlafondFixe(),
                h.getNouveauPlafondFixe(),
                h.getAncienPlafondAnnuel(),
                h.getNouveauPlafondAnnuel(),
                h.getTypeOperation(),
                h.getOp(),
                h.getDateOp(),
                h.getNumCheque(),
                h.getDateCheque(),
                h.getCommentaire(),
                h.getCreatedBy(),
                h.getCreatedAt()
        );
    }
}
