package moteur_graphique.BSP;

import entite.Joueur;
import moteur_graphique.GameRenderer;

import java.util.ArrayList;
import java.util.List;

public class BSPParcours implements GameRenderer {

    private Joueur joueur;
    private MapMur map;
    private static final int FOV = 60;

    public BSPParcours(Joueur joueur, MapMur map) {
        this.joueur = joueur;
        this.map = map;
    }

    public void render(java.awt.Graphics g, int width, int height) {
        //on fait l'algo de parcours de l'arbre BSP pour dessiner les murs dans le bon ordre
        //on fait l'arbre, on parcours, on obtiens la liste de 4 points pour chaque mur à dessiner, on gère les murs déjà remplis avec FilledScreen, et on s'arrete si l'écran est rempli
        //on fini par appeler Renderer.renderFourPointsList(g, int width, int height, List<FourPoints> pointsList) pour dessiner les murs
        ArbreBSP arbreBSP = new ArbreBSP();
        arbreBSP.construireBSP(this.map);

        FrontToBack frontToBack = new FrontToBack(arbreBSP, joueur.getX(), joueur.getY());
        FilledScreen filledScreen = new FilledScreen(width);
        WallCalcul wallCalcul = new WallCalcul();

        List<FourPoints> pointsToDraw  = new ArrayList<FourPoints>();

        Mur m = frontToBack.getNextWall();
        while(m != null && !filledScreen.isFull()) {
            FourPoints points = wallCalcul.getFourPoints(m, joueur.getX(), joueur.getY(), FOV, joueur.getAngle(), width, height);
            if(points != null) {
                List<FourPoints> pointsAfterFilledScreen = filledScreen.add(points);
                if (pointsAfterFilledScreen != null) {
                    System.out.println("pointsAfterFilledScreen:" + pointsAfterFilledScreen.get(0).x0 + " " + pointsAfterFilledScreen.get(0).y0 + " " + pointsAfterFilledScreen.get(0).x1 + " " + pointsAfterFilledScreen.get(0).y1 + " " + pointsAfterFilledScreen.get(0).x2 + " " + pointsAfterFilledScreen.get(0).y2 + " " + pointsAfterFilledScreen.get(0).x3 + " " + pointsAfterFilledScreen.get(0).y3);
                    pointsToDraw.addAll(pointsAfterFilledScreen);
                }
            }
            m = frontToBack.getNextWall();
        }

        Renderer r = new Renderer();
        r.renderFourPointsList(g, width, height, pointsToDraw);
    }
}
