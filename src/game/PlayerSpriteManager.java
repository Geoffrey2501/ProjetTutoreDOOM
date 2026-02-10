package game;

import entite.Joueur;
import entite.Sprite;
import moteur_graphique.raycasting.Raycasting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les sprites des joueurs distants dans le jeu multijoueur.
 * Responsable de la création, mise à jour et suppression des sprites.
 */
public class PlayerSpriteManager {

    private final Map<String, Sprite> playerSprites;
    private final Raycasting raycasting;
    private final GameNetworkAdapter network;

    public PlayerSpriteManager(Raycasting raycasting, GameNetworkAdapter network) {
        this.playerSprites = new ConcurrentHashMap<>();
        this.raycasting = raycasting;
        this.network = network;
    }

    /**
     * Met à jour tous les sprites des joueurs distants.
     * Interpole leur position pour un rendu fluide.
     * @param delta temps écoulé depuis la dernière mise à jour
     */
    public void update(double delta) {
        for (Map.Entry<String, Joueur> entry : network.getRemotePlayers().entrySet()) {
            String playerId = entry.getKey();
            Joueur remotePlayer = entry.getValue();

            // Interpolation pour un mouvement fluide
            remotePlayer.interpolate(delta);

            // Mise à jour de la position du sprite
            Sprite sprite = playerSprites.get(playerId);
            if (sprite != null) {
                sprite.setX(remotePlayer.getX());
                sprite.setY(remotePlayer.getY());
            }
        }
    }

    /**
     * Gère la mise à jour de position d'un joueur distant.
     * Crée le sprite si nécessaire.
     * @param playerId identifiant du joueur
     * @param x nouvelle position X
     * @param y nouvelle position Y
     */
    public void onPlayerPositionUpdate(String playerId, double x, double y) {
        synchronized (playerSprites) {
            if (!playerSprites.containsKey(playerId)) {
                Joueur remotePlayer = network.getRemotePlayer(playerId);
                if (remotePlayer != null && remotePlayer.isPositionInitialized()) {
                    Sprite playerSprite = new Sprite(x, y, GameConfig.PLAYER_SPRITE_PATH, playerId);
                    playerSprites.put(playerId, playerSprite);
                    raycasting.addSprite(playerSprite);
                }
            }
        }
    }

    /**
     * Gère l'arrivée d'un nouveau joueur.
     * Crée son sprite s'il a une position initialisée.
     * @param playerId identifiant du joueur
     * @return true si le sprite a été créé
     */
    public boolean onPlayerJoin(String playerId) {
        synchronized (playerSprites) {
            if (playerSprites.containsKey(playerId)) {
                return false;
            }

            Joueur remotePlayer = network.getRemotePlayer(playerId);
            if (remotePlayer != null && remotePlayer.isPositionInitialized()) {
                Sprite playerSprite = new Sprite(
                        remotePlayer.getX(),
                        remotePlayer.getY(),
                        GameConfig.PLAYER_SPRITE_PATH,
                        playerId
                );
                playerSprites.put(playerId, playerSprite);
                raycasting.addSprite(playerSprite);
                return true;
            }
        }
        return false;
    }

    /**
     * Gère le départ d'un joueur.
     * Supprime son sprite du jeu.
     * @param playerId identifiant du joueur
     * @return true si le sprite a été supprimé
     */
    public boolean onPlayerLeave(String playerId) {
        Sprite sprite;
        synchronized (playerSprites) {
            sprite = playerSprites.remove(playerId);
        }
        if (sprite != null) {
            raycasting.removeSprite(sprite);
            return true;
        }
        return false;
    }

    public Map<String, Sprite> getPlayerSprites() {
        return playerSprites;
    }
}

