package Reseau;

/**
 * Exemple d'utilisation du système P2P
 *
 * Crée 3 nœuds P2P qui se connectent les uns aux autres et simulent
 * des mouvements de joueurs. Les coordonnées sont diffusées à tous les pairs.
 *
 */
public class P2PNodeExample {
    /**
     * Point d'entrée du programme d'exemple P2P
     *
     * @param args Arguments non utilisés
     * @throws InterruptedException si un thread est interrompu
     */
    public static void main(String[] args) throws InterruptedException {
        // Créer trois nœuds P2P
        P2PNode node1 = new P2PNode("J1", 5001);
        P2PNode node2 = new P2PNode("J2", 5002);
        P2PNode node3 = new P2PNode("J3", 5003);

        // Démarrer les nœuds
        node1.start();
        node2.start();
        node3.start();
        Thread.sleep(500);

        // Établir les connexions
        System.out.println("\n=== Connexions ===\n");
        node2.connectToNode("J1", "localhost", 5001);
        node3.connectToNode("J1", "localhost", 5001);
        Thread.sleep(500);

        // Simuler les mouvements
        System.out.println("\n=== Positions ===\n");
        node1.movePlayer(100, 200);
        Thread.sleep(200);
        node2.movePlayer(150, 250);
        Thread.sleep(200);
        node3.movePlayer(300, 100);
        Thread.sleep(500);

        // Afficher l'état
        node1.printStatus();
        node2.printStatus();
        node3.printStatus();

        Thread.sleep(2000);

        // Arrêter
        node1.shutdown();
        node2.shutdown();
        node3.shutdown();
    }
}

