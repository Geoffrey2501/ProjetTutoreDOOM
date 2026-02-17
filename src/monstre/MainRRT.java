package monstre;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainRRT {
    public static void main(String[] args) {
        // 1. Initialisation des données
        Map map = new Map(600, 600);
        Monstre monstre = new Monstre(30, 30, map);

        SwingUtilities.invokeLater(() -> {
            // 2. Création de la fenêtre et de la vue (sans RRT initial)
            JFrame frame = new JFrame("Projet DOOM - Cliquez pour déplacer le monstre");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            RRTVisualisation visualisation = new RRTVisualisation(map, monstre);
            frame.add(visualisation);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // 3. Démarrage de la boucle de jeu (tourne en continu, idle quand pas de chemin)
            SimulateurMonstre simulateur = new SimulateurMonstre(visualisation, monstre);
            simulateur.demarrer();

            // 4. Gestion du clic souris pour naviguer
            visualisation.addMouseListener(new MouseAdapter() {
                private SwingWorker<Noeud, Void> workerEnCours = null;

                @Override
                public void mouseClicked(MouseEvent e) {
                    int destX = e.getX();
                    int destY = e.getY();

                    // Ignorer les clics dans un mur
                    if (map.estDansMurAvecRayon(destX, destY, Monstre.RAYON)) {
                        return;
                    }

                    // Annuler le calcul RRT* précédent s'il est en cours
                    if (workerEnCours != null && !workerEnCours.isDone()) {
                        workerEnCours.cancel(true);
                    }

                    // Afficher immédiatement la cible
                    visualisation.setCible(destX, destY);

                    // Position actuelle du monstre (point de départ)
                    int startX = (int) monstre.getX();
                    int startY = (int) monstre.getY();

                    // Calcul RRT* en arrière-plan pour ne pas figer l'UI
                    workerEnCours = new SwingWorker<Noeud, Void>() {
                        private RRT rrt;

                        @Override
                        protected Noeud doInBackground() {
                            rrt = new RRT(map);
                            return rrt.trouverChemin(startX, startY, destX, destY);
                        }

                        @Override
                        protected void done() {
                            if (isCancelled()) return;
                            try {
                                Noeud cheminFinal = get();
                                if (cheminFinal != null) {
                                    // Mettre à jour le chemin du monstre
                                    monstre.setChemin(cheminFinal);
                                    // Mettre à jour la visualisation
                                    visualisation.updateRRT(rrt.getNoeuds(), cheminFinal);
                                } else {
                                    System.out.println("Aucun chemin trouvé vers (" + destX + ", " + destY + ")");
                                }
                            } catch (Exception ex) {
                                // Ignoré si annulé
                            }
                        }
                    };
                    workerEnCours.execute();
                }
            });
        });
    }
}