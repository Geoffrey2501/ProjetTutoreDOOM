package Reseau;

import java.util.Scanner;

/**
 * Client P2P - Nœud qui se connecte au serveur
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class P2PClient {
    /**
     * Point d'entrée du client P2P
     *
     * @param args Arguments non utilisés
     * @throws InterruptedException si un thread est interrompu
     */
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Démarrage du Client P2P ===\n");

        // Demander le nom et le port du client
        System.out.print("Nom du joueur : ");
        String playerName = scanner.nextLine();

        System.out.print("Port du client (ex: 5002) : ");
        int clientPort = scanner.nextInt();
        scanner.nextLine(); // Consommer le retour à la ligne

        System.out.print("Adresse IP du serveur (ex: 192.168.1.10 ou localhost) : ");
        String serverIp = scanner.nextLine().trim();
        if (serverIp.isEmpty()) {
            serverIp = "localhost";
        }

        System.out.print("Port du serveur (ex: 5001) : ");
        int serverPort = scanner.nextInt();
        scanner.nextLine(); // Consommer le retour à la ligne

        // Créer le nœud client
        Serveur clientNode = new Serveur(playerName, "0.0.0.0", clientPort);
        clientNode.start();
        Thread.sleep(1000);

        System.out.println("\nClient démarré sur le port " + clientPort);

        // Se connecter au serveur
        System.out.println("Connexion au serveur sur " + serverIp + ":" + serverPort + "...\n");
        clientNode.connectToNode("Serveur", serverIp, serverPort);
        Thread.sleep(2000);

        // Afficher l'état
        clientNode.printStatus();

        System.out.println("\nLe client reçoit maintenant les mises à jour du réseau P2P");
        System.out.println("Appuyez sur Entrée pour quitter...");
        scanner.nextLine();
        scanner.nextLine();

        clientNode.shutdown();
        scanner.close();
    }
}

