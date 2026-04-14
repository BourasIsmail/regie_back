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

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
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
        return toResponse(saved);
    }

    public RegionResponse updateRegion(Long id, RegionRequest request) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with id: " + id));
        region.setName(request.getName());
        Region updated = regionRepository.save(region);
        return toResponse(updated);
    }

    public void deleteRegion(Long id) {
        if (!regionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Region not found with id: " + id);
        }
        regionRepository.deleteById(id);
    }

    private RegionResponse toResponse(Region region) {
        return new RegionResponse(region.getId(), region.getName());
    }
}


