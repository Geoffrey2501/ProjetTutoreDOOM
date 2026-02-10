package Reseau;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Nœud Peer-to-Peer (P2P) pour le système multijoueur Doom-like
 * @author Groupe DOOM
 * @version 1.2 (Correction IP + Relais)
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

    public Serveur(String nodeId, String host, int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }

    public String getNodeId() {
        return nodeId;
    }

    protected List<GestionConnection> getConnectedPeersList() {
        return connectedPeers;
    }

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
                // On enverra la PEER_LIST après avoir reçu le HELLO du client pour être sûr de l'avoir identifié
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
            } else if (message.startsWith("PEER_LEFT:")) {
                processPeerLeftMessage(message);
            } else {
                processLegacyMoveMessage(message, sender);
            }
        } catch (Exception e) {
        }
    }

    private void processHelloMessage(String message, GestionConnection sender) {
        try {
            // Format reçu: "HELLO:Nom@IP_Declaree:Port"
            String content = message.substring(6);
            PeerInfo declaredInfo = PeerInfo.fromString(content.trim());
            if (declaredInfo == null) return;

            // 1. Récupération de l'IP réelle via la socket (celle qui marche vraiment)
            String realIP = sender.getRemoteIPAddress();

            // 2. Création d'une info corrigée : On garde l'ID et le Port déclarés, mais on force l'IP réelle
            // (Sauf si c'est du localhost pour des tests locaux)
            PeerInfo correctInfo;
            if (realIP != null && !realIP.equals("127.0.0.1") && !realIP.equals("0:0:0:0:0:0:0:1") && !realIP.equals(declaredInfo.getHost())) {
                System.out.println("[" + nodeId + "] ⚠️ Correction IP pour " + declaredInfo.getPeerId() +
                        ": " + declaredInfo.getHost() + " -> " + realIP);
                correctInfo = new PeerInfo(declaredInfo.getPeerId(), realIP, declaredInfo.getPort());
            } else {
                correctInfo = declaredInfo;
            }

            // Gestion des doublons (inchangée)
            for (GestionConnection existingPeer : connectedPeers) {
                if (existingPeer != sender &&
                        correctInfo.getPeerId().equals(existingPeer.getRemotePeerId())) {
                    if (nodeId.compareTo(correctInfo.getPeerId()) < 0) {
                        sender.disconnect();
                        return;
                    } else {
                        existingPeer.disconnect();
                        break;
                    }
                }
            }

            sender.setRemotePeerId(correctInfo.getPeerId());
            System.out.println("[" + nodeId + "] ✓ " + correctInfo.getPeerId() + " identifié et connecté.");

            // Nettoyer les entrées temporaires (ex: "ip:port") qui pointent vers le même hôte/port
            for (Map.Entry<String, PeerInfo> entry : knownPeers.entrySet()) {
                PeerInfo info = entry.getValue();
                if (!entry.getKey().equals(correctInfo.getPeerId())
                        && info.getHost().equals(correctInfo.getHost())
                        && info.getPort() == correctInfo.getPort()) {
                    knownPeers.remove(entry.getKey());
                }
            }

            // On stocke la BONNE info dans knownPeers
            knownPeers.put(correctInfo.getPeerId(), correctInfo);

            // On envoie la liste des pairs au nouveau venu
            sendPeerListTo(sender);

            // IMPORTANT: On diffuse la BONNE info (IP corrigée) à tout le monde
            broadcastNewPeer(correctInfo);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

            // Relais (conserve le maillage si connexion directe impossible)
            for (GestionConnection peer : connectedPeers) {
                if (peer != sender) {
                    peer.sendMessage(message);
                }
            }
        } catch (NumberFormatException e) {
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
            // Si on reçoit une info de pair, on tente de s'y connecter
            if (peerInfo == null || peerInfo.getPeerId().equals(nodeId) || knownPeers.containsKey(peerInfo.getPeerId())) return;
            connectToNode(peerInfo.getPeerId(), peerInfo.getHost(), peerInfo.getPort());
        } catch (Exception e) {}
    }

    private void processPeerLeftMessage(String message) {
        try {
            String peerId = message.substring(10).trim();
            if (peerId.isEmpty() || peerId.equals(nodeId)) return;
            handlePeerLeft(peerId, false);
        } catch (Exception e) {
        }
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
        if (peerId != null) {
            handlePeerLeft(peerId, true);
        }
    }

    protected void onPeerDisconnected(String peerId) {
    }

    private void handlePeerLeft(String peerId, boolean broadcast) {
        if (peerId == null || peerId.equals(nodeId)) return;
        knownPeers.remove(peerId);
        playerPositions.remove(peerId);
        if (broadcast) {
            broadcastToPeers("PEER_LEFT:" + peerId);
        }
        onPeerDisconnected(peerId);
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

