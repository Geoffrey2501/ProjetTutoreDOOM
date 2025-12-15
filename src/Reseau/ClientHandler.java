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
            System.out.println("Joueur enregistré: " + playerId);

            out.println("CONNECTED:" + playerId);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + playerId + "]: " + message);

                processAction(message);

                server.broadcastMessage(playerId + ":" + message, this);
            }
        } catch (IOException e) {
            System.out.println("Erreur de communication avec " + playerId);
        } finally {
            try {
                socket.close();
                server.removeClient(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Traiter les actions reçues du client
     *
     * @param action Action à traiter (format: "X,Y")
     */
    private void processAction(String action) {
        try {
            String[] coords = action.split(",");
            if (coords.length == 2) {
                posX = Integer.parseInt(coords[0]);
                posY = Integer.parseInt(coords[1]);
                System.out.println(playerId + " -> (" + posX + ", " + posY + ")");
            }
        } catch (NumberFormatException e) {
            System.err.println("Erreur parsing action: " + action);
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

