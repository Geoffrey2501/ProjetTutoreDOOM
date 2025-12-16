package game;

/**
 * Interface de callback pour les événements réseau
 * Permet au jeu de réagir aux mises à jour des autres joueurs
 */
public interface NetworkListener {

    /**
     * Appelé quand un joueur distant met à jour sa position
     * @param playerId L'identifiant du joueur
     * @param x Position X
     * @param y Position Y
     * @param angle Angle de vue (en radians)
     */
    void onPlayerPositionUpdate(String playerId, double x, double y, double angle);

    /**
     * Appelé quand un nouveau joueur rejoint la partie
     * @param playerId L'identifiant du nouveau joueur
     */
    void onPlayerJoin(String playerId);

    /**
     * Appelé quand un joueur quitte la partie
     * @param playerId L'identifiant du joueur qui part
     */
    void onPlayerLeave(String playerId);
}

