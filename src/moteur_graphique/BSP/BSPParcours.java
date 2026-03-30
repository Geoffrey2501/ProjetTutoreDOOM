package moteur_graphique.BSP;

import entite.Joueur;
import moteur_graphique.GameRenderer;

import java.util.ArrayList;
import java.util.List;

public class BSPParcours implements GameRenderer {

    private Joueur joueur;
    private static final int FOV = 60;
    private ArbreBSP arbreBSP;

    public BSPParcours(Joueur joueur, MapMur map) {
        this.joueur = joueur;
        this.arbreBSP = new ArbreBSP();
        this.arbreBSP.construireBSP(map);
    }

    public void render(java.awt.Graphics g, int width, int height) {
        //on fait l'algo de parcours de l'arbre BSP pour dessiner les murs dans le bon ordre
        //on fait l'arbre, on parcours, on obtiens la liste de 4 points pour chaque mur à dessiner, on gère les murs déjà remplis avec FilledScreen, et on s'arrete si l'écran est rempli
        //on fini par appeler Renderer.renderFourPointsList(g, int width, int height, List<FourPoints> pointsList) pour dessiner les murs

        FrontToBack frontToBack = new FrontToBack(this.arbreBSP, joueur.getX(), joueur.getY());
        FilledScreen filledScreen = new FilledScreen(width);
        WallCalcul wallCalcul = new WallCalcul();

        List<FourPoints> pointsToDraw  = new ArrayList<FourPoints>();

        Mur m = frontToBack.getNextWall();
        while(m != null && !filledScreen.isFull()) {
            FourPoints points = wallCalcul.getFourPoints(m, joueur.getX(), joueur.getY(), FOV, joueur.getAngle(), width, height);
            if(points != null) {
                List<FourPoints> pointsAfterFilledScreen = filledScreen.add(points);
                if (pointsAfterFilledScreen != null) {
                    pointsToDraw.addAll(pointsAfterFilledScreen);
                }
            }
            m = frontToBack.getNextWall();
        }

        Renderer r = new Renderer();
        r.renderFourPointsList(g, width, height, pointsToDraw);
    }

}
