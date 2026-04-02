package monstre;

import entite.Joueur;

import javax.swing.*;
import java.awt.event.*;

public class MainRRT {

    public static void main(String[] args) {
        // 1. Initialisation des données
        Map map = new Map(600, 600);

        // Création du joueur (cible potentielle)
        Joueur joueur = new Joueur("Joueur", 300, 300, 0);
        Target target = new Target(joueur);

        // Créer une liste de monstres pour tester la séparation et l'alerte
        java.util.List<Monstre> listeMonstres = new java.util.ArrayList<>();

        // Définir les points de patrouille (on peut les partager ou non)
        Noeud pointA = new Noeud(50, 50);
        Noeud pointB = new Noeud(550, 50);

        // Monstre 1
        Monstre monstre1 = new Monstre(30, 30, map);
        monstre1.setTarget(target);
        monstre1.setPointsPatrouille(pointA, pointB);
        listeMonstres.add(monstre1);

        // Monstre 2 (plus proche de l'autre point pour tester les trajectoires croisées)
        Monstre monstre2 = new Monstre(550, 40, map);
        monstre2.setTarget(target);
        monstre2.setPointsPatrouille(pointB, pointA); // patrouille inversée
        listeMonstres.add(monstre2);

        // Monstre 3 (plus en retrait)
        Monstre monstre3 = new Monstre(300, 100, map);
        monstre3.setTarget(target);
        monstre3.setPointsPatrouille(new Noeud(250, 100), new Noeud(350, 100));
        listeMonstres.add(monstre3);

        RRT rrt = new RRT(map);

        SwingUtilities.invokeLater(() -> {
            // 2. Création de la fenêtre
            JFrame frame = new JFrame("Test Multi-Monstres: Séparation & Alerte | Clic: Déplacer joueur");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            RRTVisualisation visualisation = new RRTVisualisation(map, listeMonstres);
            visualisation.setJoueur(joueur);  // Pour afficher le joueur
            visualisation.setPointsPatrouille(pointA, pointB);  // Afficher les points de patrouille
            frame.add(visualisation);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // 5. Boucle de jeu (60 FPS)
            Timer gameLoop = new Timer(16, e -> {
                for (Monstre m : listeMonstres) {
                    m.update();
                }

                // Mettre à jour la visualisation du chemin (pour le premier monstre, juste indicatif)
                if (!listeMonstres.isEmpty()) {
                    visualisation.updateChemin(listeMonstres.get(0).getChemin());
                }

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