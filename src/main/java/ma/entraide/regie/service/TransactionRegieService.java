package ma.entraide.regie.service;

import ma.entraide.regie.dto.TransactionRegieRequest;
import ma.entraide.regie.dto.TransactionRegieResponse;
import ma.entraide.regie.entity.JournalOperation;
import ma.entraide.regie.entity.User;
import ma.entraide.regie.entity.PlafondRegie;
import ma.entraide.regie.entity.Province;
import ma.entraide.regie.entity.TransactionRegie;
import ma.entraide.regie.exception.ResourceNotFoundException;
import ma.entraide.regie.repository.PlafondRegieRepository;
import ma.entraide.regie.repository.ProvinceRepository;
import ma.entraide.regie.repository.TransactionRegieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Service
public class TransactionRegieService {

    private final TransactionRegieRepository transactionRepository;
    private final PlafondRegieRepository plafondRepository;
    private final ProvinceRepository provinceRepository;
    private final JournalService journal;
    private final CurrentUserScope scope;
    private final ExerciceService exerciceService;

    public TransactionRegieService(TransactionRegieRepository transactionRepository,
                                   PlafondRegieRepository plafondRepository,
                                   ProvinceRepository provinceRepository,
                                   JournalService journal,
                                   CurrentUserScope scope,
                                   ExerciceService exerciceService) {
        this.transactionRepository = transactionRepository;
        this.plafondRepository = plafondRepository;
        this.provinceRepository = provinceRepository;
        this.journal = journal;
        this.scope = scope;
        this.exerciceService = exerciceService;
    }

    public List<TransactionRegieResponse> getAll(Integer exercice) {
        return visible(transactionRepository.findAll(), exercice);
    }

    public TransactionRegieResponse getById(Long id) {
        TransactionRegie transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return toResponse(transaction);
    }

    public List<TransactionRegieResponse> getByProvinceId(Long provinceId, Integer exercice) {
        if (!provinceRepository.existsById(provinceId)) {
            throw new ResourceNotFoundException("Province not found with id: " + provinceId);
        }
        return visible(transactionRepository.findByProvinceId(provinceId), exercice);
    }

    public List<TransactionRegieResponse> getByRegionId(Long regionId, Integer exercice) {
        return visible(transactionRepository.findByProvinceRegionId(regionId), exercice);
    }

    /** Keeps the transactions the user may read, for the requested exercice (default: current one). */
    private List<TransactionRegieResponse> visible(List<TransactionRegie> all, Integer exercice) {
        User user = scope.current();
        exerciceService.checkViewable(user, exercice);
        Map<Long, Integer> years = exerciceService.currentYears();
        return all.stream()
                .filter(t -> scope.canReadProvince(user, t.getProvince()))
                .filter(t -> exerciceService.matches(t.getExercice(), t.getProvince().getId(), exercice, years))
                .map(t -> toResponse(t, years))
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
        transaction.setExercice(exerciceService.currentYear(province.getId()));
        transaction.setFournisseur(request.getFournisseur());
        transaction.setAdresseFournisseur(request.getAdresseFournisseur());
        transaction.setFactureNumero(request.getFactureNumero());
        transaction.setFactureDate(request.getFactureDate());
        transaction.setNumeroAp(request.getNumeroAp());
        // Auto-set dateAp = factureDate
        transaction.setDateAp(request.getFactureDate());
        // Auto-calculate moisAnnee from factureDate (format: yyyy-MM)
        if (request.getFactureDate() != null) {
            String moisAnnee = request.getFactureDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            transaction.setMoisAnnee(moisAnnee);
        }
        transaction.setTypeTransaction(request.getTypeTransaction());
        transaction.setDescription(request.getDescription());
        transaction.setDateTransaction(request.getDateTransaction());
        transaction.setCreatedBy(createdBy);

        // Calculate and store disponibleAnnuelSnapshot at creation time
        // = budgetAnnuelInitial - total of all confirmed AND pending transactions for this compte
        BigDecimal budgetAnnuelInitial = plafond.getBudgetAnnuelInitial() != null
                ? plafond.getBudgetAnnuelInitial()
                : plafond.getPlafondAnnuel();
        BigDecimal totalDepenses = transactionRepository.findByProvinceIdAndCompteCode(
                        province.getId(), request.getCompteCode())
                .stream()
                .filter(t -> transaction.getExercice().equals(t.getExercice()))
                .filter(t -> "CONFIRMEE".equals(t.getStatut()) || "EN_ATTENTE".equals(t.getStatut()))
                .map(t -> t.getMontantValide() != null ? t.getMontantValide() : t.getMontant())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disponibleSnapshot = budgetAnnuelInitial.subtract(totalDepenses);
        transaction.setDisponibleAnnuelSnapshot(disponibleSnapshot);

        TransactionRegie saved = transactionRepository.save(transaction);

        // Log to historique
        logTx("DEPENSE_EN_ATTENTE", saved, plafond, montant, ancienEncaissement,
                "Depense #" + saved.getId() + " en attente - " + request.getFournisseur(),
                new JournalService.Diff()
                        .set("Montant", null, montant)
                        .set("Fournisseur", null, request.getFournisseur())
                        .set("Facture", null, request.getFactureNumero())
                        .set("Date facture", null, request.getFactureDate())
                        .set("Numero AP", null, request.getNumeroAp())
                        .set("Type", null, request.getTypeTransaction())
                        .set("Encaissement", ancienEncaissement, plafond.getPlafondEncaissement()));

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
        exerciceService.assertEditable(transaction.getProvince().getId(), transaction.getExercice());

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

        // Update transaction (disponibleAnnuelSnapshot is already set at creation time)
        transaction.setMontantValide(montantValide);
        transaction.setStatut("CONFIRMEE");
        transaction.setValidatedBy(validatedBy);
        transaction.setValidatedAt(java.time.LocalDateTime.now());

        TransactionRegie saved = transactionRepository.save(transaction);

        // Log to historique
        logTx("CONFIRMATION_DEPENSE", saved, plafond, montantValide, ancienEncaissement,
                "Confirmation transaction #" + id + " - Montant demande: " + montantOriginal + " DH, Montant valide: " + montantValide + " DH",
                new JournalService.Diff()
                        .set("Statut", "EN_ATTENTE", "CONFIRMEE")
                        .add("Montant", montantOriginal, montantValide)
                        .set("Encaissement", ancienEncaissement, plafond.getPlafondEncaissement()));

        return toResponse(saved);
    }

    /**
     * Reject a transaction (REGION role only).
     * Restores the montant to encaissement since it was deducted at creation.
     * @param motifRejet The reason for rejection
     */
    @Transactional
    public TransactionRegieResponse reject(Long id, String motifRejet, String rejectedBy) {
        TransactionRegie transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        exerciceService.assertEditable(transaction.getProvince().getId(), transaction.getExercice());

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
        transaction.setMotifRejet(motifRejet);

        TransactionRegie saved = transactionRepository.save(transaction);

        // Log to historique with motif
        String commentaire = "Rejet transaction #" + id + " - Montant restitue: " + montant + " DH";
        if (motifRejet != null && !motifRejet.isBlank()) {
            commentaire += " | Motif: " + motifRejet;
        }
        logTx("REJET_DEPENSE", saved, plafond, montant, ancienEncaissement, commentaire,
                new JournalService.Diff()
                        .set("Statut", "EN_ATTENTE", "REJETEE")
                        .set("Motif", null, motifRejet)
                        .set("Encaissement", ancienEncaissement, plafond.getPlafondEncaissement()));

        // Recalculate disponibleAnnuelSnapshot for all transactions AFTER this one (same rubrique)
        recalculateSnapshotsAfterRejection(transaction);

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
        exerciceService.assertEditable(transaction.getProvince().getId(), transaction.getExercice());

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

        // Available encaissement = current encaissement + old transaction amount (since it will be restored)
        BigDecimal encaissementDisponible = plafond.getPlafondEncaissement().add(oldMontant);

        // Validation 1: new amount must not exceed max per invoice
        if (newMontant.compareTo(plafond.getPlafondMaxFacture()) > 0) {
            throw new IllegalArgumentException(
                    "Le montant saisi (" + newMontant + " DH) depasse le plafond maximum par facture ("
                            + plafond.getPlafondMaxFacture() + " DH).");
        }

        // Validation 2: new amount must not exceed available encaissement.
        // Note: an expense only debits the encaissement (caisse). The annual budget
        // (plafondAnnuel) is only reduced when alimenting the encaissement, so it must
        // NOT be used to validate an expense here.
        if (newMontant.compareTo(encaissementDisponible) > 0) {
            throw new IllegalArgumentException(
                    "Le montant saisi (" + newMontant + " DH) depasse l'encaissement disponible ("
                            + encaissementDisponible + " DH).");
        }

        JournalService.Diff diff = new JournalService.Diff()
                .add("Montant", oldMontant, newMontant)
                .add("Fournisseur", transaction.getFournisseur(), request.getFournisseur())
                .add("Adresse fournisseur", transaction.getAdresseFournisseur(), request.getAdresseFournisseur())
                .add("Facture", transaction.getFactureNumero(), request.getFactureNumero())
                .add("Date facture", transaction.getFactureDate(), request.getFactureDate())
                .add("Numero AP", transaction.getNumeroAp(), request.getNumeroAp())
                .add("Type", transaction.getTypeTransaction(), request.getTypeTransaction())
                .add("Description", transaction.getDescription(), request.getDescription());

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
        // Auto-set dateAp = factureDate
        transaction.setDateAp(request.getFactureDate());
        // Auto-calculate moisAnnee from factureDate (format: yyyy-MM)
        if (request.getFactureDate() != null) {
            String moisAnnee = request.getFactureDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            transaction.setMoisAnnee(moisAnnee);
        }
        transaction.setTypeTransaction(request.getTypeTransaction());
        transaction.setDescription(request.getDescription());
        // Only update dateTransaction if provided, otherwise keep original
        if (request.getDateTransaction() != null) {
            transaction.setDateTransaction(request.getDateTransaction());
        }

        TransactionRegie updated = transactionRepository.save(transaction);

        // Log modification to historique
        logTx("MODIFICATION", updated, plafond, newMontant, ancienEncaissement,
                "Modification transaction #" + id + " - Ancien: " + oldMontant + " DH, Nouveau: " + newMontant + " DH",
                diff.add("Encaissement", ancienEncaissement, plafond.getPlafondEncaissement()));

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
        exerciceService.assertEditable(transaction.getProvince().getId(), transaction.getExercice());

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
        String commentaire = "Suppression transaction #" + id + " (" + statut + ") - " + transaction.getFournisseur();
        if (montantARestituer.compareTo(BigDecimal.ZERO) > 0) {
            commentaire += " - " + montantARestituer + " DH restitue";
        }
        logTx("SUPPRESSION", transaction, plafond, montantValide != null ? montantValide : montant, ancienEncaissement, commentaire,
                new JournalService.Diff()
                        .set("Statut", statut, null)
                        .set("Montant", montant, null)
                        .set("Fournisseur", transaction.getFournisseur(), null)
                        .set("Facture", transaction.getFactureNumero(), null)
                        .set("Encaissement", ancienEncaissement, plafond.getPlafondEncaissement()));

        transactionRepository.delete(transaction);
    }

    /**
     * Recalculate disponibleAnnuelSnapshot for all transactions AFTER the rejected one (same rubrique).
     * This ensures subsequent transactions reflect the correct available balance.
     */
    private void recalculateSnapshotsAfterRejection(TransactionRegie rejectedTransaction) {
        Long provinceId = rejectedTransaction.getProvince().getId();
        String compteCode = rejectedTransaction.getCompteCode();
        java.time.LocalDateTime rejectedCreatedAt = rejectedTransaction.getCreatedAt();

        // Get budget initial for this plafond
        PlafondRegie plafond = plafondRepository.findByProvinceIdAndCompteCode(provinceId, compteCode)
                .orElse(null);
        if (plafond == null) return;

        BigDecimal budgetAnnuelInitial = plafond.getBudgetAnnuelInitial() != null
                ? plafond.getBudgetAnnuelInitial()
                : plafond.getPlafondAnnuel();

        // Get all non-rejected transactions for this rubrique, sorted by creation date
        java.util.List<TransactionRegie> allTransactions = transactionRepository.findByProvinceIdAndCompteCode(provinceId, compteCode)
                .stream()
                .filter(t -> rejectedTransaction.getExercice() == null
                        || rejectedTransaction.getExercice().equals(t.getExercice()))
                .filter(t -> !"REJETEE".equals(t.getStatut()))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(java.util.stream.Collectors.toList());

        // Recalculate snapshot for each transaction based on all previous transactions
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (TransactionRegie t : allTransactions) {
            // Snapshot = budgetInitial - sum of all previous transactions (confirmed or pending)
            BigDecimal newSnapshot = budgetAnnuelInitial.subtract(runningTotal);

            // Only update if the transaction was created after the rejected one
            if (t.getCreatedAt().isAfter(rejectedCreatedAt) || t.getCreatedAt().isEqual(rejectedCreatedAt)) {
                if (!t.getId().equals(rejectedTransaction.getId())) {
                    t.setDisponibleAnnuelSnapshot(newSnapshot);
                    transactionRepository.save(t);
                }
            }

            // Add this transaction's amount to running total for next iteration
            BigDecimal montant = t.getMontantValide() != null ? t.getMontantValide() : t.getMontant();
            runningTotal = runningTotal.add(montant);
        }
    }

    private void logTx(String action, TransactionRegie t, PlafondRegie plafond, BigDecimal montant,
                       BigDecimal ancienEncaissement, String commentaire, JournalService.Diff diff) {
        JournalOperation j = journal.entry("TRANSACTION", action, t.getId(), t.getProvince(), plafond, t.getExercice());
        j.setMontant(montant);
        j.setAncienEncaissement(ancienEncaissement);
        j.setNouveauEncaissement(plafond.getPlafondEncaissement());
        j.setCommentaire(commentaire);
        journal.save(j, diff);
    }

    private TransactionRegieResponse toResponse(TransactionRegie t) {
        return toResponse(t, null);
    }

    private TransactionRegieResponse toResponse(TransactionRegie t, Map<Long, Integer> years) {
        TransactionRegieResponse response = new TransactionRegieResponse(
                t.getId(),
                t.getProvince().getId(),
                t.getProvince().getName(),
                t.getCompteCode(),
                t.getMontant(),
                t.getMontantValide(),
                t.getStatut(),
                t.getValidatedBy(),
                t.getValidatedAt(),
                t.getMotifRejet(),
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
        response.setDisponibleAnnuelSnapshot(t.getDisponibleAnnuelSnapshot());
        response.setExercice(t.getExercice());
        int current = years != null
                ? exerciceService.currentYear(years, t.getProvince().getId())
                : exerciceService.currentYear(t.getProvince().getId());
        response.setCloture(t.getExercice() != null && t.getExercice() < current);
        return response;
    }
}
