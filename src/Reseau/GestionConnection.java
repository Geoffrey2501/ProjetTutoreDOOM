package Reseau;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gère la connexion bidirectionnelle avec un autre pair dans le système P2P
 * @author Groupe DOOM
 * @version 1.1
 */
public class GestionConnection implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Serveur localNode;
    private String remotePeerId;

    // Système de tick rate
    private static final long TICK_INTERVAL_MS = 16;
    private final AtomicReference<String> pendingMoveMessage = new AtomicReference<>(null);
    private Thread tickThread;
    private volatile boolean running = true;

    public GestionConnection(Socket socket, Serveur localNode) {
        this.socket = socket;
        this.localNode = localNode;
        try {
            socket.setTcpNoDelay(true);
            socket.setPerformancePreferences(0, 1, 0);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            startTickSystem();
        } catch (IOException e) {
            // Ignorer
        }
    }

    private void startTickSystem() {
        tickThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(TICK_INTERVAL_MS);
                    String message = pendingMoveMessage.getAndSet(null);
                    if (message != null && out != null) {
                        out.println(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        tickThread.setDaemon(true);
        tickThread.setPriority(Thread.MAX_PRIORITY);
        tickThread.start();
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                localNode.processMessageFromPeer(message, this);
            }
        } catch (IOException e) {
            //déconnexion
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String message) {
        if (out != null && message != null) {
            if (message.startsWith("MOVE:")) {
                pendingMoveMessage.set(message);
            } else {
                out.println(message);
            }
        }
    }

    public void setRemotePeerId(String peerId) {
        this.remotePeerId = peerId;
    }

    public void disconnect() {
        running = false;
        if (tickThread != null) {
            tickThread.interrupt();
        }
        try {
            socket.close();
            localNode.removePeer(this);
        } catch (IOException e) {
            System.err.println("Error closing socket : " + e.getMessage());
        }
    }

    public String getRemotePeerId() {
        return remotePeerId;
    }

    public SocketAddress getRemoteAddress() {
        return socket.getRemoteSocketAddress();
    }

    public String getRemoteIPAddress() {
        if (socket != null && socket.getInetAddress() != null) {
            return socket.getInetAddress().getHostAddress();
        }
        return null;
    }
}