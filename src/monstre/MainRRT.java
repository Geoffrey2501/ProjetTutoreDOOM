package monstre;

public class MainRRT {
    public static void main(String[] args) {
        Map map = new Map(600, 600);
        RRT rrt = new RRT(map);
        Noeud chemin = rrt.trouverChemin(50, 50, 550, 550);

        // Affichage visuel
        RRTVisualisation.afficher(map, rrt.getNoeuds(), rrt.getDebut(), rrt.getFin());

        if (chemin != null) {
            System.out.println("Chemin trouvé !");
        } else {
            System.out.println("Aucun chemin trouvé.");
        }
    }
}
