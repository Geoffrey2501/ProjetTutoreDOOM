package Reseau;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gère la connexion bidirectionnelle avec un autre pair dans le système P2P
 *
 * Cette classe implémente Runnable pour permettre une communication asynchrone.
 * Elle envoie et reçoit les messages de coordonnées (X, Y) des joueurs.
 *
 * Chaque connexion relaie les messages reçus au nœud local pour traitement.
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class GestionConnection implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Serveur localNode;
    private String remotePeerId;

    // Système de tick rate
    private static final long TICK_INTERVAL_MS = 16; // ~60 ticks/seconde
    private AtomicReference<String> pendingMoveMessage = new AtomicReference<>(null);
    private Thread tickThread;
    private volatile boolean running = true;

    /**
     * Constructeur de la connexion pair-à-pair
     *
     * @param socket    Socket de connexion avec le pair
     * @param localNode Référence au nœud local qui gère cette connexion
     */
    public GestionConnection(Socket socket, Serveur localNode) {
        this.socket = socket;
        this.localNode = localNode;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            startTickSystem();
        } catch (IOException e) {
            // Ignorer
        }
    }

    /**
     * Démarre le système de tick rate pour l'envoi régulier des messages
     */
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
        tickThread.start();
    }

    /**
     * Exécute la boucle de réception des messages du pair
     *
     * Écoute continuellement le socket et traite les messages reçus.
     */
    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                localNode.processMessageFromPeer(message, this);
            }
        } catch (IOException e) {
            //deconnexion
        } finally {
            disconnect();
        }
    }

    /**
     * Envoyer un message au pair avec tick rate
     *
     * Format : "NomJoueur:X,Y"
     *
     * @param message Message à envoyer
     */
    public void sendMessage(String message) {
        if (out != null && message != null) {
            if (message.startsWith("MOVE:")) {
                // Stocker le dernier message MOVE, sera envoyé au prochain tick
                pendingMoveMessage.set(message);
            } else {
                // Envoyer immédiatement les autres types de messages
                out.println(message);
            }
        }
    }

    /**
     * Définir l'identifiant du pair distant
     *
     * @param peerId Identifiant unique du pair (ex: "J1")
     */
    public void setRemotePeerId(String peerId) {
        this.remotePeerId = peerId;
    }

    /**
     * Fermer la connexion avec le pair
     *
     * Ferme le socket et notifie le nœud local de la déconnexion.
     */
    public void disconnect() {
        running = false;
        if (tickThread != null) {
            tickThread.interrupt();
        }
        try {
            socket.close();
            localNode.removePeer(this);
        } catch (IOException e) {
        }
    }

    /**
     * Obtenir l'identifiant du pair distant
     *
     * @return ID du pair distant
     */
    public String getRemotePeerId() {
        return remotePeerId;
    }
}
