package Reseau;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

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

    // Rate limiting pour l'envoi
    private AtomicLong lastSendTime = new AtomicLong(0);
    private static final long MIN_SEND_INTERVAL_MS = 20; // Max ~50 messages/seconde

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
        } catch (IOException e) {
            // Ignorer
        }
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
     * Envoyer un message au pair avec rate limiting
     *
     * Format : "NomJoueur:X,Y"
     *
     * @param message Message à envoyer
     */
    public void sendMessage(String message) {
        if (out != null) {
            long now = System.currentTimeMillis();
            long last = lastSendTime.get();

            //rate limiting seulement pour les messages MOVE (pas pour PEER_LIST, etc.)
            if (message.startsWith("MOVE:")) {
                if (now - last < MIN_SEND_INTERVAL_MS) {
                    return; //ignorer ce message, trop rapide
                    //sinon sa plante certain pc à cause de la surcharge du buffer
                    //TODO: implementer des tickrate pour eviter ce probleme et ne pas perdre de messages
                }
                lastSendTime.set(now);
            }

            out.println(message);
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
