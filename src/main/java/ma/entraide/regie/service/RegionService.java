package ma.entraide.regie.service;

import ma.entraide.regie.dto.RegionRequest;
import ma.entraide.regie.dto.RegionResponse;
import ma.entraide.regie.entity.Region;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegionService {

    private final RegionRepository regionRepository;

    private final JournalService journal;

    public RegionService(RegionRepository regionRepository, JournalService journal) {
        this.regionRepository = regionRepository;
        this.journal = journal;
    }

    public List<RegionResponse> getAllRegions() {
        return regionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RegionResponse getRegionById(Long id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with id: " + id));
        return toResponse(region);
    }

    public RegionResponse createRegion(RegionRequest request) {
        Region region = new Region();
        region.setName(request.getName());
        Region saved = regionRepository.save(region);
        logRegion("CREATION", saved.getId(), saved.getName(), new JournalService.Diff().set("Nom", null, saved.getName()));
        return toResponse(saved);
    }

    public RegionResponse updateRegion(Long id, RegionRequest request) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with id: " + id));
        String ancienNom = region.getName();
        region.setName(request.getName());
        Region updated = regionRepository.save(region);
        logRegion("MODIFICATION", id, updated.getName(), new JournalService.Diff().add("Nom", ancienNom, updated.getName()));
        return toResponse(updated);
    }

    public void deleteRegion(Long id) {
        if (!regionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Region not found with id: " + id);
        }
        String nom = regionRepository.findById(id).map(Region::getName).orElse(null);
        regionRepository.deleteById(id);
        logRegion("SUPPRESSION", id, nom, new JournalService.Diff().set("Nom", nom, null));
    }

    private RegionResponse toResponse(Region region) {
        return new RegionResponse(region.getId(), region.getName());
    }

    private void logRegion(String action, Long id, String nom, JournalService.Diff diff) {
        var j = journal.entry("REGION", action, id, null, null, java.time.LocalDate.now().getYear());
        j.setRegionId(id);
        j.setRegionName(nom);
        j.setCommentaire(action + " region " + nom);
        journal.save(j, diff);
    }
}
