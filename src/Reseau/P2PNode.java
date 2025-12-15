package Reseau;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Nœud Peer-to-Peer (P2P) pour le système multijoueur Doom-like
 *
 * Chaque nœud agit à la fois comme client et serveur. Il peut :
 * - Accepter les connexions entrantes d'autres joueurs
 * - Se connecter à d'autres joueurs
 * - Envoyer ses coordonnées (X, Y) à tous les pairs
 * - Recevoir et relayer les coordonnées des autres joueurs
 *
 * Format des messages : "NomJoueur:X,Y"
 * Exemple : "J1:100,200"
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class P2PNode {
    private String nodeId;
    private int port;
    private ServerSocket serverSocket;
    private List<GestionConnection> connectedPeers = new CopyOnWriteArrayList<>();
    private ExecutorService executor = Executors.newCachedThreadPool();
    private Map<String, int[]> playerPositions = new ConcurrentHashMap<>();
    private int posX = 0;
    private int posY = 0;

    /**
     * Constructeur du nœud P2P
     *
     * @param nodeId Identifiant unique du nœud (ex: "J1", "J2")
     * @param port   Port d'écoute du serveur (ex: 5001)
     */
    public P2PNode(String nodeId, int port) {
        this.nodeId = nodeId;
        this.port = port;
    }

    /**
     * Démarrer le nœud P2P
     *
     * Crée un ServerSocket pour accepter les connexions entrantes
     * et lance un thread pour gérer les connexions entrantes.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[" + nodeId + "] Nœud P2P démarré sur le port " + port);

            executor.execute(() -> acceptIncomingConnections());

            playerPositions.put(nodeId, new int[]{posX, posY});
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du nœud P2P: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Accepter les connexions entrantes des autres pairs
     *
     * Écoute continuellement le port et crée une nouvelle connexion
     * pour chaque pair qui se connecte.
     */
    private void acceptIncomingConnections() {
        try {
            while (!serverSocket.isClosed()) {
                Socket peerSocket = serverSocket.accept();
                System.out.println("[" + nodeId + "] Nouveau pair connecté: " + peerSocket.getInetAddress());

                GestionConnection peerConnection = new GestionConnection(peerSocket, this);
                connectedPeers.add(peerConnection);
                executor.execute(peerConnection);
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                System.err.println("Erreur lors de l'acceptation des connexions: " + e.getMessage());
            }
        }
    }

    /**
     * Se connecter à un autre nœud P2P
     *
     * @param remoteNodeId Identifiant du nœud distant
     * @param host         Adresse IP du nœud distant (ex: "localhost")
     * @param remotePort   Port du nœud distant (ex: 5001)
     */
    public void connectToNode(String remoteNodeId, String host, int remotePort) {
        executor.execute(() -> {
            try {
                Socket socket = new Socket(host, remotePort);
                System.out.println("[" + nodeId + "] → " + remoteNodeId);

                GestionConnection peerConnection = new GestionConnection(socket, this);
                peerConnection.setRemotePeerId(remoteNodeId);
                connectedPeers.add(peerConnection);
                executor.execute(peerConnection);
            } catch (IOException e) {
                System.err.println("[" + nodeId + "] Erreur: " + e.getMessage());
            }
        });
    }

    /**
     * Déplacer le joueur et envoyer ses coordonnées à tous les pairs
     *
     * @param x Coordonnée X du joueur
     * @param y Coordonnée Y du joueur
     */
    public void movePlayer(int x, int y) {
        posX = x;
        posY = y;
        playerPositions.put(nodeId, new int[]{posX, posY});

        broadcastToPeers(nodeId + ":" + x + "," + y);
        System.out.println("[" + nodeId + "] Position: (" + x + ", " + y + ")");
    }

    /**
     * Diffuser un message à tous les pairs connectés
     *
     * @param message Message au format "NomJoueur:X,Y"
     */
    public void broadcastToPeers(String message) {
        for (GestionConnection peer : connectedPeers) {
            peer.sendMessage(message);
        }
    }

    /**
     * Traiter un message reçu d'un pair
     *
     * Extrait les coordonnées du message et met à jour la position du joueur.
     * Le message est ensuite relayé aux autres pairs.
     *
     * Format attendu : "NomJoueur:X,Y"
     * Exemple : "J2:150,250"
     *
     * @param message Message reçu
     * @param sender  Connexion qui a envoyé le message
     */
    public void processMessageFromPeer(String message, GestionConnection sender) {
        // Ignorer les messages null ou vides
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        try {
            String[] parts = message.split(":");

            // Vérifier que le message a le bon format "playerId:coords"
            if (parts.length != 2) {
                return; // Message mal formé, ignorer silencieusement
            }

            String playerId = parts[0];
            String coordsStr = parts[1];

            // Vérifier que le playerId n'est pas vide
            if (playerId == null || playerId.trim().isEmpty()) {
                return; // Pas de nom de joueur, ignorer
            }

            // Vérifier que les coordonnées sont présentes
            if (coordsStr == null || coordsStr.trim().isEmpty()) {
                return; // Pas de coordonnées, ignorer
            }

            String[] coords = coordsStr.split(",");

            // Vérifier qu'on a exactement 2 coordonnées
            if (coords.length != 2) {
                return; // Format de coordonnées invalide
            }

            // Vérifier que les coordonnées ne sont pas vides
            if (coords[0].trim().isEmpty() || coords[1].trim().isEmpty()) {
                return; // Coordonnées vides
            }

            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());
            playerPositions.put(playerId, new int[]{x, y});
            System.out.println("[" + nodeId + "] " + playerId + ": (" + x + ", " + y + ")");

            for (GestionConnection peer : connectedPeers) {
                if (peer != sender) {
                    peer.sendMessage(message);
                }
            }
        } catch (NumberFormatException e) {
            // Coordonnées non numériques, ignorer silencieusement
        } catch (Exception e) {
            System.err.println("[" + nodeId + "] Erreur parsing message: " + message);
        }
    }

    /**
     * Retirer un pair déconnecté de la liste des connexions
     *
     * @param peer La connexion à retirer
     */
    public void removePeer(GestionConnection peer) {
        connectedPeers.remove(peer);
        System.out.println("[" + nodeId + "] Pair déconnecté. Pairs restants: " + connectedPeers.size());
    }

    /**
     * Obtenir la liste des positions de tous les joueurs
     *
     * @return Map avec les noms des joueurs et leurs coordonnées [X, Y]
     */
    public Map<String, int[]> getPlayerPositions() {
        return new HashMap<>(playerPositions);
    }

    /**
     * Afficher l'état complet du nœud
     *
     * Affiche :
     * - La position du nœud local
     * - Le nombre de pairs connectés
     * - Les positions de tous les joueurs connus
     */
    public void printStatus() {
        System.out.println("\n===== État du nœud " + nodeId + " =====");
        System.out.println("Position: (" + posX + ", " + posY + ")");
        System.out.println("Pairs connectés: " + connectedPeers.size());
        System.out.println("Positions des joueurs:");
        for (Map.Entry<String, int[]> entry : playerPositions.entrySet()) {
            int[] pos = entry.getValue();
            System.out.println("  " + entry.getKey() + ": (" + pos[0] + ", " + pos[1] + ")");
        }
        System.out.println("=============================\n");
    }

    /**
     * Arrêter le nœud P2P
     *
     * Ferme toutes les connexions aux pairs et arrête le serveur.
     */
    public void shutdown() {
        try {
            for (GestionConnection peer : connectedPeers) {
                peer.disconnect();
            }
            serverSocket.close();
            executor.shutdown();
            System.out.println("[" + nodeId + "] Nœud arrêté");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

