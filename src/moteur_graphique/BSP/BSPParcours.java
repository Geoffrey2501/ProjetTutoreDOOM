package moteur_graphique.BSP;

import entite.Joueur;
import entite.Sprite;
import game.GameConfig;
import moteur_graphique.GameRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BSPParcours implements GameRenderer {

    private Joueur joueur;
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

        // On stocke des couples [Objet, MurOriginal] pour savoir où insérer les sprites
        // Objet est soit un Sprite, soit un FourPoints
        List<Object[]> orderedItems = new ArrayList<>();

        // 1. On récupère les murs visibles en Front-to-Back
        Mur m = frontToBack.getNextWall();
        while(m != null && !filledScreen.isFull()) {
            FourPoints points = wallCalcul.getFourPoints(m, joueur.getX(), joueur.getY(), GameConfig.FOV, joueur.getAngle(), width, height);
            if(points != null) {
                List<FourPoints> segments = filledScreen.add(points);
                if (segments != null) {
                    for (FourPoints seg : segments) {
                        orderedItems.add(new Object[]{seg, m}); // On garde la réf au mur original
                    }
                }
            }
            m = frontToBack.getNextWall();
        }

        // 2. On insère les sprites dynamiquement dans la liste
        double jX = joueur.getX();
        double jY = joueur.getY();

        // On trie d'abord les sprites par distance (proche en premier pour l'insertion FTB)
        List<Sprite> sortedSprites = new ArrayList<>(sprites);
        sortedSprites.sort((a, b) -> Double.compare(
                Math.pow(a.getX()-jX, 2) + Math.pow(a.getY()-jY, 2),
                Math.pow(b.getX()-jX, 2) + Math.pow(b.getY()-jY, 2)
        ));

        for (Sprite s : sortedSprites) {
            int insertIndex = orderedItems.size();
            for (int i = 0; i < orderedItems.size(); i++) {
                Mur wallRef = (Mur) orderedItems.get(i)[1];
                if (wallRef != null) {
                    int sideS = getSide(wallRef, s.getX(), s.getY());
                    int sideJ = getSide(wallRef, jX, jY);

                    // Un sprite est devant un mur si :
                    // 1. Il est du même côté que le joueur
                    // 2. Ou le joueur est sur la ligne (dans ce cas, le mur ne peut pas l'occlure)
                    if (sideS == sideJ || sideJ == 0) {
                        insertIndex = i;
                        break;
                    }
                }
            }
            orderedItems.add(insertIndex, new Object[]{s, null});
        }

        //On inverse la liste
        java.util.Collections.reverse(orderedItems);

        //plus qu'a dessiner, le culling sera fait automatiquement par l'ordre Back-to-Front
        renderer.renderWorld(g, width, height, orderedItems, joueur);
    }

    private int getSide(Mur wall, double px, double py) {
        double epsilon = 1e-5;
        double res = (wall.x1 - wall.x0) * (py - wall.y0) - (wall.y1 - wall.y0) * (px - wall.x0);
        if (res > epsilon) return 1;
        if (res < -epsilon) return -1;
        return 0; // Sur la ligne
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

    public void addSprite(Sprite sprite) { sprites.add(sprite); }
    public void removeSprite(Sprite sprite) { sprites.remove(sprite); }
}
