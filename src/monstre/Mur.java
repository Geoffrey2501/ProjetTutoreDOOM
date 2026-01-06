package monstre;

/**
 * Représente un mur rectangulaire défini par deux coins opposés.
 * (x1, y1) = coin supérieur gauche
 * (x2, y2) = coin inférieur droit
 */
public class Mur {
    public int x1, y1, x2, y2;

    /**
     * Constructeur d'un mur rectangulaire.
     *
     * @param x1 coordonnée X du premier coin
     * @param y1 coordonnée Y du premier coin
     * @param x2 coordonnée X du coin opposé
     * @param y2 coordonnée Y du coin opposé
     */
    public Mur(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * Vérifie si un point (x, y) est à l'intérieur du mur.
     *
     * @param x coordonnée X du point
     * @param y coordonnée Y du point
     * @return true si le point est dans le mur, false sinon
     */
    public boolean esDansMur(int x, int y) {
        return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) && y >= Math.min(y1, y2) && y <= Math.max(y1, y2);
    }

    /**
     * Vérifie si un segment intersecte le mur (rectangle).
     * Utilise l'algorithme de Liang-Barsky pour une détection efficace en O(1).
     *
     * L'algorithme paramètre le segment comme P(t) = P1 + t*(P2-P1) où t ∈ [0,1]
     * et calcule les valeurs de t où le segment entre/sort du rectangle.
     *
     * @param sx1 coordonnée X du début du segment
     * @param sy1 coordonnée Y du début du segment
     * @param sx2 coordonnée X de la fin du segment
     * @param sy2 coordonnée Y de la fin du segment
     * @return true si le segment traverse ou touche le mur, false sinon
     */
    public boolean esDansMurSegment(int sx1, int sy1, int sx2, int sy2) {
        // Bornes du rectangle (mur)
        int minX = Math.min(this.x1, this.x2);
        int maxX = Math.max(this.x1, this.x2);
        int minY = Math.min(this.y1, this.y2);
        int maxY = Math.max(this.y1, this.y2);

        // Direction du segment
        double dx = sx2 - sx1;
        double dy = sy2 - sy1;

        // p[i] = direction perpendiculaire aux 4 bords (gauche, droite, bas, haut)
        // q[i] = distance signée du point de départ au bord
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {sx1 - minX, maxX - sx1, sy1 - minY, maxY - sy1};

        // tMin et tMax définissent l'intervalle du segment à l'intérieur du rectangle
        double tMin = 0;  // Début du segment
        double tMax = 1;  // Fin du segment

        // Vérifier chaque bord du rectangle
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                // Segment parallèle à ce bord
                if (q[i] < 0) {
                    // Segment complètement à l'extérieur de ce bord
                    return false;
                }
                // Sinon, segment entre les bords parallèles, on continue
            } else {
                // Calcul du paramètre t où le segment croise ce bord
                double t = q[i] / p[i];

                if (p[i] < 0) {
                    // Segment entre dans le rectangle par ce bord
                    tMin = Math.max(tMin, t);
                } else {
                    // Segment sort du rectangle par ce bord
                    tMax = Math.min(tMax, t);
                }

                // Si tMin > tMax, le segment ne traverse pas le rectangle
                if (tMin > tMax) {
                    return false;
                }
            }
        }

        // Le segment traverse le rectangle si on arrive ici
        return true;
    }
}