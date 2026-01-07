package moteur_graphique.BSP;

import java.util.ArrayList;
import java.util.List;

public class ArbreBSP {

    private NoeudBSP racine;

    //une fonction pour déterminer de quel côté un point est par rapport à une ligne
    private int coterDuPoint(Mur murPartition, double pointX, double pointY) {
        double dx = murPartition.x1 - murPartition.x0;
        double dy = murPartition.y1 - murPartition.y0;

        double px = pointX - murPartition.x0;
        double py = pointY - murPartition.y0;

        double produitCroise = dx * py - dy * px;

        if (produitCroise > 0) {
            return 1; // gauche
        } else if (produitCroise < 0) {
            return -1; // droite
        } else {
            return 0; // sur ligne
        }
    }

    //une fonction pour dire si un mur est à gauche, droite, ou coupé par le mur
    private int classerMur(Mur murPartition, Mur murAClasser) {
        int cote0 = coterDuPoint(murPartition, murAClasser.x0, murAClasser.y0);
        int cote1 = coterDuPoint(murPartition, murAClasser.x1, murAClasser.y1);

        if ((cote0 == 1 && cote1 == 1) || (cote0 == 0 && cote1 == 1) || (cote0 == 1 && cote1 == 0)) {
            return 1; // gauche
        } else if ((cote0 == -1 && cote1 == -1) || (cote0 == 0 && cote1 == -1) || (cote0 == -1 && cote1 == 0)) {
            return -1; // droite
        } else {
            return 0; // coupe
        }
    }

    //fonction pour calculer le point d'intersection entre deux murs
    private double[] calculerIntersection(Mur murPartition, Mur murACouper) {
        double denominateur = (murPartition.x0 - murPartition.x1) * (murACouper.y0 - murACouper.y1)
                - (murPartition.y0 - murPartition.y1) * (murACouper.x0 - murACouper.x1);

        double produit_12 = (murPartition.x0 * murPartition.y1 - murPartition.y0 * murPartition.x1);
        double produit_34 = (murACouper.x0 * murACouper.y1 - murACouper.y0 * murACouper.x1);

        double pointX = (produit_12 * (murACouper.x0 - murACouper.x1) - (murPartition.x0 - murPartition.x1) * produit_34) / denominateur;
        double pointY = (produit_12 * (murACouper.y0 - murACouper.y1) - (murPartition.y0 - murPartition.y1) * produit_34) / denominateur;

        return new double[]{pointX, pointY};
    }

    //fonction pour couper un mur en deux à partir d'un mur de partition
    private Mur[] couperMur(Mur murPartition, Mur murACouper) {
        double[] intersection = calculerIntersection(murPartition, murACouper);
        double pointIntersectionX = intersection[0];
        double pointIntersectionY = intersection[1];

        Mur mur1 = new Mur(murACouper.x0, murACouper.y0, pointIntersectionX, pointIntersectionY);
        Mur mur2 = new Mur(pointIntersectionX, pointIntersectionY, murACouper.x1, murACouper.y1);

        return new Mur[]{mur1, mur2};
    }

    //fonction récursive pour construire l'arbre BSP
    public NoeudBSP construireBSP(MapMur map) {
        //si c’est vide on a fini
        if (map.getMurs().length == 0) {
            return null;
        }

        //choisir un mur
        //on choisit le premier, mais on peut imaginer en choisir un autre … c’est un peu aléatoire ici,
        //il faut tester plusieur cas et voir si on arrive à un arbre avec moins de noeud (comme doom)
        Mur murPartition = map.getMurs()[0];

        //créer le nœud
        NoeudBSP noeud = new NoeudBSP(murPartition);
        noeud.mur = murPartition;

        //listes pour les murs à gauche et à droite, vides au début
        List<Mur> mursGauche = new ArrayList<>();
        List<Mur> mursDroite = new ArrayList<>();

        //parcourir les autres murs (on démarre à 1 pour ignorer le mur parent, ATTENTION si on
        //utilise un autre mur que le premier comme parent)
        for (int i = 1; i < map.getMurs().length; i++) {
            Mur mur = map.getMurs()[i];
            int classification = classerMur(murPartition, mur);

            if (classification == 1) {
                mursGauche.add(mur);
            } else if (classification == -1) {
                mursDroite.add(mur);
            } else if (classification == 0) {
                //couper le mur en deux
                Mur[] mursCoupees = couperMur(murPartition, mur);
                mursGauche.add(mursCoupees[0]);
                mursDroite.add(mursCoupees[1]);
            }
        }

        //construire récursivement les sous-arbres
        noeud.gauche = construireBSP(new MapMur(mursGauche.toArray(new Mur[0])));
        noeud.droit = construireBSP(new MapMur(mursDroite.toArray(new Mur[0])));

        this.racine = noeud;
        return noeud;
    }

    public NoeudBSP getRacine() {
        return racine;
    }
}
