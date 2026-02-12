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

    /**
     * Découpe un mur (défini par ses 4 points projetés à l'écran) en plusieurs segments visibles, en fonction des zones d'occlusion (murs devant).
     * @param wall Le mur à découper, défini par ses 4 points projetés à l'écran (haut-gauche, bas-gauche, bas-droite, haut-droite).
     * @param zones Une liste de zones d'occlusion, chacune définie par un intervalle [z_start, z_end] en coordonnées écran (X) qui représente une zone cachée par un mur devant.
     * @return Une liste de segments visibles du mur, chacun défini par ses 4 points projetés à l'écran, après découpage des zones d'occlusion.
     */
    public List<FourPoints> cropWall(FourPoints wall, List<int[]> zones) {
        List<FourPoints> result = new ArrayList<>();

        // Identifier les coordonnées X de départ et de fin du mur
        double startX = wall.x0;
        double endX = wall.x2;

        // Calculer la largeur totale du mur à l'écran
        double totalWidth = endX - startX;

        if (totalWidth <= 0.001) {
            return result;
        }

        // Liste des segments X qui sont encore visibles
        // On commence par le mur complet
        List<double[]> visibleSpans = new ArrayList<>();
        visibleSpans.add(new double[]{startX, endX});

        // Découper le mur avec chaque zone d'occlusion (murs devant)
        for (int[] zone : zones) {
            double zStart = zone[0];
            double zEnd = zone[1];
            List<double[]> nextSpans = new ArrayList<>();

            for (double[] span : visibleSpans) {
                double sStart = span[0];
                double sEnd = span[1];

                if (zEnd <= sStart || zStart >= sEnd) {
                    // La zone est hors du segment : le morceau reste totalement visible
                    nextSpans.add(span);
                } else if (zStart <= sStart && zEnd >= sEnd) {
                    // La zone recouvre complètement le segment : il est caché, on l'efface
                    // (On ne l'ajoute pas à nextSpans)
                } else if (zStart > sStart && zEnd < sEnd) {
                    // La zone tape en plein milieu du segment : on le coupe en deux
                    nextSpans.add(new double[]{sStart, zStart});
                    nextSpans.add(new double[]{zEnd, sEnd});
                } else if (zStart <= sStart && zEnd < sEnd) {
                    // La zone cache le côté gauche du segment
                    nextSpans.add(new double[]{zEnd, sEnd});
                } else if (zStart > sStart && zEnd >= sEnd) {
                    // La zone cache le côté droit du segment
                    nextSpans.add(new double[]{sStart, zStart});
                }
            }
            // On met à jour les morceaux visibles pour la zone occlusive suivante
            visibleSpans = nextSpans;
        }

        // Reconstruire les polygones (FourPoints) pour chaque morceau visible
        for (double[] span : visibleSpans) {
            double vx0 = span[0];
            double vx1 = span[1];

            // Ignorer les micro-fragments invisibles à l'écran (ex: < 0.1 pixel)
            if (vx1 - vx0 <= 0.1) continue;

            // Calcul du ratio de progression (entre 0.0 et 1.0) par rapport au mur d'origine
            double t0 = (vx0 - startX) / totalWidth;
            double t1 = (vx1 - startX) / totalWidth;

            // Interpolation linéaire des hauteurs
            // Rappel de l'ordre d'origine de tes points :
            // y0 = Top-Left, y1 = Bottom-Left, y2 = Bottom-Right, y3 = Top-Right

            double newYTop0 = wall.y0 + t0 * (wall.y3 - wall.y0);       // Nouveau Haut-Gauche
            double newYBottom0 = wall.y1 + t0 * (wall.y2 - wall.y1);    // Nouveau Bas-Gauche

            double newYTop1 = wall.y0 + t1 * (wall.y3 - wall.y0);       // Nouveau Haut-Droite
            double newYBottom1 = wall.y1 + t1 * (wall.y2 - wall.y1);    // Nouveau Bas-Droite

            // On ajoute ce nouveau sous-mur à rendre
            result.add(new FourPoints(
                    vx0, newYTop0,       // Point 0: Haut-gauche
                    vx0, newYBottom0,    // Point 1: Bas-gauche
                    vx1, newYBottom1,    // Point 2: Bas-droite
                    vx1, newYTop1        // Point 3: Haut-droite
            ));
        }

        return result;
    }
}
