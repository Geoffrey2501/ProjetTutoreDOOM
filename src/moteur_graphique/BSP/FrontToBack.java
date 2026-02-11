package moteur_graphique.BSP;

/**
 * Classe perméttant de parcourir l'arbre BSP segment dans l'ordre Front-to-Back (du plus proche au plus éloigné) pour un point de vue donné (x,y).
 * Elle est utilisée pour le rendu afin de dessiner les murs dans le bon ordre, et de pouvoir s'arreter au plus tôt afin d'optimiser le rendu (ex: ne pas dessiner les murs derrière un mur déjà dessiné).
 */
public class FrontToBack {
    private ArbreBSP arbre;
    //coordonnées du point de vue
    private double x, y;

    public FrontToBack(ArbreBSP arbre, double x, double y) {
        this.arbre = arbre;
        this.x = x;
        this.y = y;
    }

    public Mur getNextWall() {
        //TODO
        return new Mur(0, 0, 0, 0); //Placeholder
    }
}
