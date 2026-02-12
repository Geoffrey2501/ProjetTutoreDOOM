package moteur_graphique.BSP;

import java.util.ArrayList;
import java.util.List;

public class WallCalcul {
    //hauteur du mur
    private static final double WALL_HEIGHT = 1.0;
    //hauteur des yeux du joueur
    private static final double PLAYER_HEIGHT = 0.5;

    public FourPoints getFourPoints(Mur mur, double playerX, double playerY, int FOV, double angle, int screenWidth, int screenHeight) {
        // Calculer les coordonnées des 4 coins du mur projeté à l'écran
        // en fonction de la position du joueur et de son angle de vue.
        // Les coordonnées retournées seront utilisées pour dessiner le mur en utilisant des polygones.

        //-- Legacy, code de Cyp (adapté) : --
        //cos et sin de l'angle de vue
        double cosA = Math.cos(angle);
        double sinA = Math.sin(angle);

        //transformer le mur en espace joueur
        double wx0 = mur.x0 - playerX;
        double wy0 = mur.y0 - playerY;
        double wx1 = mur.x1 - playerX;
        double wy1 = mur.y1 - playerY;

        //rotation inverse (monde -> caméra)
        double cx0 = -wx0 * sinA + wy0 * cosA;
        double cz0 =  wx0 * cosA + wy0 * sinA;
        double cx1 = -wx1 * sinA + wy1 * cosA;
        double cz1 =  wx1 * cosA + wy1 * sinA;

        // Rejet rapide (Si les deux points sont derrière le joueur)
        double epsilon = 0.1;

        if (cz0 < epsilon && cz1 < epsilon) {
            //on peut ignorer le mur
            return null;
        }


        // Clipping (Si un seul point est derrière)
        // Si point 0 derrière, on le coupe pour le ramener à z = 0.1
        if (cz0 < epsilon) {
            double t = (epsilon - cz0) / (cz1 - cz0);
            cz0 = epsilon;
            cx0 = cx0 + t * (cx1 - cx0);
        }

        if (cz1 < epsilon) {
            double t = (epsilon - cz1) / (cz0 - cz1);
            cz1 = epsilon;
            cx1 = cx1 + t * (cx0 - cx1);
        }

        // Projection perspective pour X
        double scale = (screenWidth / 2.0) / Math.tan(Math.toRadians(FOV) / 2.0);
        double sx0 = (cx0 / cz0) * scale + (screenWidth / 2.0);
        double sx1 = (cx1 / cz1) * scale + (screenWidth / 2.0);

        // Projection perspective pour Y (hauteur du mur)
        // Le haut du mur est à (WALL_HEIGHT - PLAYER_HEIGHT) au-dessus des yeux
        // Le bas du mur est à (-PLAYER_HEIGHT) en dessous des yeux (sol)
        double topHeight = WALL_HEIGHT - PLAYER_HEIGHT;
        double bottomHeight = -PLAYER_HEIGHT;

        double centerY = screenHeight / 2.0;

        // Côté gauche du mur (point 0)
        double yTop0 = centerY - (topHeight / cz0) * scale;
        double yBottom0 = centerY - (bottomHeight / cz0) * scale;

        // Côté droit du mur (point 1)
        double yTop1 = centerY - (topHeight / cz1) * scale;
        double yBottom1 = centerY - (bottomHeight / cz1) * scale;

        // Mise en ordre (gauche vers droite)
        // Il faut s'assurer que x_start < x_end pour la boucle de dessin
        double x_start, x_end, z_start, z_end;
        if (sx0 < sx1) {
            x_start = sx0;
            x_end = sx1;
            z_start = cz0;
            z_end = cz1;
        } else {
            x_start = sx1;
            x_end = sx0;
            z_start = cz1;
            z_end = cz0;
        }

        // Retourner les 4 points dans le bon ordre pour former un polygone
        // Ordre : haut-gauche, bas-gauche, bas-droite, haut-droite
        if (sx0 < sx1) {
            return new FourPoints(sx0, yTop0, sx0, yBottom0, sx1, yBottom1, sx1, yTop1);
        } else {
            return new FourPoints(sx1, yTop1, sx1, yBottom1, sx0, yBottom0, sx0, yTop0);
        }
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
