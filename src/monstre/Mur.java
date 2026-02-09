package monstre;

public abstract class Mur {
    protected int x1, y1, x2, y2;

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
    public abstract boolean esDansMur(int x, int y);

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
    public abstract boolean esDansMurSegment(int sx1, int sy1, int sx2, int sy2);

    /**
     * Vérifie si un cercle intersecte le mur (rectangle).
     * Trouve le point le plus proche du centre du cercle sur le rectangle
     * et vérifie si la distance est inférieure au rayon.
     *
     * @param cx coordonnée X du centre du cercle
     * @param cy coordonnée Y du centre du cercle
     * @param rayon rayon du cercle
     * @return true si le cercle touche ou chevauche le mur
     */
    public abstract boolean cercleIntersecte(int cx, int cy, int rayon);

}
