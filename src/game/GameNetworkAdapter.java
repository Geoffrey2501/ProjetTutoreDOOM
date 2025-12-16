package game;

import Reseau.GestionConnection;
import Reseau.Serveur;
import prototype_raycasting.Joueur;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptateur réseau pour le jeu Doom-like multijoueur
 * Fait le pont entre le système P2P (Reseau.Serveur) et le prototype raycasting
 *
 * Gère:
 * - La synchronisation des positions des joueurs (double x, y, angle)
 * - La conversion entre le format réseau et les objets Joueur
 * - Les callbacks pour les événements réseau
 */
public class GameNetworkAdapter {

    private Serveur serveur;
    private Joueur localPlayer;
    private Map<String, Joueur> remotePlayers;
    private NetworkListener listener;

    // Intervalle d'envoi de position (en ms)
    private static final long SEND_INTERVAL = 50; // 20 fois par seconde
    private long lastSendTime = 0;

    /**
     * Crée un adaptateur réseau pour le jeu
     * @param nodeId Identifiant unique du joueur (ex: "J1", "Player1")
     * @param host Adresse IP locale
     * @param port Port d'écoute
     */
    public GameNetworkAdapter(String nodeId, String host, int port) {
        this.serveur = new ServeurGame(nodeId, host, port, this);
        this.remotePlayers = new ConcurrentHashMap<>();
    }

    /**
     * Définit le joueur local
     */
    public void setLocalPlayer(Joueur player) {
        this.localPlayer = player;
        this.localPlayer.setId(serveur.getNodeId());
    }

    /**
     * Définit le listener pour les événements réseau
     */
    public void setNetworkListener(NetworkListener listener) {
        this.listener = listener;
    }

    /**
     * Démarre le serveur réseau
     */
    public void start() {
        serveur.start();
    }

    /**
     * Se connecte à un autre joueur
     */
    public void connectToPlayer(String playerId, String host, int port) {
        serveur.connectToNode(playerId, host, port);
    }

    /**
     * Envoie la position du joueur local à tous les pairs
     * Utilise un throttle pour éviter de surcharger le réseau
     */
    public void sendPlayerPosition() {
        if (localPlayer == null) return;

        long now = System.currentTimeMillis();
        if (now - lastSendTime < SEND_INTERVAL) {
            return; // Throttle
        }
        lastSendTime = now;

        // Format: MOVE:playerId:x,y,angle
        String message = "MOVE:" + localPlayer.getId() + ":" + localPlayer.toNetworkString();
        serveur.broadcastToPeers(message);
    }

    /**
     * Force l'envoi immédiat de la position (ignore le throttle)
     */
    public void sendPlayerPositionNow() {
        if (localPlayer == null) return;
        lastSendTime = System.currentTimeMillis();
        String message = "MOVE:" + localPlayer.getId() + ":" + localPlayer.toNetworkString();
        serveur.broadcastToPeers(message);
    }

    /**
     * Traite une mise à jour de position reçue du réseau
     * Appelé par ServeurGame
     */
    void onPositionReceived(String playerId, String positionData) {
        if (playerId.equals(localPlayer.getId())) {
            return; // Ignorer ses propres messages
        }

        Joueur remotePlayer = remotePlayers.get(playerId);
        if (remotePlayer == null) {
            // Nouveau joueur
            remotePlayer = Joueur.fromNetwork(playerId, positionData);
            remotePlayers.put(playerId, remotePlayer);

            if (listener != null) {
                listener.onPlayerJoin(playerId);
            }
        } else {
            remotePlayer.fromNetworkString(positionData);
        }

        // Notifier le listener
        if (listener != null) {
            listener.onPlayerPositionUpdate(
                playerId,
                remotePlayer.getX(),
                remotePlayer.getY(),
                remotePlayer.getAngle()
            );
        }
    }

    /**
     * Appelé quand un joueur se déconnecte
     */
    void onPlayerDisconnected(String playerId) {
        remotePlayers.remove(playerId);
        if (listener != null) {
            listener.onPlayerLeave(playerId);
        }
    }

    /**
     * Obtient tous les joueurs distants
     */
    public Map<String, Joueur> getRemotePlayers() {
        return remotePlayers;
    }

    /**
     * Obtient un joueur distant par son ID
     */
    public Joueur getRemotePlayer(String playerId) {
        return remotePlayers.get(playerId);
    }

    /**
     * Arrête le réseau
     */
    public void shutdown() {
        serveur.shutdown();
    }

    /**
     * Affiche l'état du réseau
     */
    public void printStatus() {
        serveur.printStatus();
        System.out.println("Joueurs distants: " + remotePlayers.size());
        for (Joueur j : remotePlayers.values()) {
            System.out.println("  " + j);
        }
    }

    public String getNodeId() {
        return serveur.getNodeId();
    }

    // =========================================================================
    // Classe interne: ServeurGame - Extension de Serveur pour le jeu
    // =========================================================================

    /**
     * Extension du Serveur P2P pour supporter les positions double et l'angle
     */
    private static class ServeurGame extends Serveur {
        private GameNetworkAdapter adapter;
        private String nodeId;

        public ServeurGame(String nodeId, String host, int port, GameNetworkAdapter adapter) {
            super(nodeId, host, port);
            this.nodeId = nodeId;
            this.adapter = adapter;
        }

        public String getNodeId() {
            return nodeId;
        }

        @Override
        public void processMessageFromPeer(String message, GestionConnection sender) {
            if (message == null || message.trim().isEmpty()) {
                return;
            }

            // Traiter les messages MOVE avec le nouveau format (x,y,angle en double)
            if (message.startsWith("MOVE:")) {
                processGameMoveMessage(message, sender);
            } else {
                // Déléguer les autres messages au parent
                super.processMessageFromPeer(message, sender);
            }
        }

        private void processGameMoveMessage(String message, GestionConnection sender) {
            try {
                // Format: "MOVE:playerId:x,y,angle"
                String content = message.substring(5); // Enlever "MOVE:"
                int colonIndex = content.indexOf(':');
                if (colonIndex == -1) return;

                String playerId = content.substring(0, colonIndex);
                String positionData = content.substring(colonIndex + 1);

                // Notifier l'adaptateur
                adapter.onPositionReceived(playerId, positionData);

                // Relayer aux autres pairs
                for (GestionConnection peer : getConnectedPeers()) {
                    if (peer != sender) {
                        peer.sendMessage(message);
                    }
                }

                System.out.println("[" + nodeId + "] " + playerId + ": " + positionData);
            } catch (Exception e) {
                System.err.println("[" + nodeId + "] Erreur parsing MOVE: " + message);
            }
        }

        // Méthode pour accéder aux pairs connectés (nécessaire pour le relais)
        protected java.util.List<GestionConnection> getConnectedPeers() {
            return getConnectedPeersList();
        }
    }
}

