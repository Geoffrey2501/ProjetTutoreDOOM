package moteur_graphique.BSP;

import entite.Joueur;
import moteur_graphique.GameRenderer;

public class BSPParcours implements GameRenderer {

    private Joueur joueur;
    private MapMur map;
    private static final int FOV = 60;

    public BSPParcours(Joueur joueur, MapMur map) {
        this.joueur = joueur;
        this.map = map;
    }

    public void render(java.awt.Graphics g, int width, int height) {
        //TODO
        //on fait l'algo de parcours de l'arbre BSP pour dessiner les murs dans le bon ordre
        //on fait l'arbre, on parcours, on obtiens la liste de 4 points pour chaque mur à dessiner, on gère les murs déjà remplis avec FilledScreen, et on s'arrete si l'écran est rempli
        //on fini par appeler Renderer.renderFourPointsList(g, int width, int height, List<FourPoints> pointsList) pour dessiner les murs
    }
}
