package monstre;

import javax.swing.*;
import java.awt.*;

/**
 * Contrôleur qui gère la boucle de jeu (Game Loop) et met à jour la logique.
 */
public class SimulateurMonstre implements Runnable {
    private final RRTVisualisation vue;
    private final Monstre monstre;

    private Thread gameThread;
    private volatile boolean running = false;

    // Configuration 60 FPS
    private static final int FPS = 60;
    private static final long TARGET_TIME = 1000 / FPS;

    public SimulateurMonstre(RRTVisualisation vue, Monstre monstre) {
        this.vue = vue;
        this.monstre = monstre;
    }

    public void demarrer() {
        if (running) return;
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void arreter() {
        running = false;
    }

    @Override
    public void run() {
        long start, elapsed, wait;

        while (running) {
            start = System.nanoTime();

            // 1. Mise à jour de la logique (Physique)
            update();

            // 2. Mise à jour de l'affichage (Rendu)
            vue.render();

            // INDISPENSABLE sur Linux/Windows pour la fluidité (évite le tearing)
            Toolkit.getDefaultToolkit().sync();

            // 3. Gestion du temps (Sleep) pour maintenir 60 FPS stables
            elapsed = System.nanoTime() - start;
            wait = TARGET_TIME - (elapsed / 1_000_000);

            if (wait < 5) wait = 5; // Pause minimale pour laisser respirer le CPU

            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Condition d'arrêt
            if (monstre.isArrived()) {
                System.out.println("Monstre arrivé à destination !");
                running = false;
            }
        }
    }

    private void update() {
        if (monstre != null) {
            monstre.update();
        }
    }
}