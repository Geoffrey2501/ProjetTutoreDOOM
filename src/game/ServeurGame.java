package game;

import Reseau.GestionConnection;
import Reseau.Serveur;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe représentant le serveur de jeu multijoueur.
 * Étend la classe Serveur pour gérer les communications réseau spécifiques au jeu.
 * Gère les messages de position des joueurs et leur relais aux autres pairs.
 */
public class ServeurGame extends Serveur {
    /** Adaptateur réseau pour communiquer avec le jeu */
    private final GameNetworkAdapter adapter;

    /** Identifiant unique du nœud */
    private final String nodeId;

    /** Map stockant le dernier temps de relais pour chaque joueur */
    private final Map<String, Long> lastRelayTime = new ConcurrentHashMap<>();

    /** Intervalle minimum en millisecondes entre deux relais de position */
    private static final long RELAY_MIN_INTERVAL_MS = 30;

    /**
     * Constructeur du serveur de jeu.
     * @param nodeId  Identifiant unique du nœud (ex: "J1", "J2")
     * @param host    Adresse IP du serveur (ex: "localhost", "192.168.1.10")
     * @param port    Port d'écoute du serveur (ex: 5001)
     * @param adapter Adaptateur réseau pour communiquer avec le jeu
     */
    public ServeurGame(String nodeId, String host, int port, GameNetworkAdapter adapter) {
        super(nodeId, host, port);
        this.nodeId = nodeId;
        this.adapter = adapter;
    }

    /**
     * Callback appelé lorsqu'un pair se déconnecte.
     * @param peerId Identifiant du pair déconnecté
     */
    @Override
    protected void onPeerDisconnected(String peerId) {
        adapter.onPlayerDisconnected(peerId);
    }

    /**
     * Obtenir l'identifiant unique du nœud.
     *
     * @return L'identifiant du nœud
     */
    @Override
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Traite un message reçu d'un pair.
     * Si le message est un message de déplacement (MOVE:), il est traité spécifiquement.
     * Sinon, il est délégué à la classe parente.
     *
     * @param message Message reçu
     * @param sender  Connexion du pair émetteur
     */
    @Override
    public void processMessageFromPeer(String message, GestionConnection sender) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        if (message.startsWith("MOVE:")) {
            processGameMoveMessage(message, sender);
        } else {
            super.processMessageFromPeer(message, sender);
        }
    }

    /**
     * Traite un message de déplacement de joueur.
     * Extrait les informations de position, met à jour l'état du jeu
     * et relaye le message aux autres pairs connectés.
     *
     * @param message Message de déplacement au format "MOVE:playerId:positionData"
     * @param sender  Connexion du pair émetteur
     */
    private void processGameMoveMessage(String message, GestionConnection sender) {
        try {
            String content = message.substring(5);
            int colonIndex = content.indexOf(':');
            if (colonIndex == -1) {
                return;
            }

            String playerId = content.substring(0, colonIndex);
            String positionData = content.substring(colonIndex + 1);

            if (sender.getRemotePeerId() == null) {
                sender.setRemotePeerId(playerId);
                adapter.sendPlayerPositionTo(sender);
            }

            long now = System.currentTimeMillis();
            Long lastTime = lastRelayTime.get(playerId);
            if (lastTime != null && (now - lastTime) < RELAY_MIN_INTERVAL_MS) {
                adapter.onPositionReceived(playerId, positionData);
                return;
            }
            lastRelayTime.put(playerId, now);

            adapter.onPositionReceived(playerId, positionData);

            for (GestionConnection peer : getConnectedPeers()) {
                if (peer != sender) {
                    peer.sendMessage(message);
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs de parsing
        }
    }

    /**
     * Obtenir la liste des pairs connectés.
     *
     * @return Liste des connexions aux pairs
     */
    protected java.util.List<GestionConnection> getConnectedPeers() {
        return getConnectedPeersList();
    }
}

