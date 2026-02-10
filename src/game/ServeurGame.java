package game;

import Reseau.GestionConnection;
import Reseau.Serveur;

/**
 * Classe représentant le serveur de jeu multijoueur.
 * Étend la classe Serveur pour gérer les communications réseau spécifiques au jeu.
 * Gère les messages de position des joueurs.
 */
public class ServeurGame extends Serveur {
    /** Adaptateur réseau pour communiquer avec le jeu */
    private final GameNetworkAdapter adapter;

    /** Identifiant unique du nœud */
    private final String nodeId;

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
     * Extrait les informations de position et met à jour l'état du jeu.
     * MODIFIÉ : Ajoute un relais pour assurer que tous les joueurs reçoivent l'info
     * même si la connexion directe P2P échoue.
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

            // Identifier le pair si pas encore fait
            if (sender.getRemotePeerId() == null) {
                sender.setRemotePeerId(playerId);
                adapter.sendPlayerPositionTo(sender);
            }

            // Mettre à jour la position localement
            adapter.onPositionReceived(playerId, positionData);

            // --- FIX P2P VISIBILITY ---
            // Relayer le message aux autres pairs connectés.
            // Cela permet de contourner les problèmes de NAT/Pare-feu qui bloquent
            // les connexions directes entre clients. Le serveur agit comme un pont.
            for (GestionConnection peer : connectedPeers) {
                // Ne pas renvoyer à l'expéditeur
                if (peer != sender) {
                    peer.sendMessage(message);
                }
            }
            // --------------------------

        } catch (Exception e) {
            // Ignorer les erreurs de parsing
        }
    }
}