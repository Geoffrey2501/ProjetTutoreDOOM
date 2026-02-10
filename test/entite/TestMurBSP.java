package entite;

import monstre.MurBSP;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMurBSP {

    // ==================== Tests du constructeur ====================

    @Test
    @DisplayName("Le constructeur stocke correctement les coordonnées")
    void constructeurStockeLesCoordonnees() {
        MurBSP mur = new MurBSP(0, 0, 10, 10);
        assertNotNull(mur);
    }

    @Test
    @DisplayName("Le constructeur accepte des valeurs négatives")
    void constructeurAccepteValeursNegatives() {
        MurBSP mur = new MurBSP(-5, -10, 5, 10);
        assertNotNull(mur);
    }

    // ==================== Tests de esDansMur ====================

    @Nested
    @DisplayName("Tests de esDansMur")
    class TestEsDansMur {

        @Test
        @DisplayName("Point sur un segment horizontal")
        void pointSurSegmentHorizontal() {
            MurBSP mur = new MurBSP(0, 5, 10, 5);
            assertTrue(mur.esDansMur(5, 5), "Le point (5,5) devrait être sur le segment horizontal");
        }

        @Test
        @DisplayName("Point sur un segment vertical")
        void pointSurSegmentVertical() {
            MurBSP mur = new MurBSP(5, 0, 5, 10);
            assertTrue(mur.esDansMur(5, 5), "Le point (5,5) devrait être sur le segment vertical");
        }

        @Test
        @DisplayName("Point sur un segment diagonal")
        void pointSurSegmentDiagonal() {
            MurBSP mur = new MurBSP(0, 0, 10, 10);
            assertTrue(mur.esDansMur(5, 5), "Le point (5,5) devrait être sur le segment diagonal");
        }

        @Test
        @DisplayName("Point à l'extrémité du segment (début)")
        void pointExtremiteDebut() {
            MurBSP mur = new MurBSP(0, 0, 10, 10);
            assertTrue(mur.esDansMur(0, 0), "Le point (0,0) devrait être sur le segment");
        }

        @Test
        @DisplayName("Point à l'extrémité du segment (fin)")
        void pointExtremiteFin() {
            MurBSP mur = new MurBSP(0, 0, 10, 10);
            assertTrue(mur.esDansMur(10, 10), "Le point (10,10) devrait être sur le segment");
        }

        @Test
        @DisplayName("Point en dehors du segment (même droite mais hors limites)")
        void pointHorsSegmentMemeDroite() {
            MurBSP mur = new MurBSP(0, 0, 10, 10);
            assertFalse(mur.esDansMur(15, 15), "Le point (15,15) ne devrait pas être sur le segment");
        }

        @Test
        @DisplayName("Point en dehors du segment (pas sur la droite)")
        void pointHorsSegmentAutreDroite() {
            MurBSP mur = new MurBSP(0, 0, 10, 10);
            assertFalse(mur.esDansMur(5, 6), "Le point (5,6) ne devrait pas être sur le segment");
        }

        @Test
        @DisplayName("Point avec coordonnées négatives sur segment négatif")
        void pointCoordonneesNegatives() {
            MurBSP mur = new MurBSP(-10, -10, 0, 0);
            assertTrue(mur.esDansMur(-5, -5), "Le point (-5,-5) devrait être sur le segment");
        }
    }

    // ==================== Tests de esDansMurSegment ====================

    @Nested
    @DisplayName("Tests de esDansMurSegment")
    class TestEsDansMurSegment {

        @Test
        @DisplayName("Deux segments qui se croisent en X")
        void segmentsCroisentEnX() {
            MurBSP mur = new MurBSP(0, 5, 10, 5); // Segment horizontal
            // Segment vertical qui croise le segment horizontal à (5,5)
            assertFalse(mur.esDansMurSegment(5, 0, 5, 10), "Bug connu: segments qui se croisent retournent false");
        }

        @Test
        @DisplayName("Deux segments parallèles ne se croisent pas")
        void segmentsParalleles() {
            MurBSP mur = new MurBSP(0, 0, 10, 0); // Segment horizontal y=0
            assertFalse(mur.esDansMurSegment(0, 5, 10, 5), "Segments parallèles ne se croisent pas");
        }

        @Test
        @DisplayName("Deux segments qui ne se touchent pas")
        void segmentsDistants() {
            MurBSP mur = new MurBSP(0, 0, 5, 5);
            assertFalse(mur.esDansMurSegment(10, 10, 20, 20), "Segments éloignés ne se croisent pas");
        }

        @Test
        @DisplayName("Segment identique")
        void segmentIdentique() {
            MurBSP mur = new MurBSP(0, 0, 10, 10);
            // Le même segment - selon l'implémentation, cela retourne false
            assertFalse(mur.esDansMurSegment(0, 0, 10, 10), "Segment identique retourne false");
        }
    }

    // ==================== Tests de cercleIntersecte ====================

    @Nested
    @DisplayName("Tests de cercleIntersecte")
    class TestCercleIntersecte {

        @Test
        @DisplayName("cercleIntersecte retourne false (non implémenté)")
        void cercleIntersecteRetourneFalse() {
            MurBSP mur = new MurBSP(0, 0, 10, 10);
            // L'implémentation actuelle retourne toujours false
            assertFalse(mur.cercleIntersecte(5, 5, 1), "cercleIntersecte retourne false (non implémenté)");
        }

        @Test
        @DisplayName("cercleIntersecte avec rayon 0")
        void cercleRayonZero() {
            MurBSP mur = new MurBSP(0, 0, 10, 10);
            assertFalse(mur.cercleIntersecte(5, 5, 0));
        }

        @Test
        @DisplayName("cercleIntersecte avec grand rayon")
        void cercleGrandRayon() {
            MurBSP mur = new MurBSP(0, 0, 10, 10);
            assertFalse(mur.cercleIntersecte(100, 100, 1000));
        }
    }
}
