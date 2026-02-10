package game;

import Reseau.GestionConnection;
import entite.Joueur;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameNetworkAdapter {

    private final ServeurGame serveur;
    private Joueur localPlayer;
    private final Map<String, Joueur> remotePlayers;
    private final Set<String> notifiedPlayers; // Pour éviter les notifications en double
    private NetworkListener listener;

    /**
    * Constructeur de l'adaptateur réseau de jeu
     */
    public GameNetworkAdapter(String nodeId, String host, int port) {
        this.serveur = new ServeurGame(nodeId, host, port, this);
        this.remotePlayers = new ConcurrentHashMap<>();
        this.notifiedPlayers = ConcurrentHashMap.newKeySet();
    }
    /**
    * Définir le joueur local
     */
    public void setLocalPlayer(Joueur player) {
        this.localPlayer = player;
        this.localPlayer.setId(serveur.getNodeId());
    }

    /**
     * Définir le listener réseau
     * @param listener listener à définir
     */
    public void setNetworkListener(NetworkListener listener) {
        this.listener = listener;
    }

    /**
     * Démarrer le serveur réseau
     */
    public void start() {
        serveur.start();
    }

    /**
     * Connecter à un autre joueur
     */
    public void connectToPlayer(String playerId, String host, int port) {
        serveur.connectToNode(playerId, host, port);
    }

    /**
     * Envoyer la position du joueur local à tous les pairs
     */
    public void sendPlayerPosition() {
        if (localPlayer == null) return;
        String message = "MOVE:" + localPlayer.getId() + ":" + localPlayer.toNetworkString();
        serveur.broadcastToPeers(message);
    }

    /**
     * Envoyer immédiatement la position du joueur local à tous les pairs
     */
    public void sendPlayerPositionNow() {
        if (localPlayer == null) return;
        String message = "MOVE:" + localPlayer.getId() + ":" + localPlayer.toNetworkString();
        serveur.broadcastToPeers(message);
    }

    /**
     * Envoyer la position du joueur local à un pair spécifique
     */
    void sendPlayerPositionTo(GestionConnection peer) {
        if (localPlayer == null) return;
        String message = "MOVE:" + localPlayer.getId() + ":" + localPlayer.toNetworkString();
        peer.sendMessage(message);
    }

    /**
     * Gérer la réception de la position d'un autre joueur
     * @param playerId identifiant du joueur
     * @param positionData données de position reçues
     */
    void onPositionReceived(String playerId, String positionData) {
        if (localPlayer != null && playerId.equals(localPlayer.getId())) {
            return;
        }

        boolean isNewPlayer = false;
        Joueur remotePlayer;

        // Synchroniser pour éviter la création de doublons
        synchronized (remotePlayers) {
            remotePlayer = remotePlayers.get(playerId);

            if (remotePlayer == null) {
                // Créer le joueur sans position (non initialisé)
                remotePlayer = new Joueur(playerId);
                // Parser les coordonnées
                if (!remotePlayer.fromNetworkString(positionData)) {
                    // Si le parsing échoue, ne pas ajouter ce joueur
                    return;
                }
                remotePlayers.put(playerId, remotePlayer);

                // Vérifier si on a déjà notifié ce joueur
                if (notifiedPlayers.add(playerId)) {
                    isNewPlayer = true;
                }
            } else {
                remotePlayer.fromNetworkString(positionData);
            }
        }

        // Ne notifier que si la position est valide
        if (listener != null && remotePlayer.isPositionInitialized()) {
            if (isNewPlayer) {
                listener.onPlayerJoin(playerId);
            }
            listener.onPlayerPositionUpdate(
                playerId,
                remotePlayer.getX(),
                remotePlayer.getY(),
                remotePlayer.getAngle()
            );
        }
    }

    /**
     * Gérer la déconnexion d'un joueur
     * @param playerId identifiant du joueur déconnecté
     */
    void onPlayerDisconnected(String playerId) {
        Joueur removed = remotePlayers.remove(playerId);
        notifiedPlayers.remove(playerId); // Permettre une re-notification si le joueur revient
        if (removed != null && listener != null) {
            listener.onPlayerLeave(playerId);
        }
    }

    /**
     * Obtenir la liste des joueurs distants
     * @return Map des joueurs distants
     */
    public Map<String, Joueur> getRemotePlayers() {
        return remotePlayers;
    }

    /**
     * Obtenir un joueur distant par son ID
     * @param playerId identifiant du joueur
     * @return Joueur distant ou null s'il n'existe pas
     */
    public Joueur getRemotePlayer(String playerId) {
        return remotePlayers.get(playerId);
    }

    /**
     * Arrêter le serveur réseau
     */
    public void shutdown() {
        serveur.shutdown();
    }
}
