package ma.entraide.regie.service;

import ma.entraide.regie.dto.ProvinceRequest;
import ma.entraide.regie.dto.ProvinceResponse;
import ma.entraide.regie.entity.Province;
import ma.entraide.regie.entity.Region;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.ProvinceRepository;
import ma.entraide.regie.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProvinceService {

    private final ProvinceRepository provinceRepository;
    private final RegionRepository regionRepository;

    public ProvinceService(ProvinceRepository provinceRepository, RegionRepository regionRepository) {
        this.provinceRepository = provinceRepository;
        this.regionRepository = regionRepository;
    }

    public List<ProvinceResponse> getAllProvinces() {
        return provinceRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProvinceResponse getProvinceById(Long id) {
        Province province = provinceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + id));
        return toResponse(province);
    }

    public List<ProvinceResponse> getProvincesByRegionId(Long regionId) {
        if (!regionRepository.existsById(regionId)) {
            throw new ResourceNotFoundException("Region not found with id: " + regionId);
        }
        return provinceRepository.findByRegionId(regionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProvinceResponse createProvince(ProvinceRequest request) {
        Region region = regionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with id: " + request.getRegionId()));

        Province province = new Province();
        province.setName(request.getName());
        province.setRegion(region);
        Province saved = provinceRepository.save(province);
        return toResponse(saved);
    }

    public ProvinceResponse updateProvince(Long id, ProvinceRequest request) {
        Province province = provinceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + id));

        Region region = regionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with id: " + request.getRegionId()));

        province.setName(request.getName());
        province.setRegion(region);
        Province updated = provinceRepository.save(province);
        return toResponse(updated);
    }

    public void deleteProvince(Long id) {
        if (!provinceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Province not found with id: " + id);
        }
        provinceRepository.deleteById(id);
    }

    private ProvinceResponse toResponse(Province province) {
        return new ProvinceResponse(
                province.getId(),
                province.getName(),
                province.getRegion().getId(),
                province.getRegion().getName()
        );
    }
}

