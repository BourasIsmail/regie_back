package ma.entraide.regie.service;

import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.Role;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Resolves what the authenticated user is allowed to see: everything (ADMIN, ADMIN_VIEW),
 * one region (REGION, VIEW_REGION) or one province (PROV).
 */
@Component
public class CurrentUserScope {

    private final UserRepository userRepository;

    public CurrentUserScope(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User load(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur introuvable"));
    }

    public boolean canRead(User user, PlafondRegie plafond) {
        return switch (user.getRole()) {
            case ADMIN, ADMIN_VIEW -> true;
            case REGION, VIEW_REGION -> user.getRegion() != null
                    && user.getRegion().getId().equals(plafond.getProvince().getRegion().getId());
            case PROV -> user.getProvince() != null
                    && user.getProvince().getId().equals(plafond.getProvince().getId());
            default -> false;
        };
    }

    public boolean canWrite(User user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.REGION || user.getRole() == Role.PROV;
    }
}
