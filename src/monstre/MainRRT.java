package monstre;

public class MainRRT {
    public static void main(String[] args) {
        Map map = new Map(600, 600);
        RTT rrt = new RTT(map, 30);
        Noeud chemin = rrt.trouverChemin(50, 50, 550, 550);

        if (chemin != null) {
            System.out.println("Chemin trouvé !");

            // Créer le monstre à la position de départ
            Monstre monstre = new Monstre(50, 50);
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
