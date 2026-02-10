package entite;

import monstre.Mur;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMur {

    private static class MurStub extends Mur {
        MurStub(int x1, int y1, int x2, int y2) {
            super(x1, y1, x2, y2);
        }

        int getX1() { return x1; }
        int getY1() { return y1; }
        int getX2() { return x2; }
        int getY2() { return y2; }

        @Override
        public boolean esDansMur(int x, int y) {
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }

        @Override
        public boolean esDansMurSegment(int sx1, int sy1, int sx2, int sy2) {
            return esDansMur(sx1, sy1) || esDansMur(sx2, sy2);
        }

        @Override
        public boolean cercleIntersecte(int cx, int cy, int rayon) {
            return esDansMur(cx, cy) || rayon == 0;
        }
    }

    @Test
    void constructeurStockeLesCoordonnees() {
        MurStub mur = new MurStub(1, 2, 3, 4);

        assertEquals(1, mur.getX1());
        assertEquals(2, mur.getY1());
        assertEquals(3, mur.getX2());
        assertEquals(4, mur.getY2());
    }

    @Test
    void constructeurSupporteValeursNegatives() {
        MurStub mur = new MurStub(-10, 5, 7, -3);

        assertEquals(-10, mur.getX1());
        assertEquals(5, mur.getY1());
        assertEquals(7, mur.getX2());
        assertEquals(-3, mur.getY2());
    }

    @Test
    void lesMethodesUtilisentLesCoordonneesStockees() {
        MurStub mur = new MurStub(0, 0, 10, 10);

        assertTrue(mur.esDansMur(5, 5));
        assertFalse(mur.esDansMur(11, 5));
        assertTrue(mur.esDansMurSegment(5, 5, 20, 20));
        assertFalse(mur.esDansMurSegment(20, 20, 30, 30));
        assertTrue(mur.cercleIntersecte(5, 5, 0));
    }
}