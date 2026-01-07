package moteur_graphique.BSP;

import java.util.ArrayList;
import java.util.List;

public class ArbreBSP {

    private NoeudBSP racine;
    // Tolérance pour éviter les erreurs de virgule flottante
    private static final double EPSILON = 1e-5;

    // Détermine de quel côté un point est par rapport à une ligne
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

    // Classe le mur : 1 (Gauche), -1 (Droite), 0 (Coupe)
    private int classerMur(Mur murPartition, Mur murAClasser) {
        int cote0 = coterDuPoint(murPartition, murAClasser.x0, murAClasser.y0);
        int cote1 = coterDuPoint(murPartition, murAClasser.x1, murAClasser.y1);

        // 1. CAS CRITIQUE AJOUTÉ : Le mur est aligné (colinéaire)
        if (cote0 == 0 && cote1 == 0) {
            // On décide arbitrairement de le mettre à GAUCHE (ou droite, peu importe, mais pas COUPER)
            // Cela empêche le mur de disparaître.
            return 1;
        }

        // 2. Si un point est sur la ligne, on utilise l'autre point pour décider
        if (cote0 == 0) return cote1;
        if (cote1 == 0) return cote0;

        // 3. Si les deux sont du même côté
        if (cote0 == cote1) {
            return cote0;
        }

        // 4. Sinon, ils sont de côtés opposés -> IL FAUT COUPER
        return 0;
    }

    private double[] calculerIntersection(Mur murPartition, Mur murACouper) {
        double denominateur = (murPartition.x0 - murPartition.x1) * (murACouper.y0 - murACouper.y1)
                - (murPartition.y0 - murPartition.y1) * (murACouper.x0 - murACouper.x1);

        // Sécurité anti-crash si jamais on essaie de couper deux murs parallèles (ne devrait plus arriver avec le fix classerMur)
        if (Math.abs(denominateur) < EPSILON) {
            return new double[]{murACouper.x0, murACouper.y0};
        }

        double produit_12 = (murPartition.x0 * murPartition.y1 - murPartition.y0 * murPartition.x1);
        double produit_34 = (murACouper.x0 * murACouper.y1 - murACouper.y0 * murACouper.x1);

        double pointX = (produit_12 * (murACouper.x0 - murACouper.x1) - (murPartition.x0 - murPartition.x1) * produit_34) / denominateur;
        double pointY = (produit_12 * (murACouper.y0 - murACouper.y1) - (murPartition.y0 - murPartition.y1) * produit_34) / denominateur;

        return new double[]{pointX, pointY};
    }

    private Mur[] couperMur(Mur murPartition, Mur murACouper) {
        double[] intersection = calculerIntersection(murPartition, murACouper);
        // On crée les deux nouveaux murs
        Mur mur1 = new Mur(murACouper.x0, murACouper.y0, intersection[0], intersection[1]);
        Mur mur2 = new Mur(intersection[0], intersection[1], murACouper.x1, murACouper.y1);
        return new Mur[]{mur1, mur2};
    }

    public NoeudBSP construireBSP(MapMur map) {
        if (map.getMurs().length == 0) return null;

        Mur murPartition = map.getMurs()[0];
        NoeudBSP noeud = new NoeudBSP(murPartition);
        noeud.mur = murPartition;

        List<Mur> mursGauche = new ArrayList<>();
        List<Mur> mursDroite = new ArrayList<>();

        for (int i = 1; i < map.getMurs().length; i++) {
            Mur mur = map.getMurs()[i];
            int classification = classerMur(murPartition, mur);

            if (classification == 1) {
                mursGauche.add(mur);
            } else if (classification == -1) {
                mursDroite.add(mur);
            } else {
                // CAS COUPURE (classification == 0)
                Mur[] mursCoupees = couperMur(murPartition, mur);
                int coteDepart = coterDuPoint(murPartition, mur.x0, mur.y0);

                if (coteDepart == 1) { // Début à Gauche
                    mursGauche.add(mursCoupees[0]);
                    mursDroite.add(mursCoupees[1]);
                } else if (coteDepart == -1) { // Début à Droite
                    mursDroite.add(mursCoupees[0]);
                    mursGauche.add(mursCoupees[1]);
                } else {
                    // SAUVETAGE : Si le point de départ est pile sur la ligne (coteDepart == 0)
                    // On regarde le point d'arrivée pour savoir l'orientation
                    int coteArrivee = coterDuPoint(murPartition, mur.x1, mur.y1);
                    if (coteArrivee == 1) { // Fin à Gauche -> Début (0) considéré Droite relative ? Non, juste l'inverse.
                        // Si Fin est Gauche, alors Début est "Neutre/Droite", donc :
                        mursDroite.add(mursCoupees[0]); // Partie "sur la ligne" ou presque
                        mursGauche.add(mursCoupees[1]);
                    } else {
                        mursGauche.add(mursCoupees[0]);
                        mursDroite.add(mursCoupees[1]);
                    }
                }
            }
        }

        noeud.gauche = construireBSP(new MapMur(mursGauche.toArray(new Mur[0])));
        noeud.droit = construireBSP(new MapMur(mursDroite.toArray(new Mur[0])));
        this.racine = noeud;
        return noeud;
    }

    public NoeudBSP getRacine() { return racine; }
}