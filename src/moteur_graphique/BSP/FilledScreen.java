package moteur_graphique.BSP;

import java.util.List;

/*
    * Cette classe permet de stocker la largeur d'écran qui sera remplie par les murs.
    * Permet d'ignorer / crop les murs qui seront déjà remplis ou partiellement remplis par d'autres murs plus proches du joueur.
 */
public class FilledScreen {
    private List<int[]> segmentRemplis;
    private int screenWidth;

    public FilledScreen(int screenWidth) {
        this.screenWidth = screenWidth;
        this.segmentRemplis = new java.util.ArrayList<>();
    }

    public List<FourPoints> add(FourPoints points) {
        //TODO
        //crop si besoin ? WallCalcul.cropWall(points, segmentRemplis) ?
        return null;
    }

    public boolean isFull() {
        //TODO
        return false;
    }
}
