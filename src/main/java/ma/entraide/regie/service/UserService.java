package ma.entraide.regie.service;

import ma.entraide.regie.dto.UserResponse;
import ma.entraide.regie.dto.UserUpdateRequest;
import ma.entraide.regie.entity.Province;
import ma.entraide.regie.entity.Region;
import ma.entraide.regie.entity.Role;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.exception.DuplicateResourceException;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.ProvinceRepository;
import ma.entraide.regie.repository.RegionRepository;
import ma.entraide.regie.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final ProvinceRepository provinceRepository;

    public UserService(UserRepository userRepository, RegionRepository regionRepository,
                       ProvinceRepository provinceRepository) {
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.provinceRepository = provinceRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        user.setEmail(request.getEmail());
        user.setRole(Role.valueOf(request.getRole().toUpperCase()));

        // Handle region
        if (request.getRegionId() != null) {
            Region region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region not found with id: " + request.getRegionId()));
            user.setRegion(region);
        } else {
            user.setRegion(null);
        }

        // Handle province
        if (request.getProvinceId() != null) {
            Province province = provinceRepository.findById(request.getProvinceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + request.getProvinceId()));
            user.setProvince(province);
        } else {
            user.setProvince(null);
        }

        User updated = userRepository.save(user);
        return toResponse(updated);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getRegion() != null ? user.getRegion().getId() : null,
                user.getRegion() != null ? user.getRegion().getName() : null,
                user.getProvince() != null ? user.getProvince().getId() : null,
                user.getProvince() != null ? user.getProvince().getName() : null
        );
    }
}

