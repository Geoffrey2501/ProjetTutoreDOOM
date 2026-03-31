package moteur_graphique.BSP;

import entite.Joueur;

import game.GameConfig;

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

    public List<Mur> getMursVisibles(double x, double y) {
        //à 360 degres, on appelle juste 3 fois getMursVisiblesAngle avec un fov de 120, et on merge les listes en supprimant les doublons
        //180 de fov et 360 ne fonctionnent pas. 120x3 = 360 donc on peut scan tous les murs visibles
        List<Mur> mursVisibles = new ArrayList<>();
        mursVisibles.addAll(getMursVisiblesAngle(x, y, 0, 120));
        mursVisibles.addAll(getMursVisiblesAngle(x, y, 120, 120));
        mursVisibles.addAll(getMursVisiblesAngle(x, y, 240, 120));

        //supprimer les doublons
        List<Mur> mursVisiblesSansDoublons = new ArrayList<>();
        for (Mur m : mursVisibles) {
            if (!mursVisiblesSansDoublons.contains(m)) {
                mursVisiblesSansDoublons.add(m);
            }
        }

        return mursVisiblesSansDoublons;
    }

    public List<Mur> getMursVisiblesAngle(double x, double y, double angle, int fov) {
        //on fait un algo similaire à render, juste on n'a pas besoin de la reel taille d'écran.
        //on peut aussi avoir 1 de fov par exemple juste pour avoir le mur en face et limiter les calculs
        int width = 720;
        int height = 1080;

        FrontToBack frontToBack = new FrontToBack(this.arbreBSP, x, y);
        FilledScreen filledScreen = new FilledScreen(width);
        WallCalcul wallCalcul = new WallCalcul();

        List<Mur> mursVisibles = new ArrayList<>();

        Mur m = frontToBack.getNextWall();
        while(m != null && !filledScreen.isFull()) {
            FourPoints points = wallCalcul.getFourPoints(m, x, y, fov, angle, width, height);
            if (points != null) {
                List<FourPoints> pointsAfterFilledScreen = filledScreen.add(points);
                if (pointsAfterFilledScreen != null) {
                    mursVisibles.add(m);
                    //moins optimisé, car on peut avoir des murs partiellement visibles
                    //mais en soit plus simple pour les calculs de collisions au final ?
                    //à voir
                }
            }
            m = frontToBack.getNextWall();
        }

        return mursVisibles;
    }
}
