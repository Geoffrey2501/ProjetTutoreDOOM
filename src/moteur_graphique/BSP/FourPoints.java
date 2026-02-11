package moteur_graphique.BSP;

/**
 * Classe permettant de facilement stocker 4 points.
 * Les points représentent la position de 4 coins d'un mur projeté à l'écran, après calcul de perspective.
 * Utile pour le rendu des murs car on remplira avec des polygones maintenant
 */
public class FourPoints {
    public double x0, y0, x1, y1, x2, y2, x3, y3;

    public FourPoints(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.x3 = x3;
        this.y3 = y3;
    }
}
