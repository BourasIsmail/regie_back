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

    private final JournalService journal;

    public ProvinceService(ProvinceRepository provinceRepository, RegionRepository regionRepository,
                           JournalService journal) {
        this.journal = journal;
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
        logProvince("CREATION", saved, new JournalService.Diff()
                .set("Nom", null, saved.getName()).set("Region", null, region.getName()));
        return toResponse(saved);
    }

    public ProvinceResponse updateProvince(Long id, ProvinceRequest request) {
        Province province = provinceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + id));

        Region region = regionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with id: " + request.getRegionId()));

        JournalService.Diff diff = new JournalService.Diff()
                .add("Nom", province.getName(), request.getName())
                .add("Region", province.getRegion() != null ? province.getRegion().getName() : null, region.getName());
        province.setName(request.getName());
        province.setRegion(region);
        Province updated = provinceRepository.save(province);
        logProvince("MODIFICATION", updated, diff);
        return toResponse(updated);
    }

    public void deleteProvince(Long id) {
        if (!provinceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Province not found with id: " + id);
        }
        Province old = provinceRepository.findById(id).orElse(null);
        provinceRepository.deleteById(id);
        if (old != null) {
            logProvince("SUPPRESSION", old, new JournalService.Diff().set("Nom", old.getName(), null));
        }
    }

    private ProvinceResponse toResponse(Province province) {
        return new ProvinceResponse(
                province.getId(),
                province.getName(),
                province.getRegion().getId(),
                province.getRegion().getName()
        );
    }

    private void logProvince(String action, Province province, JournalService.Diff diff) {
        var j = journal.entry("PROVINCE", action, province.getId(), province, null, java.time.LocalDate.now().getYear());
        j.setCommentaire(action + " province " + province.getName());
        journal.save(j, diff);
    }
}
