package moteur_graphique.BSP;

import java.util.List;

public class WallCalcul {
    public FourPoints getFourPoints(Mur mur, double playerX, double playerY, int FOV, double angle, int screenWidth, int screenHeight) {
        //TODO
        // Calculer les coordonnées des 4 coins du mur projeté à l'écran
        // en fonction de la position du joueur et de son angle de vue.
        // Les coordonnées retournées seront utilisées pour dessiner le mur en utilisant des polygones.
        return new FourPoints(0, 0, 0, 0, 0, 0, 0, 0); // Placeholder
    }

    public List<FourPoints> cropWall(FourPoints fourPoints, List<int[]> zones) {
        //TODO
        // Prendre les 4 points du mur projeté à l'écran, et les zones déjà remplies par d'autres murs plus proches du joueur (zones).
        // Retourner une liste de 4 points représentant les parties du mur qui ne sont pas recouvertes par les zones déjà remplies.
        // Utile pour optimiser le rendu en ne dessinant que les parties visibles des murs.
        // il faut clip les murs proprement pour reformer des 4points valides rendable sous forme de polygone (ex: un mur peut être coupé en 2 ou 3 morceaux visibles, il faut alors retourner une liste de 2 ou 3 FourPoints représentant ces morceaux)
        return null;
    }
}
