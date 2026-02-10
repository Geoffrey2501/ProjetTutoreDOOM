package Reseau;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Nœud Peer-to-Peer (P2P) pour le système multijoueur Doom-like avec maillage complet
 * Chaque nœud agit à la fois comme client et serveur. Il peut :
 * - Accepter les connexions entrantes d'autres joueurs
 * - Se connecter à d'autres joueurs
 * - Partager la liste des pairs connus avec les nouveaux connectés (maillage complet)
 * - Envoyer ses coordonnées (X, Y) à tous les pairs
 * - Recevoir et relayer les coordonnées des autres joueurs
 * Protocole des messages :
 * - "MOVE:NomJoueur:X,Y" - Coordonnées d'un joueur
 * - "PEER_LIST:J1@host1:port1;J2@host2:port2" - Liste des pairs connus
 * - "NEW_PEER:JoueurX@host:port" - Annonce d'un nouveau pair au réseau
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class Serveur {

    private final String nodeId;
    private final int port;
    private final String host;
    private ServerSocket serverSocket;
    protected List<GestionConnection> connectedPeers = new CopyOnWriteArrayList<>();
    private final Map<String, PeerInfo> knownPeers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, int[]> playerPositions = new ConcurrentHashMap<>();
    private int posX = 0;
    private int posY = 0;

    /**
     * Constructeur du nœud P2P
     *
     * @param nodeId Identifiant unique du nœud (ex: "J1", "J2")
     * @param host   Adresse IP du nœud (ex: "localhost", "192.168.1.10")
     * @param port   Port d'écoute du serveur (ex: 5001)
     */
    public Serveur(String nodeId, String host, int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }

    /**
     * Obtenir l'identifiant du nœud
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Obtenir la liste des pairs connectés (pour les sous-classes)
     */
    protected List<GestionConnection> getConnectedPeersList() {
        return connectedPeers;
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
            // Optimisation : privilégier la latence (1) et la bande passante (0)
            serverSocket.setPerformancePreferences(0, 1, 0);

            //obtenir l'IP réelle de la machine
            String realHost = getLocalIPAddress();

            //s'ajouter à la liste des pairs connus avec l'IP réelle
            knownPeers.put(nodeId, new PeerInfo(nodeId, realHost, port));

            executor.execute(() -> acceptIncomingConnections());

            playerPositions.put(nodeId, new int[]{posX, posY});
        } catch (IOException e) {
            System.err.println("Erreur démarrage: " + e.getMessage());
        }
    }

    /**
     * Obtenir l'adresse IP locale de la machine (pas localhost)
     */
    private String getLocalIPAddress() {
        try {
            //essayer de trouver une IP non-loopback
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
        }
        return host;
    }

    /**
     * Accepter les connexions entrantes des autres pairs
     *
     * Écoute continuellement le port et crée une nouvelle connexion
     * pour chaque pair qui se connecte. Envoie immédiatement la liste
     * des pairs connus au nouveau connecté pour établir le maillage complet.
     */
    private void acceptIncomingConnections() {
        try {
            while (!serverSocket.isClosed()) {
                Socket peerSocket = serverSocket.accept();
                GestionConnection peerConnection = new GestionConnection(peerSocket, this);
                connectedPeers.add(peerConnection);
                executor.execute(peerConnection);

                //IMPORTANT : Envoyer la liste des pairs connus au nouveau connecté
                sendPeerListTo(peerConnection);
            }
        } catch (IOException e) {
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
        // Ne pas se connecter à soi-même
        if (remoteNodeId.equals(nodeId)) {
            return;
        }

        // Vérifier si on est déjà connecté à ce pair
        for (GestionConnection peer : connectedPeers) {
            if (remoteNodeId.equals(peer.getRemotePeerId())) {
                return;
            }
        }

        // Vérifier si ce pair est déjà connu (connexion en cours ou établie)
        if (knownPeers.containsKey(remoteNodeId)) {
            return;
        }

        // Marquer ce pair comme "en cours de connexion" pour éviter les doublons
        knownPeers.put(remoteNodeId, new PeerInfo(remoteNodeId, host, remotePort));

        executor.execute(() -> {
            try {
                Socket socket = new Socket(host, remotePort);
                GestionConnection peerConnection = new GestionConnection(socket, this);
                peerConnection.setRemotePeerId(remoteNodeId);
                connectedPeers.add(peerConnection);

                executor.execute(peerConnection);

                // Envoyer un message HELLO avec notre ID et port d'écoute
                peerConnection.sendMessage("HELLO:" + nodeId + "@" + getLocalIPAddress() + ":" + port);
            } catch (IOException e) {
                // En cas d'échec, retirer de knownPeers pour permettre une nouvelle tentative
                knownPeers.remove(remoteNodeId);
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

        broadcastToPeers("MOVE:" + nodeId + ":" + x + "," + y);
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
     * Gère différents types de messages :
     * - HELLO:peerId@host:port - Identification d'un nouveau pair
     * - MOVE:NomJoueur:X,Y - Mise à jour de position
     * - PEER_LIST:peer1@host1:port1;peer2@host2:port2 - Liste des pairs
     * - NEW_PEER:peerX@host:port - Annonce d'un nouveau pair
     *
     * @param message Message reçu
     * @param sender  Connexion qui a envoyé le message
     */
    public void processMessageFromPeer(String message, GestionConnection sender) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        try {
            // Déterminer le type de message
            if (message.startsWith("HELLO:")) {
                processHelloMessage(message, sender);
            } else if (message.startsWith("MOVE:")) {
                processMoveMessage(message, sender);
            } else if (message.startsWith("PEER_LIST:")) {
                processPeerListMessage(message);
            } else if (message.startsWith("NEW_PEER:")) {
                processNewPeerMessage(message);
            } else {
                processLegacyMoveMessage(message, sender);
            }
        } catch (Exception e) {
        }
    }

    /**
     * Traiter un message de type HELLO:peerId@host:port
     * Identifie un nouveau pair et propage ses infos aux autres pairs.
     */
    // Dans src/Reseau/Serveur.java

    private void processHelloMessage(String message, GestionConnection sender) {
        try {
            String content = message.substring(6);
            PeerInfo peerInfo = PeerInfo.fromString(content.trim());
            if (peerInfo == null) return;
            
            sender.setRemotePeerId(peerInfo.getPeerId());

            // 1. On l'ajoute officiellement à nos connaissances
            knownPeers.put(peerInfo.getPeerId(), peerInfo);

            // 2. On lui envoie TOUTE notre liste actuelle (Maillage complet)
            sendPeerListTo(sender);

            // 3. On annonce son arrivée à tout le réseau existant
            broadcastNewPeer(peerInfo);

            System.out.println("[" + nodeId + "] " + peerInfo.getPeerId() + " est maintenant intégré au maillage.");
        } catch (Exception e) {
            // Log error
        }
    }

    /**
     * Traiter un message de type MOVE:playerId:x,y
     * En P2P maillé complet, pas de relais nécessaire.
     */
    private void processMoveMessage(String message, GestionConnection sender) {
        try {
            // Format: "MOVE:J1:100,200"
            String content = message.substring(5); // Enlever "MOVE:"
            String[] parts = content.split(":");
            if (parts.length != 2) return;

            String playerId = parts[0];
            String[] coords = parts[1].split(",");
            if (coords.length < 2) return;

            int x = Integer.parseInt(coords[0].trim().split("\\.")[0]); //gérer les doubles
            int y = Integer.parseInt(coords[1].trim().split("\\.")[0]);

            playerPositions.put(playerId, new int[]{x, y});
        } catch (NumberFormatException e) {
            // Ignorer
        }
    }

    /**
     * Traiter un message de type PEER_LIST:peer1@host1:port1;peer2@host2:port2
     */
    private void processPeerListMessage(String message) {
        try {
            System.out.println("[" + nodeId + "] Reçu PEER_LIST: " + message);
            // Format: "PEER_LIST:J1@localhost:5001;J2@localhost:5002"
            String content = message.substring(10);

            // Enlever "PEER_LIST:"
            if (content.isEmpty()) return;

            String[] peerStrings = content.split(";");
            for (String peerString : peerStrings) {
                PeerInfo peerInfo = PeerInfo.fromString(peerString.trim());
                if (peerInfo == null) continue;

                // Ne pas se connecter à soi-même
                if (peerInfo.getPeerId().equals(nodeId)) continue;

                // Ne pas se connecter si déjà connu
                if (knownPeers.containsKey(peerInfo.getPeerId())) continue;

                // Se connecter au nouveau pair (connectToNode gère l'ajout à knownPeers)
                connectToNode(peerInfo.getPeerId(), peerInfo.getHost(), peerInfo.getPort());
            }
        } catch (Exception e) {
        }
    }

    /**
     * Traiter un message de type NEW_PEER:peerX@host:port
     */

    private void processNewPeerMessage(String message) {
        try {
            String content = message.substring(9);
            PeerInfo peerInfo = PeerInfo.fromString(content.trim());
            if (peerInfo == null) return;

            if (peerInfo.getPeerId().equals(nodeId)) return;

            // Si on ne connaissait pas ce pair, on se connecte ET on le présente à nos autres amis
            if (!knownPeers.containsKey(peerInfo.getPeerId())) {
                System.out.println("[" + nodeId + "] Découverte d'un nouveau pair via relais : " + peerInfo.getPeerId());
                connectToNode(peerInfo.getPeerId(), peerInfo.getHost(), peerInfo.getPort());

                // RELAIS : On informe nos autres pairs connectés de l'existence de ce nouveau
                // Cela garantit que l'information circule partout dans le maillage
                broadcastToPeers(message);
            }
        } catch (Exception e) {
            // Log error
        }
    }

    /**
     * Traiter un message au format ancien (compatibilité) : "playerId:x,y"
     * En P2P maillé complet, pas de relais nécessaire.
     */
    private void processLegacyMoveMessage(String message, GestionConnection sender) {
        try {
            String[] parts = message.split(":");
            if (parts.length != 2) return;

            String playerId = parts[0];
            String[] coords = parts[1].split(",");
            if (coords.length != 2) return;

            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());

            playerPositions.put(playerId, new int[]{x, y});

            // Pas de relais en P2P maillé complet
        } catch (NumberFormatException e) {
            // Ignorer
        }
    }

    /**
     * Envoyer la liste des pairs connus à une connexion spécifique
     * Exclut l'hôte local de la liste (le destinataire est déjà connecté à nous)
     *
     * @param connection Connexion à laquelle envoyer la liste
     */
    private void sendPeerListTo(GestionConnection connection) {
        if (knownPeers.isEmpty()) return;

        StringBuilder sb = new StringBuilder("PEER_LIST:");
        boolean first = true;
        for (PeerInfo peerInfo : knownPeers.values()) {
            // Ne pas inclure soi-même dans la liste (le destinataire est déjà connecté à nous)
            if (peerInfo.getPeerId().equals(nodeId)) continue;

            if (!first) sb.append(";");
            sb.append(peerInfo.toString());
            first = false;
        }

        // N'envoyer que si la liste contient au moins un pair
        if (!first) {
            connection.sendMessage(sb.toString());
        }
    }


    /**
     * Annoncer un nouveau pair à tous les pairs connectés
     *
     * @param peerInfo Informations du nouveau pair
     */
    private void broadcastNewPeer(PeerInfo peerInfo) {
        String message = "NEW_PEER:" + peerInfo.toString();
        System.out.println("[" + nodeId + "] PEER_LIST: " + message);
        broadcastToPeers(message);
    }

    /**
     * Retirer un pair déconnecté de la liste des connexions
     *
     * @param peer La connexion à retirer
     */
    public void removePeer(GestionConnection peer) {
        connectedPeers.remove(peer);
        String peerId = peer.getRemotePeerId();
        if (peerId != null) {
            onPeerDisconnected(peerId);
        }
    }

    /**
     * Callback appelé quand un pair se déconnecte
     * Peut être surchargé par les sous-classes
     */
    protected void onPeerDisconnected(String peerId) {
        // Par défaut, ne fait rien - à surcharger dans les sous-classes
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
     * - La liste des pairs connus
     * - Les positions de tous les joueurs connus
     */
    public void printStatus() {
        System.out.println("\n===== État du nœud " + nodeId + " =====");
        System.out.println("Position: (" + posX + ", " + posY + ")");
        System.out.println("Pairs connectés: " + connectedPeers.size());
        System.out.println("Pairs connus: " + knownPeers.size());
        for (PeerInfo peerInfo : knownPeers.values()) {
            System.out.println("  - " + peerInfo);
        }
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
        } catch (IOException e) {
            // Ignorer
        }
    }
}

