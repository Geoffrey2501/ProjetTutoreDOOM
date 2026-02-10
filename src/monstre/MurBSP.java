package monstre;

public class MurBSP extends Mur {

    public MurBSP(int x1, int y1, int x2, int y2) {
        super(x1, y1, x2, y2);
    }

    @Override
    public boolean esDansMur(int x, int y) {
        // Utilisation du produit en croix pour éviter la division par zéro
        // coeficientA == coeficientB équivaut à (y2-y1)*(x-x1) == (y-y1)*(x2-x1)
        long produitCroiseA = (long)(y2 - y1) * (x - x1);
        long produitCroiseB = (long)(y - y1) * (x2 - x1);

        if (produitCroiseA == produitCroiseB
                && x >= Math.min(x1, x2)
                && x <= Math.max(x1, x2)
                && y >= Math.min(y1, y2)
                && y <= Math.max(y1, y2)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean esDansMurSegment(int sx1, int sy1, int sx2, int sy2) {

        double abc = orientation(x1, y1, x2, y2, sx1, sy1);
        double abd = orientation(x1, y1, x2, y2, sx2, sy2);
        double cda = orientation(sx1, sy1, sx2, sy2, x1, y1);
        double cdb = orientation(sx1, sy1, sx2, sy2, x2, y2);

        if (abc * abd < 0
                && cda * cdb < 0
                && (abc == 0 && esDansMur(sx1, sy1))
                && (abd == 0 && esDansMur(sx2, sy2))
                && (cda == 0 && esDansMur(x1, y1))
                && (cdb == 0 && esDansMur(x2, y2))) {
            return true; // Les segments s'intersectent
        }


        return false;
    }

    private double orientation(int ax, int ay, int bx, int by, int cx, int cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);

    }

    @Override
    public boolean cercleIntersecte(int cx, int cy, int rayon) {
        return false;
    }
}
