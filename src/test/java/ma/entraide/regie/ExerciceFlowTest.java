package ma.entraide.regie;

import ma.entraide.regie.dto.*;
import ma.entraide.regie.entity.*;
import ma.entraide.regie.repository.*;
import ma.entraide.regie.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ExerciceFlowTest {

    @Autowired RegionRepository regions;
    @Autowired ProvinceRepository provinces;
    @Autowired UserRepository users;
    @Autowired PlafondRegieRepository plafonds;
    @Autowired PlafondRegieService plafondService;
    @Autowired TransactionRegieService txService;
    @Autowired VersementService versementService;
    @Autowired SituationService situationService;
    @Autowired ClotureService clotureService;
    @Autowired HistoriqueAlimentationService historique;
    @Autowired ExerciceService exerciceService;

    Province p1;
    PlafondRegie plafond;

    @BeforeEach
    void setup() {
        Region r1 = regions.save(region("R1"));
        Region r2 = regions.save(region("R2"));
        p1 = new Province(); p1.setName("P1"); p1.setRegion(r1); p1 = provinces.save(p1);
        user("admin@x", Role.ADMIN, null, null);
        user("adminview@x", Role.ADMIN_VIEW, null, null);
        user("region1@x", Role.REGION, r1, null);
        user("region2@x", Role.REGION, r2, null);
        user("prov1@x", Role.PROV, r1, p1);
        plafond = new PlafondRegie();
        plafond.setProvince(p1); plafond.setCompteCode("611"); plafond.setLibelle("Fournitures");
        plafond.setPlafondAnnuel(new BigDecimal("1000")); plafond.setBudgetAnnuelInitial(new BigDecimal("1000"));
        plafond.setPlafondEncaissement(BigDecimal.ZERO); plafond.setPlafondMaxFacture(new BigDecimal("500"));
        plafond = plafonds.save(plafond);
    }

    @Test
    void fullLifecycle() {
        as("prov1@x", "PROV");
        AlimentationRequest a = new AlimentationRequest(); a.setMontant(new BigDecimal("600"));
        plafondService.alimenter(plafond.getId(), a, "prov1@x");

        TransactionRegieRequest t = new TransactionRegieRequest();
        t.setProvinceId(p1.getId()); t.setCompteCode("611"); t.setMontant(new BigDecimal("200"));
        t.setFournisseur("ACME"); t.setDateTransaction(LocalDateTime.now());
        TransactionRegieResponse tx = txService.create(t, "prov1@x");
        assertEquals(2026, tx.getExercice());
        as("region1@x", "REGION");
        txService.confirm(tx.getId(), new BigDecimal("200"), "region1@x");

        // solde = avance 600 - depenses 200 = 400 ; versement above the solde is refused
        as("prov1@x", "PROV");
        PlafondRegie live = plafonds.findById(plafond.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("400").compareTo(situationService.soldeCourant(live)));
        assertThrows(IllegalArgumentException.class, () ->
                versementService.create(plafond.getId(), new VersementRequest(new BigDecimal("500"), LocalDate.now(), "R", null, "VERSEMENT"), "prov1@x"));
        // encaissement is only 400 - 0 = 400 but the cap is the solde, both types work the same
        versementService.create(plafond.getId(), new VersementRequest(new BigDecimal("300"), LocalDate.now(), "R1", null, "VERSEMENT"), "prov1@x");
        VersementResponse annul = versementService.create(plafond.getId(),
                new VersementRequest(new BigDecimal("100"), LocalDate.now(), "R2", null, "ANNULATION"), "prov1@x");
        assertEquals("ANNULATION", annul.type());

        SituationRowResponse row = situationService.getSituation("prov1@x", null, null).rows().get(0);
        assertEquals(0, new BigDecimal("400").compareTo(row.versement()));
        assertEquals(0, BigDecimal.ZERO.compareTo(row.solde()));
        assertFalse(row.cloture());

        // closure: only admin
        assertThrows(AccessDeniedException.class, () -> clotureService.cloturer(p1.getId(), 2026));
        as("admin@x", "ADMIN");
        assertTrue(clotureService.precheck(p1.getId(), 2026).cloturable());
        assertThrows(IllegalArgumentException.class, () -> clotureService.cloturer(p1.getId(), 2027));
        clotureService.cloturer(p1.getId(), 2026);

        // reset rules for the new exercice
        PlafondRegie after = plafonds.findById(plafond.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1000").compareTo(after.getPlafondAnnuel()));
        assertEquals(0, BigDecimal.ZERO.compareTo(after.getPlafondEncaissement()));
        assertEquals(0, new BigDecimal("500").compareTo(after.getPlafondMaxFacture()));
        assertEquals(2027, exerciceService.currentYear(p1.getId()));
        assertThrows(IllegalArgumentException.class, () -> clotureService.cloturer(p1.getId(), 2026));

        // closed exercice is read-only
        assertThrows(IllegalArgumentException.class, () -> txService.delete(tx.getId(), "admin@x"));
        assertThrows(IllegalArgumentException.class, () -> versementService.delete(annul.id(), "admin@x"));

        // default view = new exercice (empty), 2026 still readable by admin & region of the province
        assertTrue(txService.getAll(null).isEmpty());
        assertEquals(1, txService.getAll(2026).size());
        assertTrue(txService.getAll(2026).get(0).isCloture());
        SituationResponse s26 = situationService.getSituation("admin@x", 2026, null);
        SituationRowResponse c = s26.rows().get(0);
        assertTrue(c.cloture());
        assertEquals(0, new BigDecimal("600").compareTo(c.avance()));
        assertEquals(0, new BigDecimal("200").compareTo(c.depenses()));
        assertEquals(0, new BigDecimal("400").compareTo(c.versement()));
        assertEquals(0, BigDecimal.ZERO.compareTo(c.solde()));
        assertEquals(1, plafondService.getAll(2026).size());
        assertTrue(plafondService.getAll(2026).get(0).isCloture());
        assertEquals(0, new BigDecimal("400").compareTo(plafondService.getAll(2026).get(0).getPlafondAnnuel()));
        SituationResponse s27 = situationService.getSituation("admin@x", null, null);
        assertEquals(2027, s27.exercice());
        assertFalse(s27.rows().get(0).cloture());
        assertEquals(0, new BigDecimal("1000").compareTo(s27.rows().get(0).disponibleRubrique()));
        assertEquals(0, BigDecimal.ZERO.compareTo(s27.rows().get(0).versement()));

        as("adminview@x", "ADMIN_VIEW");
        assertEquals(1, txService.getAll(2026).size());
        as("region1@x", "REGION");
        assertEquals(1, txService.getAll(2026).size());
        as("region2@x", "REGION");
        assertTrue(txService.getAll(2026).isEmpty());
        as("prov1@x", "PROV");
        assertThrows(AccessDeniedException.class, () -> txService.getAll(2026));
        assertThrows(AccessDeniedException.class, () -> situationService.getSituation("prov1@x", 2026, null));

        // new exercice: normal operations resume, journal keeps everything
        TransactionRegieRequest t2 = new TransactionRegieRequest();
        t2.setProvinceId(p1.getId()); t2.setCompteCode("611"); t2.setMontant(new BigDecimal("50"));
        t2.setDateTransaction(LocalDateTime.now());
        a.setMontant(new BigDecimal("100"));
        plafondService.alimenter(plafond.getId(), a, "prov1@x");
        assertEquals(2027, txService.create(t2, "prov1@x").getExercice());

        as("admin@x", "ADMIN");
        List<JournalResponse> j26 = historique.getAll(2026);
        assertTrue(j26.stream().anyMatch(j -> "CLOTURE".equals(j.action())));
        assertTrue(j26.stream().anyMatch(j -> "ANNULATION".equals(j.action())));
        assertTrue(j26.stream().anyMatch(j -> "CONFIRMATION_DEPENSE".equals(j.action())));
        assertTrue(j26.stream().allMatch(j -> j.details() == null || j.details().startsWith("[")));
        assertTrue(historique.getAll(null).stream().anyMatch(j -> "ALIMENTATION".equals(j.action()) && j.exercice() == 2027));

        // admin edit of a plafond is journalised with before/after
        PlafondRegieRequest upd = new PlafondRegieRequest();
        upd.setProvinceId(p1.getId()); upd.setCompteCode("611"); upd.setLibelle("Fournitures 2");
        upd.setPlafondAnnuel(new BigDecimal("900")); upd.setPlafondEncaissement(new BigDecimal("100"));
        upd.setPlafondMaxFacture(new BigDecimal("500"));
        plafondService.update(plafond.getId(), upd);
        JournalResponse mod = historique.getAll(null).stream().filter(j -> "MODIFICATION".equals(j.action()) && "PLAFOND".equals(j.entite())).findFirst().orElseThrow();
        assertTrue(mod.details().contains("Libelle"));
        assertEquals("admin@x", mod.createdBy());
    }

    @Test
    void closureBlockedWhenSoldeOrPending() {
        as("prov1@x", "PROV");
        AlimentationRequest a = new AlimentationRequest(); a.setMontant(new BigDecimal("300"));
        plafondService.alimenter(plafond.getId(), a, "prov1@x");
        as("admin@x", "ADMIN");
        CloturePrecheckResponse p = clotureService.precheck(p1.getId(), 2026);
        assertFalse(p.cloturable());
        assertThrows(IllegalArgumentException.class, () -> clotureService.cloturer(p1.getId(), 2026));
    }

    private Region region(String n) { Region r = new Region(); r.setName(n); return r; }

    private void user(String email, Role role, Region r, Province p) {
        User u = new User(); u.setEmail(email); u.setPassword("x"); u.setRole(role); u.setRegion(r); u.setProvince(p);
        users.save(u);
    }

    private void as(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
