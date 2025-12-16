package game;

import Reseau.GestionConnection;
import Reseau.Serveur;
import prototype_raycasting.Joueur;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameNetworkAdapter {

    private ServeurGame serveur;
    private Joueur localPlayer;
    private Map<String, Joueur> remotePlayers;
    private NetworkListener listener;


    public GameNetworkAdapter(String nodeId, String host, int port) {
        this.serveur = new ServeurGame(nodeId, host, port, this);
        this.remotePlayers = new ConcurrentHashMap<>();
    }

    public void setLocalPlayer(Joueur player) {
        this.localPlayer = player;
        this.localPlayer.setId(serveur.getNodeId());
    }

    public void setNetworkListener(NetworkListener listener) {
        this.listener = listener;
    }

    public void start() {
        serveur.start();
    }

    public void connectToPlayer(String playerId, String host, int port) {
        serveur.connectToNode(playerId, host, port);
    }

    public void sendPlayerPosition() {
        if (localPlayer == null) return;
        String message = "MOVE:" + localPlayer.getId() + ":" + localPlayer.toNetworkString();
        serveur.broadcastToPeers(message);
    }

    public void sendPlayerPositionNow() {
        if (localPlayer == null) return;
        String message = "MOVE:" + localPlayer.getId() + ":" + localPlayer.toNetworkString();
        serveur.broadcastToPeers(message);
    }

    void sendPlayerPositionTo(GestionConnection peer) {
        if (localPlayer == null) return;
        String message = "MOVE:" + localPlayer.getId() + ":" + localPlayer.toNetworkString();
        peer.sendMessage(message);
    }

    void onPositionReceived(String playerId, String positionData) {
        if (localPlayer != null && playerId.equals(localPlayer.getId())) {
            return;
        }

        boolean isNewPlayer = false;
        Joueur remotePlayer = remotePlayers.get(playerId);

        if (remotePlayer == null) {
            remotePlayer = Joueur.fromNetwork(playerId, positionData);
            remotePlayers.put(playerId, remotePlayer);
            isNewPlayer = true;
        } else {
            remotePlayer.fromNetworkString(positionData);
        }

        if (listener != null) {
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

    void onPlayerDisconnected(String playerId) {
        Joueur removed = remotePlayers.remove(playerId);
        if (removed != null && listener != null) {
            listener.onPlayerLeave(playerId);
        }
    }

    void onNewPeerConnected(String peerId) {
        sendPlayerPositionNow();
    }

    public Map<String, Joueur> getRemotePlayers() {
        return remotePlayers;
    }

    public Joueur getRemotePlayer(String playerId) {
        return remotePlayers.get(playerId);
    }

    public void shutdown() {
        serveur.shutdown();
    }

    public String getNodeId() {
        return serveur.getNodeId();
    }

    private static class ServeurGame extends Serveur {
        private GameNetworkAdapter adapter;
        private String nodeId;

        private Map<String, Long> lastRelayTime = new ConcurrentHashMap<>();
        private static final long RELAY_MIN_INTERVAL_MS = 30;

        public ServeurGame(String nodeId, String host, int port, GameNetworkAdapter adapter) {
            super(nodeId, host, port);
            this.nodeId = nodeId;
            this.adapter = adapter;
        }

        @Override
        protected void onPeerDisconnected(String peerId) {
            adapter.onPlayerDisconnected(peerId);
        }

        @Override
        protected void onPeerConnected(String peerId) {
            adapter.onNewPeerConnected(peerId);
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

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
                //ignorer les erreurs de parsing
            }
        }

        protected java.util.List<GestionConnection> getConnectedPeers() {
            return getConnectedPeersList();
        }
    }
}
