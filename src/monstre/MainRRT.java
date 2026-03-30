package monstre;

import entite.Joueur;

import javax.swing.*;
import java.awt.event.*;

public class MainRRT {

    static void main() {
        // 1. Initialisation des données
        Map map = new Map(600, 600);
        Monstre monstre = new Monstre(30, 30, map);

        // Création du joueur (cible potentielle)
        Joueur joueur = new Joueur("Joueur", 300, 300, 0);
        Target target = new Target(joueur);

        // Configurer le monstre
        monstre.setTarget(target);              // Le joueur à détecter
        // Définir les points de patrouille
        Noeud pointA = new Noeud(50, 50);
        Noeud pointB = new Noeud(550, 50);
        monstre.setPointsPatrouille(pointA, pointB);

        RRT rrt = new RRT(map);

        SwingUtilities.invokeLater(() -> {
            // 2. Création de la fenêtre
            JFrame frame = new JFrame("Patrouille & Poursuite | Clic: Déplacer joueur | Espace: Reset");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            RRTVisualisation visualisation = new RRTVisualisation(map, monstre);
            visualisation.setJoueur(joueur);  // Pour afficher le joueur
            visualisation.setPointsPatrouille(pointA, pointB);  // Afficher les points de patrouille
            frame.add(visualisation);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);



            // 5. Boucle de jeu (60 FPS)
            Timer gameLoop = new Timer(16, e -> {
                monstre.update();

                // Mettre à jour la visualisation du chemin
                visualisation.updateChemin(monstre.getChemin());

                visualisation.repaint();
            });
            gameLoop.start();

            // 6. Gestion du clic souris pour déplacer le joueur
            visualisation.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int destX = e.getX();
                    int destY = e.getY();

                    // Ignorer les clics dans un mur
                    if (map.estDansMurAvecRayon(destX, destY, 5)) {
                        System.out.println("Position dans un mur, ignoré.");
                        return;
                    }

                    // Déplacer le joueur
                    joueur.setPosition(destX, destY, joueur.getAngle());
                    System.out.println("Joueur déplacé vers (" + destX + ", " + destY + ")");
                }
            });

            // Focus pour recevoir les événements clavier
            frame.requestFocus();
        });
    }
}