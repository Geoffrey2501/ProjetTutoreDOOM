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
        System.out.println("\n=== Commandes disponibles ===");
        System.out.println("  pos X Y    - Envoyer une position (ex: pos 100 200)");
        System.out.println("  move X Y   - Déplacer le joueur (ex: move 50 75)");
        System.out.println("  status     - Afficher l'état du réseau");
        System.out.println("  quit       - Quitter le client");
        System.out.println("=============================\n");

        // Boucle de commande interactive
        boolean running = true;
        while (running) {
            System.out.print("[" + playerName + "] > ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            switch (command) {
                case "pos":
                case "move":
                    if (parts.length >= 3) {
                        try {
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            clientNode.movePlayer(x, y);
                            System.out.println("Position envoyée: (" + x + ", " + y + ")");
                        } catch (NumberFormatException e) {
                            System.out.println("Erreur: Les coordonnées doivent être des nombres entiers.");
                            System.out.println("Usage: pos X Y (ex: pos 100 200)");
                        }
                    } else {
                        System.out.println("Usage: pos X Y (ex: pos 100 200)");
                    }
                    break;

                case "status":
                    clientNode.printStatus();
                    break;

                case "quit":
                case "exit":
                case "q":
                    running = false;
                    System.out.println("Déconnexion en cours...");
                    break;

                default:
                    System.out.println("Commande inconnue: " + command);
                    System.out.println("Commandes disponibles: pos, move, status, quit");
                    break;
            }
        }

        clientNode.shutdown();
        scanner.close();
        System.out.println("Client arrêté.");
    }
}

