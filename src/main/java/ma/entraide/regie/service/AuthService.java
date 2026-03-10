package ma.entraide.regie.service;

import ma.entraide.regie.dto.AuthRequest;
import ma.entraide.regie.dto.AuthResponse;
import ma.entraide.regie.dto.RegisterRequest;
import ma.entraide.regie.entity.Province;
import ma.entraide.regie.entity.Region;
import ma.entraide.regie.entity.Role;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.repository.ProvinceRepository;
import ma.entraide.regie.repository.RegionRepository;
import ma.entraide.regie.repository.UserRepository;
import ma.entraide.regie.security.CustomUserDetailsService;
import ma.entraide.regie.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final ProvinceRepository provinceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
                       RegionRepository regionRepository,
                       ProvinceRepository provinceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.provinceRepository = provinceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + request.getRole());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        if (request.getRegionId() != null) {
            Region region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new RuntimeException("Region not found with id: " + request.getRegionId()));
            user.setRegion(region);
        }

        if (request.getProvinceId() != null) {
            Province province = provinceRepository.findById(request.getProvinceId())
                    .orElseThrow(() -> new RuntimeException("Province not found with id: " + request.getProvinceId()));
            user.setProvince(province);
        }

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getRegion() != null ? user.getRegion().getId() : null,
                user.getRegion() != null ? user.getRegion().getName() : null,
                user.getProvince() != null ? user.getProvince().getId() : null,
                user.getProvince() != null ? user.getProvince().getName() : null
        );
    }

    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getRegion() != null ? user.getRegion().getId() : null,
                user.getRegion() != null ? user.getRegion().getName() : null,
                user.getProvince() != null ? user.getProvince().getId() : null,
                user.getProvince() != null ? user.getProvince().getName() : null
        );
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtUtil.extractEmail(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtUtil.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new AuthResponse(
                newAccessToken,
                refreshToken,
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
