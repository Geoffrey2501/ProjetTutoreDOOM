package moteur_graphique.BSP;

import entite.Joueur;
import entite.Sprite;
import moteur_graphique.GameRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BSPParcours implements GameRenderer {

    private Joueur joueur;
    private static final int FOV = 60;
    private ArbreBSP arbreBSP;

    private final Renderer renderer = new Renderer();

    private final List<Sprite> sprites = new CopyOnWriteArrayList<>();

    public BSPParcours(Joueur joueur, MapMur map) {
        this.joueur = joueur;
        this.arbreBSP = new ArbreBSP();
        this.arbreBSP.construireBSP(map);
    }

    public void render(java.awt.Graphics g, int width, int height) {
        FrontToBack frontToBack = new FrontToBack(this.arbreBSP, joueur.getX(), joueur.getY());
        FilledScreen filledScreen = new FilledScreen(width);
        WallCalcul wallCalcul = new WallCalcul();

        List<FourPoints> wallItems = new ArrayList<>();

        // 1. On récupère les murs visibles en Front-to-Back
        Mur m = frontToBack.getNextWall();
        while (m != null && !filledScreen.isFull()) {
            FourPoints points = wallCalcul.getFourPoints(m, joueur.getX(), joueur.getY(), FOV, joueur.getAngle(), width, height);
            if (points != null) {
                List<FourPoints> segments = filledScreen.add(points);
                if (segments != null) {
                    wallItems.addAll(segments);
                }
            }
            m = frontToBack.getNextWall();
        }

        // 2. Construire le z-buffer : pour chaque colonne écran, on stocke la profondeur caméra
        //    du mur le plus proche. Interpolation perspective-correcte (1/z).
        double[] zBuffer = new double[width];
        java.util.Arrays.fill(zBuffer, Double.MAX_VALUE);
        for (FourPoints fp : wallItems) {
            int xLeft  = (int) Math.max(0, fp.x0);
            int xRight = (int) Math.min(width - 1, fp.x2);
            double spanWidth = fp.x2 - fp.x0;
            if (spanWidth <= 0) continue;
            double invCzL = 1.0 / fp.cz0;
            double invCzR = 1.0 / fp.cz1;
            for (int x = xLeft; x <= xRight; x++) {
                double t  = (x - fp.x0) / spanWidth;
                double cz = 1.0 / (invCzL + t * (invCzR - invCzL));
                if (cz < zBuffer[x]) zBuffer[x] = cz;
            }
        }

        // 3. On inverse la liste pour le dessin Back-to-Front (algorithme du peintre)
        java.util.Collections.reverse(wallItems);

        // 4. Rendu : murs en Back-to-Front, sprites clippés colonne par colonne via le z-buffer
        renderer.renderWorld(g, width, height, wallItems, new ArrayList<>(sprites), joueur, zBuffer);
    }


    public void addSprite(Sprite sprite) { sprites.add(sprite); }
    public void removeSprite(Sprite sprite) { sprites.remove(sprite); }
}
