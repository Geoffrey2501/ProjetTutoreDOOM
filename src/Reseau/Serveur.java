package Reseau;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Nœud Peer-to-Peer (P2P) pour le système multijoueur Doom-like avec maillage complet
 * Chaque nœud agit à la fois comme client et serveur.
 * * MODIFICATION : Activation du relais de messages pour pallier les échecs de connexion P2P directe.
 *
 * @author Groupe DOOM
 * @version 1.1 (Avec Relais)
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
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setPerformancePreferences(0, 1, 0);

            String realHost = getLocalIPAddress();
            knownPeers.put(nodeId, new PeerInfo(nodeId, realHost, port));

            executor.execute(() -> acceptIncomingConnections());

            playerPositions.put(nodeId, new int[]{posX, posY});
        } catch (IOException e) {
            System.err.println("Erreur démarrage: " + e.getMessage());
        }
    }

    private String getLocalIPAddress() {
        try {
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

    private void acceptIncomingConnections() {
        try {
            while (!serverSocket.isClosed()) {
                Socket peerSocket = serverSocket.accept();
                System.out.println("[" + nodeId + "] Connexion entrante acceptée de " + peerSocket.getInetAddress());

                GestionConnection peerConnection = new GestionConnection(peerSocket, this);
                connectedPeers.add(peerConnection);
                executor.execute(peerConnection);

                String helloMsg = "HELLO:" + nodeId + "@" + getLocalIPAddress() + ":" + port;
                peerConnection.sendMessage(helloMsg);
                sendPeerListTo(peerConnection);
            }
        } catch (IOException e) {
        }
    }

    public void connectToNode(String remoteNodeId, String host, int remotePort) {
        System.out.println("[" + nodeId + "] Tentative de connexion à " + remoteNodeId + " @ " + host + ":" + remotePort);

        if (remoteNodeId.equals(nodeId)) return;

        for (GestionConnection peer : connectedPeers) {
            if (remoteNodeId.equals(peer.getRemotePeerId())) return;
        }

        if (knownPeers.containsKey(remoteNodeId)) return;

        knownPeers.put(remoteNodeId, new PeerInfo(remoteNodeId, host, remotePort));

        executor.execute(() -> {
            try {
                System.out.println("[" + nodeId + "] Connexion TCP à " + remoteNodeId + "...");
                Socket socket = new Socket(host, remotePort);
                GestionConnection peerConnection = new GestionConnection(socket, this);
                peerConnection.setRemotePeerId(remoteNodeId);
                connectedPeers.add(peerConnection);

                executor.execute(peerConnection);

                String helloMsg = "HELLO:" + nodeId + "@" + getLocalIPAddress() + ":" + port;
                peerConnection.sendMessage(helloMsg);
            } catch (IOException e) {
                System.err.println("[" + nodeId + "] ❌ Échec connexion à " + remoteNodeId + ": " + e.getMessage());
                knownPeers.remove(remoteNodeId);
            }
        });
    }

    public void movePlayer(int x, int y) {
        posX = x;
        posY = y;
        playerPositions.put(nodeId, new int[]{posX, posY});
        broadcastToPeers("MOVE:" + nodeId + ":" + x + "," + y);
    }

    public void broadcastToPeers(String message) {
        for (GestionConnection peer : connectedPeers) {
            peer.sendMessage(message);
        }
    }

    public void processMessageFromPeer(String message, GestionConnection sender) {
        if (message == null || message.trim().isEmpty()) return;

        try {
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

    private void processHelloMessage(String message, GestionConnection sender) {
        try {
            String content = message.substring(6);
            PeerInfo peerInfo = PeerInfo.fromString(content.trim());
            if (peerInfo == null) return;

            // Gestion des doublons
            for (GestionConnection existingPeer : connectedPeers) {
                if (existingPeer != sender &&
                        peerInfo.getPeerId().equals(existingPeer.getRemotePeerId())) {
                    if (nodeId.compareTo(peerInfo.getPeerId()) < 0) {
                        sender.disconnect();
                        return;
                    } else {
                        existingPeer.disconnect();
                        break;
                    }
                }
            }

            sender.setRemotePeerId(peerInfo.getPeerId());
            System.out.println("[" + nodeId + "] ✓ " + peerInfo.getPeerId() + " identifié");
            knownPeers.put(peerInfo.getPeerId(), peerInfo);
            sendPeerListTo(sender);
            broadcastNewPeer(peerInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Traiter un message de type MOVE:playerId:x,y
     * MODIFIÉ : Ajout du relais vers les autres pairs
     */
    private void processMoveMessage(String message, GestionConnection sender) {
        try {
            String content = message.substring(5);
            String[] parts = content.split(":");
            if (parts.length != 2) return;

            String playerId = parts[0];
            String[] coords = parts[1].split(",");
            if (coords.length < 2) return;

            int x = Integer.parseInt(coords[0].trim().split("\\.")[0]);
            int y = Integer.parseInt(coords[1].trim().split("\\.")[0]);

            playerPositions.put(playerId, new int[]{x, y});

            // --- AJOUT RELAIS ---
            // Si la connexion directe a échoué, ce relais permet aux autres pairs
            // de recevoir la position via ce nœud (serveur/pivot).
            for (GestionConnection peer : connectedPeers) {
                // On transmet à tous sauf à l'expéditeur d'origine
                if (peer != sender) {
                    peer.sendMessage(message);
                }
            }
            // --------------------

        } catch (NumberFormatException e) {
            // Ignorer
        }
    }

    private void processPeerListMessage(String message) {
        try {
            String content = message.substring(10);
            if (content.isEmpty()) return;
            String[] peerStrings = content.split(";");
            for (String peerString : peerStrings) {
                PeerInfo peerInfo = PeerInfo.fromString(peerString.trim());
                if (peerInfo == null || peerInfo.getPeerId().equals(nodeId) || knownPeers.containsKey(peerInfo.getPeerId())) continue;
                connectToNode(peerInfo.getPeerId(), peerInfo.getHost(), peerInfo.getPort());
            }
        } catch (Exception e) {}
    }

    private void processNewPeerMessage(String message) {
        try {
            String content = message.substring(9);
            PeerInfo peerInfo = PeerInfo.fromString(content.trim());
            if (peerInfo == null || peerInfo.getPeerId().equals(nodeId) || knownPeers.containsKey(peerInfo.getPeerId())) return;
            connectToNode(peerInfo.getPeerId(), peerInfo.getHost(), peerInfo.getPort());
        } catch (Exception e) {}
    }

    private void processLegacyMoveMessage(String message, GestionConnection sender) {
        try {
            String[] parts = message.split(":");
            if (parts.length != 2) return;
            String playerId = parts[0];
            String[] coords = parts[1].split(",");
            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());
            playerPositions.put(playerId, new int[]{x, y});
        } catch (NumberFormatException e) {}
    }

    private void sendPeerListTo(GestionConnection connection) {
        if (knownPeers.isEmpty()) return;
        StringBuilder sb = new StringBuilder("PEER_LIST:");
        boolean first = true;
        for (PeerInfo peerInfo : knownPeers.values()) {
            if (peerInfo.getPeerId().equals(nodeId)) continue;
            if (!first) sb.append(";");
            sb.append(peerInfo.toString());
            first = false;
        }
        if (!first) connection.sendMessage(sb.toString());
    }

    private void broadcastNewPeer(PeerInfo peerInfo) {
        broadcastToPeers("NEW_PEER:" + peerInfo.toString());
    }

    public void removePeer(GestionConnection peer) {
        connectedPeers.remove(peer);
        String peerId = peer.getRemotePeerId();
        if (peerId != null) onPeerDisconnected(peerId);
    }

    protected void onPeerDisconnected(String peerId) {
    }

    public Map<String, int[]> getPlayerPositions() {
        return new HashMap<>(playerPositions);
    }

    public void printStatus() {
        System.out.println("\n===== État du nœud " + nodeId + " =====");
        System.out.println("Position: (" + posX + ", " + posY + ")");
        System.out.println("Pairs connectés: " + connectedPeers.size());
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

    public void shutdown() {
        try {
            for (GestionConnection peer : connectedPeers) peer.disconnect();
            serverSocket.close();
            executor.shutdown();
        } catch (IOException e) {}
    }
}