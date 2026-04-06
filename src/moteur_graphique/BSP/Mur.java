package moteur_graphique.BSP;

public class Mur extends monstre.Mur {
    public double x0, y0, x1, y1;
    public String texture;

    public Mur(double x0, double y0, double x1, double y1, String texture) {
        super((int)x0, (int)y0, (int)x1, (int)y1);
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.texture = texture;
    }

    public Mur(double x0, double y0, double x1, double y1) {
        super((int)x0, (int)y0, (int)x1, (int)y1);
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.texture = "";
    }

    public boolean estDansMur(double x, double y) {
        return x>= Math.min(x0, x1) && x <= Math.max(x0, x1) &&
                y >= Math.min(y0, y1) && y <= Math.max(y0, y1);
    }

    @Override
    public String toString() {
        return "Mur (" + x0 + "," + y0 + " -> " + x1 + "," + y1 + ") [" + texture + "]";
    }

    @Override
    public boolean esDansMur(int x, int y) {
        // Utilisation du produit en croix pour éviter la division par zéro
        long produitCroiseA = (long)(super.y2 - super.y1) * (x - super.x1);
        long produitCroiseB = (long)(y - super.y1) * (super.x2 - super.x1);

        if (produitCroiseA == produitCroiseB
                && x >= Math.min(super.x1, super.x2)
                && x <= Math.max(super.x1, super.x2)
                && y >= Math.min(super.y1, super.y2)
                && y <= Math.max(super.y1, super.y2)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean esDansMurSegment(int sx1, int sy1, int sx2, int sy2) {

        double abc = orientation(super.x1, super.y1, super.x2, super.y2, sx1, sy1);
        double abd = orientation(super.x1, super.y1, super.x2, super.y2, sx2, sy2);
        double cda = orientation(sx1, sy1, sx2, sy2, super.x1, super.y1);
        double cdb = orientation(sx1, sy1, sx2, sy2, super.x2, super.y2);

        if (abc * abd < 0
                && cda * cdb < 0
                && (abc == 0 && esDansMur(sx1, sy1))
                && (abd == 0 && esDansMur(sx2, sy2))
                && (cda == 0 && esDansMur(super.x1, super.y1))
                && (cdb == 0 && esDansMur(super.x2, super.y2))) {
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
