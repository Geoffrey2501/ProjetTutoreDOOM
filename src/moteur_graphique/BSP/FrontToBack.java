package moteur_graphique.BSP;

import java.util.Stack;

/**
 * Classe perméttant de parcourir l'arbre BSP segment dans l'ordre Front-to-Back (du plus proche au plus éloigné) pour un point de vue donné (x,y).
 * Elle est utilisée pour le rendu afin de dessiner les murs dans le bon ordre, et de pouvoir s'arreter au plus tôt afin d'optimiser le rendu (ex: ne pas dessiner les murs derrière un mur déjà dessiné).
 */
public class FrontToBack {
    private ArbreBSP arbre;
    private double x, y;

    private Stack<NoeudBSP> pile;
    private NoeudBSP courant;

    private static final double EPSILON = 1e-5;

    public FrontToBack(ArbreBSP arbre, double x, double y) {
        this.arbre = arbre;
        this.x = x;
        this.y = y;
        this.pile = new Stack<>();

        if (this.arbre != null) {
            this.courant = this.arbre.getRacine();
        }
    }

    /**
     * Retourne le prochain mur à dessiner dans l'ordre Front-to-Back, ou null s'il n'y en a plus.
     * @return le prochain mur à dessiner, ou null s'il n'y en a plus
     */
    public Mur getNextWall() {
        while (courant != null || !pile.isEmpty()) {
            if (courant != null) {
                pile.push(courant);
                courant = getCoteProche(courant);
            } else {
                courant = pile.pop();
                Mur murCourant = courant.mur;

                courant = getCoteLoin(courant);

                if (murCourant != null) {
                    return murCourant;
                }
            }
        }
        return null;
    }

    /**
     * Détermine de quel côté du mur se trouve le point (x,y) en utilisant le produit croisé.
     * @param murPartition le mur qui partitionne l'espace
     * @param pointX la coordonnée x du point de vue
     * @param pointY la coordonnée y du point de vue
     * @return 1 si le point est à gauche du mur, -1 s'il est à droite, 0 s'il est sur la ligne (à epsilon près)
     */
    private int coterDuPoint(Mur murPartition, double pointX, double pointY) {
        double dx = murPartition.x1 - murPartition.x0;
        double dy = murPartition.y1 - murPartition.y0;

        double px = pointX - murPartition.x0;
        double py = pointY - murPartition.y0;

        double produitCroise = dx * py - dy * px;

        if (produitCroise > EPSILON) {
            return 1; // gauche
        } else if (produitCroise < -EPSILON) {
            return -1; // droite
        } else {
            return 0; // sur la ligne (à epsilon près)
        }
    }

    /**
     * Retourne le côté de l'arbre BSP qui est du même côté que le point de vue (x,y) par rapport au mur de partitionnement du noeud.
     * @param noeud le noeud BSP dont on veut déterminer le côté proche
     * @return le noeud BSP du côté proche du point de vue, ou null si le noeud est une feuille
     */
    private NoeudBSP getCoteProche(NoeudBSP noeud) {
        int cote = coterDuPoint(noeud.mur, this.x, this.y);
        // Dans votre ArbreBSP, "1" correspond à gauche
        return (cote >= 0) ? noeud.gauche : noeud.droit;
    }

    /**
     * Retourne le côté de l'arbre BSP qui est du côté opposé au point de vue (x,y) par rapport au mur de partitionnement du noeud.
     * @param noeud le noeud BSP dont on veut déterminer le côté éloigné
     * @return le noeud BSP du côté éloigné du point de vue, ou null si le noeud est une feuille
     */
    private NoeudBSP getCoteLoin(NoeudBSP noeud) {
        int cote = coterDuPoint(noeud.mur, this.x, this.y);
        return (cote >= 0) ? noeud.droit : noeud.gauche;
    }
}

