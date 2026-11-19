package ma.entraide.regie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-shot, idempotent data migration run at startup (the schema itself is handled by
 * hibernate ddl-auto=update):
 *  - tags existing transactions / versements with their exercice (year of their date),
 *  - flags existing versements as type VERSEMENT,
 *  - copies the legacy historique_alimentations rows into the new journal (only if the journal is empty).
 */
@Component
public class ExerciceMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExerciceMigrationRunner.class);

    private final JdbcTemplate jdbc;

    public ExerciceMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int t = jdbc.update("update transactions_regie set exercice = year(date_transaction) where exercice is null");
        int v = jdbc.update("update versements_regie set exercice = year(date_versement) where exercice is null");
        jdbc.update("update versements_regie set type_versement = 'VERSEMENT' where type_versement is null");
        if (t + v > 0) {
            log.info("Exercice migration: {} transaction(s) and {} versement(s) tagged", t, v);
        }
        migrateLegacyHistorique();
    }

    private void migrateLegacyHistorique() {
        try {
            Integer journalCount = jdbc.queryForObject("select count(*) from journal_operations", Integer.class);
            Integer legacyCount = jdbc.queryForObject("select count(*) from historique_alimentations", Integer.class);
            if (journalCount == null || legacyCount == null || journalCount > 0 || legacyCount == 0) return;
            int n = jdbc.update("""
                    insert into journal_operations
                      (entite, action, entite_id, exercice, province_id, province_name, region_id, region_name,
                       plafond_id, compte_code, libelle, montant, ancien_encaissement, nouveau_encaissement,
                       ancien_plafond_annuel, nouveau_plafond_annuel, op, date_op, num_cheque, date_cheque,
                       commentaire, created_by, created_at)
                    select
                      case when h.type_operation in ('ALIMENTATION','CORRECTION') then 'PLAFOND'
                           when h.type_operation in ('VERSEMENT','ANNULATION_VERSEMENT') then 'VERSEMENT'
                           else 'TRANSACTION' end,
                      case when h.type_operation = 'ANNULATION_VERSEMENT' then 'SUPPRESSION_VERSEMENT'
                           else coalesce(h.type_operation, 'OPERATION') end,
                      null, year(h.created_at), h.province_id, p.name, r.id, r.name,
                      h.plafond_id, pl.compte_code, pl.libelle, abs(h.montant_alimentation),
                      h.ancien_encaissement, h.nouveau_encaissement,
                      h.ancien_plafond_annuel, h.nouveau_plafond_annuel, h.op, h.date_op, h.num_cheque, h.date_cheque,
                      h.commentaire, h.created_by, h.created_at
                    from historique_alimentations h
                      left join provinces p on p.id = h.province_id
                      left join regions r on r.id = p.region_id
                      left join plafonds_regie pl on pl.id = h.plafond_id
                    """);
            log.info("Journal migration: {} legacy historique row(s) copied", n);
        } catch (Exception e) {
            log.warn("Legacy historique migration skipped: {}", e.getMessage());
        }
    }
}
