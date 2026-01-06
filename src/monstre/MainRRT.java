package monstre;

public class MainRRT {
    public static void main(String[] args) {
        Map map = new Map(600, 600);
        RRT rrt = new RRT(map);

        // Départ en haut à gauche, arrivée en bas à droite du labyrinthe
        Noeud chemin = rrt.trouverChemin(30, 30, 570, 570);

        if (chemin != null) {
            System.out.println("Chemin trouvé !");

            // Créer le monstre à la position de départ
            Monstre monstre = new Monstre(30, 30);
            monstre.setChemin(chemin);

            // Affichage visuel avec animation du monstre
            RRTVisualisation.afficherAvecMonstre(map, rrt.getNoeuds(), rrt.getDebut(), rrt.getFin(), monstre);
        } else {
            System.out.println("Aucun chemin trouvé.");
            // Affichage visuel sans monstre
            RRTVisualisation.afficher(map, rrt.getNoeuds(), rrt.getDebut(), rrt.getFin());
        }
    }
}
