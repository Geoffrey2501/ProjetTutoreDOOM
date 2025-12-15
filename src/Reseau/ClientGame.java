package Reseau;

import java.io.*;
import java.net.*;

/**
 * Client du jeu pour se connecter au serveur multijoueur
 *
 * Le client envoie les coordonnées du joueur au serveur
 * et reçoit les mises à jour de l'état du jeu.
 *
 */
public class ClientGame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private int posX = 0;
    private int posY = 0;

    /**
     * Constructeur du client
     *
     * @param host     Adresse IP du serveur
     * @param port     Port du serveur
     * @param playerId ID du joueur
     */
    public ClientGame(String host, int port, String playerId) {
        this.playerId = playerId;
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(playerId);

            String response = in.readLine();
            System.out.println("Serveur: " + response);

            new Thread(this::receiveMessages).start();
        } catch (IOException e) {
            System.err.println("Erreur de connexion au serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoyer une action de déplacement
     *
     * @param x Coordonnée X
     * @param y Coordonnée Y
     */
    public void movePlayer(int x, int y) {
        posX = x;
        posY = y;
        out.println(x + "," + y);
    }

    /**
     * Envoyer une action personnalisée
     *
     * @param action Action à envoyer
     */
    public void sendAction(String action) {
        out.println(action);
    }

    /**
     * Recevoir les messages du serveur en continu
     */
    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Message reçu: " + message);
                updateGameState(message);
            }
        } catch (IOException e) {
            System.out.println("Déconnecté du serveur");
        }
    }

    /**
     * Mettre à jour l'état du jeu avec les informations reçues
     *
     * @param message Message reçu du serveur
     */
    private void updateGameState(String message) {
        System.out.println("État mis à jour: " + message);
    }

    /**
     * Fermer la connexion
     */
    public void disconnect() {
        try {
            socket.close();
            System.out.println("Déconnecté du serveur");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public int getPosX() {
        return posX;
    }
    public int getPosY() {
        return posY;
    }
    public String getPlayerId() {
        return playerId;
    }
}

