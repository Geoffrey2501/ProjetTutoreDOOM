package moteur_graphique.BSP;

import java.util.ArrayList;
import java.util.List;

/*
    * Cette classe permet de stocker la largeur d'écran qui sera remplie par les murs.
    * Permet d'ignorer / crop les murs qui seront déjà remplis ou partiellement remplis par d'autres murs plus proches du joueur.
 */
public class FilledScreen {
    private List<int[]> segmentRemplis;
    private int screenWidth;
    private WallCalcul w;

    public FilledScreen(int screenWidth) {
        this.screenWidth = screenWidth;
        this.segmentRemplis = new java.util.ArrayList<>();
        this.w = new WallCalcul();
    }

    /**
     * Ajoute un mur à l'écran, en vérifiant s'il est totalement ou partiellement caché par les murs déjà ajoutés dans la liste des segments remplis. Si le mur est totalement caché, il est ignoré.
     * Si le mur est partiellement caché, il est recoupé pour ne garder que les parties visibles, et les segments visibles sont ajoutés à la liste des segments remplis.
     * @param points Les 4 points du mur à ajouter, définis par leurs coordonnées (x0, y0), (x1, y1), (x2, y2), (x3, y3)
     * @return une liste de 4 points représentant les parties visibles du mur après recoupement, ou null si le mur est totalement caché
     */
    public List<FourPoints> add(FourPoints points) {
        int leftX = (int) Math.min(Math.min(points.x0, points.x1), Math.min(points.x2, points.x3));
        int rightX = (int) Math.max(Math.max(points.x0, points.x1), Math.max(points.x2, points.x3));

        if (estTotalementCache(leftX, rightX)){
            return null;
        }

        List<FourPoints> morceauxVisibles = w.cropWall(points, this.segmentRemplis);

        if (morceauxVisibles == null || morceauxVisibles.isEmpty()) {
            return null;
        }

        for (FourPoints morceau : morceauxVisibles) {
            int mLeftX = (int) Math.min(morceau.x0, morceau.x1);
            int mRightX = (int) Math.max(morceau.x2, morceau.x3);
            ajouterIntervalle(mLeftX, mRightX);
        }

        return morceauxVisibles;
    }

    /**
     * Ajoute un intervalle de pixels remplis à la liste des segments remplis, en fusionnant les segments qui se chevauchent ou sont adjacents.
     *
     * @param debut L'intervalle de pixels à ajouter, défini par sa position de début (inclus) et de fin (inclus)
     * @param fin L'intervalle de pixels à ajouter, défini par sa position de début (inclus) et de fin (inclus)
     */
    private void ajouterIntervalle(int debut, int fin) {
        List<int[]> newSegments = new ArrayList<>();
        int[] nouveau = new int[]{debut, fin};

        for (int[] segmentActuel : segmentRemplis) {
            if (segmentActuel[1] < nouveau[0]) {
                newSegments.add(segmentActuel);
            } else if (segmentActuel[0] > nouveau[1]) {
                newSegments.add(nouveau);
                nouveau = segmentActuel;
            } else {
                // Fusion
                nouveau[0] = Math.min(nouveau[0], segmentActuel[0]);
                nouveau[1] = Math.max(nouveau[1], segmentActuel[1]);
            }
        }

        newSegments.add(nouveau);
        this.segmentRemplis = newSegments;
    }

    /**
     * Vérifie si un mur est totalement caché par les segments déjà remplis. Un mur est totalement caché si son
     * intervalle horizontal [leftX, rightX] est entièrement englobé par au moins un des segments remplis.
     *
     * Comme nos segments sont toujours fusionnés (ex: [0, 200]),
     * si le mur est totalement caché, il sera FORCÉMENT englobé
     * tout entier à l'intérieur d'un seul de nos segments.
     * @param leftX L'intervalle horizontal du mur à vérifier
     * @param rightX L'intervalle horizontal du mur à vérifier
     * @return true si le mur est totalement caché, false sinon
     */
    private boolean estTotalementCache(int leftX, int rightX) {
        for (int[] segment : this.segmentRemplis) {
            if (segment[0] <= leftX && segment[1] >= rightX) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si l'écran est complètement rempli, c'est-à-dire si la liste des segments remplis contient un segment qui couvre toute la largeur de l'écran (de 0 à screenWidth).
     * @return true si l'écran est complètement rempli, false sinon
     */
    public boolean isFull() {
        if (this.segmentRemplis == null || this.segmentRemplis.isEmpty()) {
            return false;
        }
        int[] premierBloc = this.segmentRemplis.get(0);

        return premierBloc[0] <= 0 && premierBloc[1] >= this.screenWidth;
    }
}
