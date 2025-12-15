package Reseau;

import java.io.*;
import java.net.*;

/**
 * Gère la connexion d'un client spécifique avec le serveur
 *
 * Chaque ClientHandler représente un joueur connecté au serveur.
 * Il traite les actions du client, les valide et les relaye aux autres joueurs.
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ServerGame server;
    private String playerId;
    private int posX = 0;
    private int posY = 0;

    /**
     * Constructeur du gestionnaire de client
     *
     * @param socket Socket de connexion du client
     * @param server Référence au serveur
     */
    public ClientHandler(Socket socket, ServerGame server) {
        this.socket = socket;
        this.server = server;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Boucle principale de gestion du client
     * Reçoit les messages du client et les traite continuellement.
     */
    @Override
    public void run() {
        try {
            playerId = in.readLine();
            if (playerId == null) {
                return; // Connexion fermée avant identification
            }
            System.out.println("Joueur enregistré: " + playerId);

            out.println("CONNECTED:" + playerId);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + playerId + "]: " + message);

                processAction(message);

                server.broadcastMessage(playerId + ":" + message, this);
            }
            // Déconnexion normale (readLine retourne null)
            System.out.println("Joueur déconnecté: " + playerId);
        } catch (IOException e) {
            // Vérifier si c'est une fermeture de socket normale
            if (!socket.isClosed()) {
                System.out.println("Erreur de communication avec " + playerId);
            }
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
                server.removeClient(this);
            } catch (IOException e) {
                // Ignorer les erreurs de fermeture
            }
        }
    }

    /**
     * Traiter les actions reçues du client
     *
     * @param action Action à traiter (format: "X,Y")
     */
    private void processAction(String action) {
        // Ignorer les actions null ou vides
        if (action == null || action.trim().isEmpty()) {
            return;
        }

        try {
            String[] coords = action.split(",");

            // Vérifier qu'on a exactement 2 coordonnées
            if (coords.length != 2) {
                return; // Format invalide, ignorer silencieusement
            }

            // Vérifier que les coordonnées ne sont pas vides
            if (coords[0].trim().isEmpty() || coords[1].trim().isEmpty()) {
                return; // Coordonnées vides, ignorer
            }

            posX = Integer.parseInt(coords[0].trim());
            posY = Integer.parseInt(coords[1].trim());
            System.out.println(playerId + " -> (" + posX + ", " + posY + ")");
        } catch (NumberFormatException e) {
            // Coordonnées non numériques, ignorer silencieusement
        }
    }

    /**
     * Envoyer un message au client
     *
     * @param message Message à envoyer
     */
    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * Obtenir l'ID du joueur
     *
     * @return ID du joueur
     */
    public String getPlayerId() {
        return playerId;
    }

    /**
     * Obtenir la position X du joueur
     *
     * @return Position X
     */
    public int getPosX() {
        return posX;
    }

    /**
     * Obtenir la position Y du joueur
     *
     * @return Position Y
     */
    public int getPosY() {
        return posY;
    }
}

