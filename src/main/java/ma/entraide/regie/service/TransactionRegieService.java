package ma.entraide.regie.service;

import ma.entraide.regie.dto.TransactionRegieRequest;
import ma.entraide.regie.dto.TransactionRegieResponse;
import ma.entraide.regie.entity.HistoriqueAlimentation;
import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.Province;
import ma.entraide.regie.entity.TransactionRegie;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.HistoriqueAlimentationRepository;
import ma.entraide.regie.repository.PlafondRegieRepository;
import ma.entraide.regie.repository.ProvinceRepository;
import ma.entraide.regie.repository.TransactionRegieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionRegieService {

    private final TransactionRegieRepository transactionRepository;
    private final PlafondRegieRepository plafondRepository;
    private final ProvinceRepository provinceRepository;
    private final HistoriqueAlimentationRepository historiqueRepository;

    public TransactionRegieService(TransactionRegieRepository transactionRepository,
                                   PlafondRegieRepository plafondRepository,
                                   ProvinceRepository provinceRepository,
                                   HistoriqueAlimentationRepository historiqueRepository) {
        this.transactionRepository = transactionRepository;
        this.plafondRepository = plafondRepository;
        this.provinceRepository = provinceRepository;
        this.historiqueRepository = historiqueRepository;
    }

    public List<TransactionRegieResponse> getAll() {
        return transactionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TransactionRegieResponse getById(Long id) {
        TransactionRegie transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return toResponse(transaction);
    }

    public List<TransactionRegieResponse> getByProvinceId(Long provinceId) {
        if (!provinceRepository.existsById(provinceId)) {
            throw new ResourceNotFoundException("Province not found with id: " + provinceId);
        }
        return transactionRepository.findByProvinceId(provinceId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionRegieResponse> getByRegionId(Long regionId) {
        return transactionRepository.findByProvinceRegionId(regionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new expense transaction in EN_ATTENTE status.
     * The montant is immediately deducted from encaissement.
     * If rejected later, the montant is restored.
     */
    @Transactional
    public TransactionRegieResponse create(TransactionRegieRequest request, String createdBy) {
        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with id: " + request.getProvinceId()));

        PlafondRegie plafond = plafondRepository.findByProvinceIdAndCompteCode(request.getProvinceId(), request.getCompteCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plafond not found for province " + request.getProvinceId()
                                + " and compte code " + request.getCompteCode()));

        BigDecimal montant = request.getMontant();

        // Validation: amount must not exceed encaissement
        if (montant.compareTo(plafond.getPlafondEncaissement()) > 0) {
            throw new IllegalArgumentException(
                    "Montant exceeds encaissement disponible. Disponible: " + plafond.getPlafondEncaissement());
        }

        // Validation: amount must not exceed max per invoice
        if (montant.compareTo(plafond.getPlafondMaxFacture()) > 0) {
            throw new IllegalArgumentException(
                    "Montant exceeds plafond max facture. Max: " + plafond.getPlafondMaxFacture());
        }

        // Store old encaissement for historique
        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();

        // Deduct from encaissement immediately
        plafond.setPlafondEncaissement(plafond.getPlafondEncaissement().subtract(montant));
        plafondRepository.save(plafond);

        // Create transaction in EN_ATTENTE status
        TransactionRegie transaction = new TransactionRegie();
        transaction.setProvince(province);
        transaction.setCompteCode(request.getCompteCode());
        transaction.setMontant(montant);
        transaction.setStatut("EN_ATTENTE");
        transaction.setFournisseur(request.getFournisseur());
        transaction.setAdresseFournisseur(request.getAdresseFournisseur());
        transaction.setFactureNumero(request.getFactureNumero());
        transaction.setFactureDate(request.getFactureDate());
        transaction.setNumeroAp(request.getNumeroAp());
        transaction.setDateAp(request.getDateAp());
        transaction.setMoisAnnee(request.getMoisAnnee());
        transaction.setTypeTransaction(request.getTypeTransaction());
        transaction.setDescription(request.getDescription());
        transaction.setDateTransaction(request.getDateTransaction());
        transaction.setCreatedBy(createdBy);

        TransactionRegie saved = transactionRepository.save(transaction);

        // Log to historique
        HistoriqueAlimentation historique = new HistoriqueAlimentation();
        historique.setPlafond(plafond);
        historique.setProvince(province);
        historique.setMontantAlimentation(montant.negate());
        historique.setAncienEncaissement(ancienEncaissement);
        historique.setNouveauEncaissement(plafond.getPlafondEncaissement());
        historique.setTypeOperation("DEPENSE_EN_ATTENTE");
        historique.setCommentaire("Depense #" + saved.getId() + " en attente - " + request.getFournisseur());
        historique.setCreatedBy(createdBy);
        historiqueRepository.save(historique);

        return toResponse(saved);
    }

    /**
     * Confirm a transaction (REGION role only).
     * If montantValide differs from montant, adjust encaissement accordingly.
     * - If montantValide < montant: restore the difference to encaissement
     * - If montantValide > montant: deduct the additional amount from encaissement
     */
    @Transactional
    public TransactionRegieResponse confirm(Long id, BigDecimal montantValide, String validatedBy) {
        TransactionRegie transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!"EN_ATTENTE".equals(transaction.getStatut())) {
            throw new IllegalArgumentException("Transaction is not in EN_ATTENTE status");
        }

        PlafondRegie plafond = plafondRepository.findByProvinceIdAndCompteCode(
                        transaction.getProvince().getId(), transaction.getCompteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found for this transaction"));

        BigDecimal montantOriginal = transaction.getMontant();
        BigDecimal difference = montantValide.subtract(montantOriginal);

        // Validation: if montantValide > montant, check if we have enough encaissement for the difference
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            if (difference.compareTo(plafond.getPlafondEncaissement()) > 0) {
                throw new IllegalArgumentException(
                        "Montant valide exceeds encaissement disponible. Difference needed: " + difference
                                + ", Available: " + plafond.getPlafondEncaissement());
            }
        }

        // Validation: amount must not exceed max per invoice
        if (montantValide.compareTo(plafond.getPlafondMaxFacture()) > 0) {
            throw new IllegalArgumentException(
                    "Montant valide exceeds plafond max facture. Max: " + plafond.getPlafondMaxFacture());
        }

        // Store old encaissement for historique
        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();

        // Adjust encaissement based on difference
        // If montantValide < montant: add back difference (positive adjustment)
        // If montantValide > montant: deduct difference (negative adjustment)
        plafond.setPlafondEncaissement(plafond.getPlafondEncaissement().subtract(difference));
        plafondRepository.save(plafond);

        // Update transaction
        transaction.setMontantValide(montantValide);
        transaction.setStatut("CONFIRMEE");
        transaction.setValidatedBy(validatedBy);
        transaction.setValidatedAt(java.time.LocalDateTime.now());

        TransactionRegie saved = transactionRepository.save(transaction);

        // Log to historique
        HistoriqueAlimentation historique = new HistoriqueAlimentation();
        historique.setPlafond(plafond);
        historique.setProvince(transaction.getProvince());
        historique.setMontantAlimentation(difference.negate()); // Negative if we deducted more
        historique.setAncienEncaissement(ancienEncaissement);
        historique.setNouveauEncaissement(plafond.getPlafondEncaissement());
        historique.setTypeOperation("CONFIRMATION_DEPENSE");
        historique.setCommentaire("Confirmation transaction #" + id + " - Montant demande: " + montantOriginal + " DH, Montant valide: " + montantValide + " DH");
        historique.setCreatedBy(validatedBy);
        historiqueRepository.save(historique);

        return toResponse(saved);
    }

    /**
     * Reject a transaction (REGION role only).
     * Restores the montant to encaissement since it was deducted at creation.
     */
    @Transactional
    public TransactionRegieResponse reject(Long id, String rejectedBy) {
        TransactionRegie transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!"EN_ATTENTE".equals(transaction.getStatut())) {
            throw new IllegalArgumentException("Transaction is not in EN_ATTENTE status");
        }

        PlafondRegie plafond = plafondRepository.findByProvinceIdAndCompteCode(
                        transaction.getProvince().getId(), transaction.getCompteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found for this transaction"));

        BigDecimal montant = transaction.getMontant();
        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();

        // Restore montant to encaissement
        plafond.setPlafondEncaissement(plafond.getPlafondEncaissement().add(montant));
        plafondRepository.save(plafond);

        // Update transaction status
        transaction.setStatut("REJETEE");
        transaction.setValidatedBy(rejectedBy);
        transaction.setValidatedAt(java.time.LocalDateTime.now());

        TransactionRegie saved = transactionRepository.save(transaction);

        // Log to historique
        HistoriqueAlimentation historique = new HistoriqueAlimentation();
        historique.setPlafond(plafond);
        historique.setProvince(transaction.getProvince());
        historique.setMontantAlimentation(montant); // Positive - restored
        historique.setAncienEncaissement(ancienEncaissement);
        historique.setNouveauEncaissement(plafond.getPlafondEncaissement());
        historique.setTypeOperation("REJET_DEPENSE");
        historique.setCommentaire("Rejet transaction #" + id + " - Montant restitue: " + montant + " DH");
        historique.setCreatedBy(rejectedBy);
        historiqueRepository.save(historique);

        return toResponse(saved);
    }

    /**
     * Update a pending transaction (EN_ATTENTE only).
     * Restores the old amount, validates the new amount, then applies the new deduction.
     * Only DELEGATION can update their own pending transactions.
     */
    @Transactional
    public TransactionRegieResponse update(Long id, TransactionRegieRequest request, String updatedBy) {
        TransactionRegie transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        // Only allow update if transaction is EN_ATTENTE
        if (!"EN_ATTENTE".equals(transaction.getStatut())) {
            throw new IllegalArgumentException("Seules les transactions en attente peuvent etre modifiees");
        }

        PlafondRegie plafond = plafondRepository.findByProvinceIdAndCompteCode(
                        transaction.getProvince().getId(), transaction.getCompteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found for this transaction"));

        BigDecimal oldMontant = transaction.getMontant();
        BigDecimal newMontant = request.getMontant();

        // Store old encaissement for historique
        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();

        // Calculate sum of existing transactions for this plafond (excluding current transaction)
        BigDecimal totalOtherTransactions = transactionRepository.findByProvinceIdAndCompteCode(
                        transaction.getProvince().getId(), transaction.getCompteCode())
                .stream()
                .filter(t -> !t.getId().equals(id))
                .map(TransactionRegie::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Available encaissement = current encaissement + old transaction amount (since it will be restored)
        BigDecimal encaissementDisponible = plafond.getPlafondEncaissement().add(oldMontant);

        // Available annual = plafondAnnuel - totalOtherTransactions
        BigDecimal annuelDisponible = plafond.getPlafondAnnuel().subtract(totalOtherTransactions);

        // Validation 1: new amount must not exceed max per invoice
        if (newMontant.compareTo(plafond.getPlafondMaxFacture()) > 0) {
            throw new IllegalArgumentException(
                    "New montant exceeds plafond max facture. Max: " + plafond.getPlafondMaxFacture());
        }

        // Validation 2: new amount must not exceed available encaissement
        if (newMontant.compareTo(encaissementDisponible) > 0) {
            throw new IllegalArgumentException(
                    "New montant exceeds encaissement disponible. Available: " + encaissementDisponible);
        }

        // Validation 3: new amount must not exceed annual budget
        if (newMontant.compareTo(annuelDisponible) > 0) {
            throw new IllegalArgumentException(
                    "New montant exceeds disponible annuel. Available: " + annuelDisponible);
        }

        // Update encaissement: restore old amount then deduct new amount
        plafond.setPlafondEncaissement(plafond.getPlafondEncaissement().add(oldMontant).subtract(newMontant));
        plafondRepository.save(plafond);

        // Update transaction
        transaction.setMontant(newMontant);
        transaction.setFournisseur(request.getFournisseur());
        transaction.setAdresseFournisseur(request.getAdresseFournisseur());
        transaction.setFactureNumero(request.getFactureNumero());
        transaction.setFactureDate(request.getFactureDate());
        transaction.setNumeroAp(request.getNumeroAp());
        transaction.setDateAp(request.getDateAp());
        transaction.setMoisAnnee(request.getMoisAnnee());
        transaction.setTypeTransaction(request.getTypeTransaction());
        transaction.setDescription(request.getDescription());
        transaction.setDateTransaction(request.getDateTransaction());

        TransactionRegie updated = transactionRepository.save(transaction);

        // Log modification to historique
        HistoriqueAlimentation historique = new HistoriqueAlimentation();
        historique.setPlafond(plafond);
        historique.setProvince(transaction.getProvince());
        historique.setMontantAlimentation(newMontant.subtract(oldMontant)); // Difference
        historique.setAncienEncaissement(ancienEncaissement);
        historique.setNouveauEncaissement(plafond.getPlafondEncaissement());
        historique.setTypeOperation("MODIFICATION");
        historique.setCommentaire("Modification transaction #" + id + " - Ancien: " + oldMontant + " DH, Nouveau: " + newMontant + " DH");
        historique.setCreatedBy(updatedBy);
        historiqueRepository.save(historique);

        return toResponse(updated);
    }

    /**
     * Delete a transaction.
     * - For EN_ATTENTE or CONFIRMEE: restore the montant to encaissement
     * - For REJETEE: no restoration (amount was already restored when rejected)
     */
    @Transactional
    public void delete(Long id, String deletedBy) {
        TransactionRegie transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        PlafondRegie plafond = plafondRepository.findByProvinceIdAndCompteCode(
                        transaction.getProvince().getId(), transaction.getCompteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Plafond not found for this transaction"));

        // Store old values for historique
        BigDecimal ancienEncaissement = plafond.getPlafondEncaissement();
        BigDecimal montant = transaction.getMontant();
        BigDecimal montantValide = transaction.getMontantValide();
        String statut = transaction.getStatut();

        // Only restore amount if not already rejected (REJETEE already restored when rejected)
        BigDecimal montantARestituer = BigDecimal.ZERO;
        if (!"REJETEE".equals(statut)) {
            // For CONFIRMEE, restore the validated amount; for EN_ATTENTE, restore the original amount
            montantARestituer = "CONFIRMEE".equals(statut) && montantValide != null
                    ? montantValide
                    : montant;
            plafond.setPlafondEncaissement(plafond.getPlafondEncaissement().add(montantARestituer));
            plafondRepository.save(plafond);
        }

        // Log suppression to historique
        HistoriqueAlimentation historique = new HistoriqueAlimentation();
        historique.setPlafond(plafond);
        historique.setProvince(transaction.getProvince());
        historique.setMontantAlimentation(montantARestituer); // Zero if REJETEE
        historique.setAncienEncaissement(ancienEncaissement);
        historique.setNouveauEncaissement(plafond.getPlafondEncaissement());
        historique.setTypeOperation("SUPPRESSION");
        String commentaire = "Suppression transaction #" + id + " (" + statut + ") - " + transaction.getFournisseur();
        if (montantARestituer.compareTo(BigDecimal.ZERO) > 0) {
            commentaire += " - " + montantARestituer + " DH restitue";
        }
        historique.setCommentaire(commentaire);
        historique.setCreatedBy(deletedBy);
        historiqueRepository.save(historique);

        transactionRepository.delete(transaction);
    }

    private TransactionRegieResponse toResponse(TransactionRegie t) {
        return new TransactionRegieResponse(
                t.getId(),
                t.getProvince().getId(),
                t.getProvince().getName(),
                t.getCompteCode(),
                t.getMontant(),
                t.getMontantValide(),
                t.getStatut(),
                t.getValidatedBy(),
                t.getValidatedAt(),
                t.getFournisseur(),
                t.getAdresseFournisseur(),
                t.getFactureNumero(),
                t.getFactureDate(),
                t.getNumeroAp(),
                t.getDateAp(),
                t.getMoisAnnee(),
                t.getTypeTransaction(),
                t.getDescription(),
                t.getDateTransaction(),
                t.getCreatedBy(),
                t.getCreatedAt()
        );
    }
}
