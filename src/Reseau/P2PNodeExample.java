package Reseau;

/**
 * Exemple d'utilisation du système P2P avec maillage complet
 *
 * Crée 4 nœuds P2P qui se connectent les uns aux autres.
 * Démonstration du maillage complet : quand J2 se connecte à J1,
 * J1 lui envoie la liste des autres pairs (J3, J4...), et J2 se connecte
 * automatiquement à tous.
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class P2PNodeExample {
    /**
     * Point d'entrée du programme d'exemple P2P
     *
     * @param args Arguments non utilisés
     * @throws InterruptedException si un thread est interrompu
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Exemple P2P avec Maillage Complet ===\n");

        // Créer quatre nœuds P2P
        P2PNode node1 = new P2PNode("J1", "localhost", 5001);
        P2PNode node2 = new P2PNode("J2", "localhost", 5002);
        P2PNode node3 = new P2PNode("J3", "localhost", 5003);
        P2PNode node4 = new P2PNode("J4", "localhost", 5004);

        // Démarrer les nœuds
        System.out.println("Démarrage des nœuds...\n");
        node1.start();
        node2.start();
        node3.start();
        node4.start();
        Thread.sleep(1000);

        // Étape 1 : J2 se connecte à J1
        System.out.println("\n=== ÉTAPE 1 : J2 se connecte à J1 ===");
        node2.connectToNode("J1", "localhost", 5001);
        Thread.sleep(1000);

        // Étape 2 : J3 se connecte à J1
        // J1 envoie la liste [J1, J2] à J3
        // J3 se connecte automatiquement à J2
        System.out.println("\n=== ÉTAPE 2 : J3 se connecte à J1 ===");
        System.out.println("J1 va envoyer sa liste de pairs [J1, J2] à J3");
        System.out.println("J3 va se connecter automatiquement à J2 (maillage complet)");
        node3.connectToNode("J1", "localhost", 5001);
        Thread.sleep(2000);

        // Étape 3 : J4 se connecte à J2
        // J2 envoie la liste [J1, J2, J3] à J4
        // J4 se connecte automatiquement à J1 et J3
        System.out.println("\n=== ÉTAPE 3 : J4 se connecte à J2 ===");
        System.out.println("J2 va envoyer sa liste de pairs [J1, J2, J3] à J4");
        System.out.println("J4 va se connecter automatiquement à J1 et J3 (maillage complet)");
        node4.connectToNode("J2", "localhost", 5002);
        Thread.sleep(2000);

        // Simuler les mouvements - tous les nœuds doivent recevoir toutes les positions
        System.out.println("\n=== Test de diffusion des mouvements ===\n");
        node1.movePlayer(100, 200);
        Thread.sleep(300);
        node2.movePlayer(150, 250);
        Thread.sleep(300);
        node3.movePlayer(300, 100);
        Thread.sleep(300);
        node4.movePlayer(400, 50);
        Thread.sleep(1000);

        // Afficher l'état de chaque nœud
        System.out.println("\n=== État final de chaque nœud ===");
        node1.printStatus();
        node2.printStatus();
        node3.printStatus();
        node4.printStatus();

        System.out.println("Vérification du maillage complet:");
        System.out.println("Chaque nœud devrait avoir 4 pairs connus (incluant lui-même)");
        System.out.println("et 3 connexions actives aux autres nœuds.\n");

        Thread.sleep(2000);

        // Arrêter tous les nœuds
        System.out.println("Arrêt des nœuds...");
        node1.shutdown();
        node2.shutdown();
        node3.shutdown();
        node4.shutdown();
    }
}

