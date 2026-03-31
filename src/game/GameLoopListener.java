package game;

/**
 * Interface pour recevoir les callbacks de la boucle de jeu.
 */
public interface GameLoopListener {
    /**
     * Appelé à chaque frame pour mettre à jour la logique du jeu.
     * @param delta temps écoulé depuis la dernière frame en secondes
     */
    void update(double delta);

    /**
     * Appelé à chaque frame pour effectuer le rendu.
     */
    void render();

    /**
     * Appelé quand la boucle de jeu se termine.
     */
    void onShutdown();

    /**
     * Reçoit la mise à jour du nombre d'images par seconde.
     */
    default void setFPS(int fps) {}
}