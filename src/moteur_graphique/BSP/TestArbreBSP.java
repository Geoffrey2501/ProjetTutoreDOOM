package moteur_graphique.BSP;

public class TestArbreBSP {

    public static void main(String[] args) {
        // Création d'une liste de murs fictifs (forme d'une pièce simple)
        Mur[] murs = new Mur[] {
            // Mur du haut
            new Mur(0, 0, 100, 0, "mur_haut"),
            // Mur de droite
            new Mur(100, 0, 100, 100, "mur_droit"),
            // Mur du bas
            new Mur(100, 100, 0, 100, "mur_bas"),
            // Mur de gauche
            new Mur(0, 100, 0, 0, "mur_gauche"),
            // Mur intérieur diagonal
            new Mur(25, 25, 75, 75, "mur_diagonal"),
            // Mur intérieur horizontal
            new Mur(50, 30, 90, 30, "mur_interieur")
        };

        // Construire l'arbre BSP
        ArbreBSP arbre = new ArbreBSP();
        MapMur map = new MapMur(murs);
        NoeudBSP racine = arbre.construireBSP(map);
    }
}
