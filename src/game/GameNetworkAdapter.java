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
        if (localPlayer != null && playerId.equals(localPlayer.getId())) return;

        boolean isNewPlayer = false;
        Joueur remotePlayer;

        synchronized (remotePlayers) {
            remotePlayer = remotePlayers.get(playerId);
            if (remotePlayer == null) {
                remotePlayer = new Joueur(playerId);
                // On tente de parser la position
                if (!remotePlayer.fromNetworkString(positionData)) return;

                remotePlayers.put(playerId, remotePlayer);
                isNewPlayer = true;
            } else {
                remotePlayer.fromNetworkString(positionData);
            }
        }

        if (listener != null && remotePlayer.isPositionInitialized()) {
            // Si c'est un nouveau joueur, on DOIT appeler onPlayerJoin
            if (isNewPlayer) {
                listener.onPlayerJoin(playerId);
            }
            // Puis on met à jour sa position pour le rendu
            listener.onPlayerPositionUpdate(playerId, remotePlayer.getX(), remotePlayer.getY(), remotePlayer.getAngle());
        }
    }

    /**
     * Gérer la déconnexion d'un joueur
     * @param playerId identifiant du joueur déconnecté
     */
    void onPlayerDisconnected(String playerId) {
        System.out.println("[GameNetworkAdapter] onPlayerDisconnected appelé pour: " + playerId);

        Joueur removed = remotePlayers.remove(playerId);
        notifiedPlayers.remove(playerId); // Permettre une re-notification si le joueur revient

        // Toujours notifier le listener pour supprimer le sprite, même si le joueur
        // n'était pas dans remotePlayers (le sprite peut avoir été créé autrement)
        if (listener != null) {
            System.out.println("[GameNetworkAdapter] Notification de départ pour: " + playerId);
            listener.onPlayerLeave(playerId);
        }
    }

    /**
     * Envoyer la position d'un monstre à tous les pairs
     */
    public void sendMonsterPosition(String monsterId, double x, double y) {
        String message = "MONSTER_MOVE:" + monsterId + ":" + x + "," + y;
        serveur.broadcastToPeers(message);
    }

    /**
     * Gérer la réception de la position d'un monstre
     */
    public void onMonsterPositionReceived(String monsterId, String positionData) {
        try {
            String[] parts = positionData.split(",");
            if (parts.length >= 2) {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                if (listener != null) {
                    listener.onMonsterMove(monsterId, x, y);
                }
            }
        } catch (NumberFormatException e) {
            // Ignorer l'erreur
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
     * Obtenir les positions de tous les joueurs distants
     * @return Map des positions (playerId -> [x, y, angle])
     */
    public Map<String, double[]> getRemotePlayersPositions() {
        Map<String, double[]> positions = new ConcurrentHashMap<>();
        for (Map.Entry<String, Joueur> entry : remotePlayers.entrySet()) {
            Joueur player = entry.getValue();
            positions.put(entry.getKey(), new double[]{player.getX(), player.getY(), player.getAngle()});
        }
        return positions;
    }

    /**
     * Arrêter le serveur réseau
     */
    public void shutdown() {
        serveur.shutdown();
    }
}
