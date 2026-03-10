package ma.entraide.regie.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AlimentationRequest {

    @NotNull(message = "Montant is required")
    @Positive(message = "Montant must be positive")
    private BigDecimal montant;

    private String op;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOp;

    private String numCheque;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateCheque;

    private String commentaire;

    public AlimentationRequest() {
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public LocalDate getDateOp() {
        return dateOp;
    }

    public void setDateOp(LocalDate dateOp) {
        this.dateOp = dateOp;
    }

    public String getNumCheque() {
        return numCheque;
    }

    public void setNumCheque(String numCheque) {
        this.numCheque = numCheque;
    }

    public LocalDate getDateCheque() {
        return dateCheque;
    }

    public void setDateCheque(LocalDate dateCheque) {
        this.dateCheque = dateCheque;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
