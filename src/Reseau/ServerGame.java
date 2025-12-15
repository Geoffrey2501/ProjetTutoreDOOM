package Reseau;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Serveur multijoueur centralisé pour le projet Doom-like
 *
 * Le serveur agit comme autorité centrale pour l'état du jeu.
 * Il gère les connexions des clients et distribue les mises à jour
 * des positions des joueurs à tous les clients connectés.
 *
 * Format des messages : "NomJoueur:X, Y"
 *
 */
public class ServerGame {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private int port;

    /**
     * Constructeur du serveur
     *
     * @param port Port d'écoute du serveur (ex: 5000)
     */
    public ServerGame(int port) {
        this.port = port;
    }

    /**
     * Démarrer le serveur
     *
     * Écoute les connexions entrantes et crée un thread pour gérer chaque client.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serveur démarré sur le port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouveau client connecté: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Envoyer un message à tous les clients sauf l'expéditeur
     *
     * @param message Message à diffuser
     * @param sender  Client qui envoie le message
     */
    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Enlever un client déconnecté
     *
     * @param client Client à retirer
     */
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client déconnecté. Nombre de clients: " + clients.size());
    }

    /**
     * Retourner la liste des clients connectés
     *
     * @return List des clients
     */
    public List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }
}

