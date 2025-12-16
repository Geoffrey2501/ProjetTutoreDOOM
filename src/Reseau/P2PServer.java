package Reseau;

/**
 * Serveur P2P - Premier nœud qui attend les connexions
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class P2PServer {
    /**
     * Point d'entrée du serveur P2P
     *
     * @param args Arguments non utilisés
     * @throws InterruptedException si un thread est interrompu
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Démarrage du Serveur P2P ===\n");

        // Créer le premier nœud (serveur) - écoute sur toutes les interfaces
        Serveur serverNode = new Serveur("Serveur", "0.0.0.0", 5001);
        serverNode.start();

        System.out.println("Serveur démarré sur le port 5001");
        System.out.println("Les clients peuvent se connecter depuis d'autres machines");
        System.out.println("En attente de connexions des clients...\n");

        // Garder le serveur actif
        while (true) {
            Thread.sleep(2000);
            serverNode.printStatus();
        }
    }
}

