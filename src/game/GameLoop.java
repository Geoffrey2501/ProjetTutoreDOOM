package game;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gère la boucle de jeu principale.
 * Responsable du timing et de l'appel des méthodes update/render à une fréquence constante.
 */
public class GameLoop implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(GameLoop.class.getName());

    private final GameLoopListener listener;
    private volatile boolean running;

    /**
     * Constructeur de la boucle de jeu.
     * @param listener
     */
    public GameLoop(GameLoopListener listener) {
        this.listener = listener;
        this.running = false;
    }
    /**
     * Démarre la boucle de jeu.
     * Appelle les méthodes update et render à une fréquence constante définie par GameConfig.
     */
    @Override
    public void run() {
        running = true;
        long lastLoopTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            long updateLength = now - lastLoopTime;
            lastLoopTime = now;
            double delta = updateLength / 1_000_000_000.0;

            // Mise à jour de la logique
            listener.update(delta);

            // Rendu
            listener.render();

            // Attente pour maintenir le FPS cible
            sleep(lastLoopTime);
        }

        listener.onShutdown();
    }

    /**
     * Attend le temps nécessaire pour maintenir le FPS cible.
     * @param lastLoopTime temps de début de la frame actuelle
     */
    private void sleep(long lastLoopTime) {
        try {
            long sleepTime = (lastLoopTime - System.nanoTime() + GameConfig.OPTIMAL_TIME) / 1_000_000;
            Thread.sleep(Math.max(0, sleepTime));
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Thread de la boucle de jeu interrompu", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Arrête la boucle de jeu.
     */
    public void stop() {
        running = false;
    }

    /**
     * @return true si la boucle est en cours d'exécution
     */
    public boolean isRunning() {
        return running;
    }
}

