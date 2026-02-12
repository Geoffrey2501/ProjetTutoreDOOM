package monstre;

import javax.swing.*;

public class MainRRT {
    public static void main(String[] args) {
        // 1. Initialisation des données (Modèle)
        Map map = new Map(600, 600);
        RRT rrt = new RRT(map);

        // Calcul du chemin
        Noeud cheminFinal = rrt.trouverChemin(30, 30, 570, 570);

        if (cheminFinal != null) {
            System.out.println("Chemin trouvé !");

            // Création du monstre
            Monstre monstre = new Monstre(30, 30, map);
            monstre.setChemin(cheminFinal);

            SwingUtilities.invokeLater(() -> {
                // 2. Création de la fenêtre et de la Vue
                JFrame frame = new JFrame("Projet DOOM - Architecture MVC");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                // On passe les données à la vue
                RRTVisualisation visualisation = new RRTVisualisation(
                        map, rrt.getNoeuds(), rrt.getDebut(), rrt.getFin(), monstre
                );

                frame.add(visualisation);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                // 3. Création et démarrage du Contrôleur
                SimulateurMonstre simulateur = new SimulateurMonstre(visualisation, monstre);
                simulateur.demarrer();
            });

        } else {
            System.out.println("Aucun chemin trouvé.");
        }
    }
}