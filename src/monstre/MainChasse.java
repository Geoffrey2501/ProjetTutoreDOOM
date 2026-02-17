package monstre;

import javax.swing.*;
import java.awt.*;

/**
 * Mode Chasse : le monstre poursuit le joueur dans le labyrinthe.
 *
 * - Joueur : contrôlé par les flèches directionnelles
 * - Monstre : utilise RRT* pour le pathfinding + Steering Behavior pour des mouvements fluides
 */
public class MainChasse {

    private static final int FPS = 60;
    private static final long INTERVALLE_RECALCUL_RRT = 2000; // Recalcul du chemin toutes les 2s

    public static void main(String[] args) {
        Map map = new Map(600, 600);
        Joueur joueur = new Joueur(570, 520, map);
        Monstre monstre = new Monstre(30, 30, map);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("DOOM - Chasse (Flèches pour se déplacer)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ChasseVisualisation vue = new ChasseVisualisation(map, monstre, joueur);
            joueur.setupKeyBindings(vue);
            frame.add(vue);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            vue.requestFocusInWindow();

            new GameLoop(map, joueur, monstre, vue).start();
        });
    }

    /**
     * Boucle de jeu principale.
     */
    private static class GameLoop extends Thread {
        private final Map map;
        private final Joueur joueur;
        private final Monstre monstre;
        private final ChasseVisualisation vue;
        private long dernierRecalcul = 0;
        private volatile boolean calculEnCours = false;

        GameLoop(Map map, Joueur joueur, Monstre monstre, ChasseVisualisation vue) {
            this.map = map;
            this.joueur = joueur;
            this.monstre = monstre;
            this.vue = vue;
            setDaemon(true);
        }

        @Override
        public void run() {
            final long targetTime = 1000 / FPS;

            while (true) {
                long start = System.nanoTime();

                update();
                vue.render();
                Toolkit.getDefaultToolkit().sync();

                sleep(targetTime, start);
            }
        }

        private void update() {
            joueur.update();

            // Lancer le calcul RRT* dans un thread séparé (non bloquant)
            if (doitRecalculerChemin() && !calculEnCours) {
                lancerCalculRRT();
            }

            // Le monstre suit son chemin (avec Steering Behavior intégré)
            if (monstre.aChemin()) {
                monstre.update();
            } else {
                monstre.seekDirect(joueur.getX(), joueur.getY());
            }

            // Collision monstre-joueur
            if (distanceJoueurMonstre() < 15) {
                System.out.println("Le monstre vous a attrapé !");
            }
        }

        private boolean doitRecalculerChemin() {
            return System.currentTimeMillis() - dernierRecalcul >= INTERVALLE_RECALCUL_RRT;
        }

        /**
         * Lance le calcul RRT* dans un thread séparé pour ne pas bloquer le jeu.
         */
        private void lancerCalculRRT() {
            calculEnCours = true;
            dernierRecalcul = System.currentTimeMillis();

            // Capturer les positions actuelles
            final int startX = (int) monstre.getX();
            final int startY = (int) monstre.getY();
            final int endX = (int) joueur.getX();
            final int endY = (int) joueur.getY();

            Thread rrtThread = new Thread(() -> {
                RRT rrt = new RRT(map);
                Noeud chemin = rrt.trouverChemin(startX, startY, endX, endY);

                // Mettre à jour sur l'EDT pour la thread-safety Swing
                SwingUtilities.invokeLater(() -> {
                    if (chemin != null) {
                        monstre.setChemin(chemin);
                        vue.updateRRT(rrt.getNoeuds(), chemin);
                    } else {
                        vue.clearRRT();
                    }
                    calculEnCours = false;
                });
            }, "RRT-Calculator");
            rrtThread.setDaemon(true);
            rrtThread.start();
        }

        private double distanceJoueurMonstre() {
            double dx = joueur.getX() - monstre.getX();
            double dy = joueur.getY() - monstre.getY();
            return Math.sqrt(dx * dx + dy * dy);
        }

        private void sleep(long targetTime, long startNano) {
            long elapsed = (System.nanoTime() - startNano) / 1_000_000;
            long wait = Math.max(5, targetTime - elapsed);
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
