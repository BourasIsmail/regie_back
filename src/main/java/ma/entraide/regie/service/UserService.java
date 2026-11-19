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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final ProvinceRepository provinceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JournalService journal;

    public UserService(UserRepository userRepository, RegionRepository regionRepository,
                       ProvinceRepository provinceRepository, PasswordEncoder passwordEncoder,
                       JournalService journal) {
        this.journal = journal;
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.provinceRepository = provinceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(UserUpdateRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        // Encode the password before saving
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.valueOf(request.getRole().toUpperCase()));

        // Handle region
        if (request.getRegionId() != null) {
            Region region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region not found with id: " + request.getRegionId()));
            user.setRegion(region);
        }

        // Handle province
        if (request.getProvinceId() != null) {
            Province province = provinceRepository.findById(request.getProvinceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + request.getProvinceId()));
            user.setProvince(province);
        }

        User saved = userRepository.save(user);
        logUser("CREATION", saved, new JournalService.Diff()
                .set("Email", null, saved.getEmail()).set("Role", null, saved.getRole())
                .set("Region", null, before(saved.getRegion()))
                .set("Province", null, before(saved.getProvince())));
        return toResponse(saved);
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

        Region oldRegion = user.getRegion();
        Province oldProvince = user.getProvince();
        JournalService.Diff diff = new JournalService.Diff()
                .add("Email", user.getEmail(), request.getEmail())
                .add("Role", user.getRole(), Role.valueOf(request.getRole().toUpperCase()));
        user.setEmail(request.getEmail());
        user.setRole(Role.valueOf(request.getRole().toUpperCase()));

        // Encode password if provided (non-empty)
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            diff.set("Mot de passe", "***", "*** (modifie)");
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

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

        diff.add("Region", before(oldRegion), before(user.getRegion()));
        diff.add("Province", before(oldProvince), before(user.getProvince()));
        User updated = userRepository.save(user);
        logUser("MODIFICATION", updated, diff);
        return toResponse(updated);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        User old = userRepository.findById(id).orElse(null);
        userRepository.deleteById(id);
        if (old != null) {
            logUser("SUPPRESSION", old, new JournalService.Diff()
                    .set("Email", old.getEmail(), null).set("Role", old.getRole(), null));
        }
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

    private String before(Region r) {
        return r == null ? null : r.getName();
    }

    private String before(Province p) {
        return p == null ? null : p.getName();
    }

    private void logUser(String action, User user, JournalService.Diff diff) {
        var j = journal.entry("UTILISATEUR", action, user.getId(), user.getProvince(), null,
                java.time.LocalDate.now().getYear());
        if (user.getRegion() != null) {
            j.setRegionId(user.getRegion().getId());
            j.setRegionName(user.getRegion().getName());
        }
        j.setCommentaire(action + " utilisateur " + user.getEmail());
        journal.save(j, diff);
    }
}
