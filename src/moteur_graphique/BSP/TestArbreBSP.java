package moteur_graphique.BSP;

import java.util.Arrays;
import java.util.List;

public class TestArbreBSP {

    public static void main(String[] args) {
        MapMur map = new MapMur("assets/maps/mapBSP.txt");

        System.out.println("Murs chargés : " + map.getMurs().length);
        for (Mur m : map.getMurs()) {
            System.out.println(" -> " + m);
        }

        System.out.println("\n=== 3. CONSTRUCTION ARBRE BSP ===");
        ArbreBSP bsp = new ArbreBSP();
        NoeudBSP racine = bsp.construireBSP(map);

        System.out.println("\n=== 4. AFFICHAGE DE L'ARBRE ===");
        if (racine != null) {
            afficherArbre(racine, "", "RACINE");
        } else {
            System.out.println("L'arbre est vide !");
        }
    }

    /**
     * Affiche l'arbre récursivement avec indentation
     */
    public static void afficherArbre(NoeudBSP noeud, String indent, String type) {
        if (noeud == null) return;

        // Affichage du noeud courant
        System.out.println(indent + "[" + type + "] Séparateur : " + noeud.mur);

        // Appel récursif pour les enfants
        // On augmente l'indentation pour visualiser la profondeur
        if (noeud.gauche != null) {
            afficherArbre(noeud.gauche, indent + "    ", "GAUCHE (Devant)");
        }

        if (noeud.droit != null) {
            afficherArbre(noeud.droit, indent + "    ", "DROITE (Derrière)");
        }
    }
}
