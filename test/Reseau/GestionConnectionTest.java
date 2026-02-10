package Reseau;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe GestionConnection
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class GestionConnectionTest {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Socket serverSideSocket;
    private GestionConnection gestionConnection;
    private Serveur mockNode;
    private static int testPort = 10001;

    private PrintWriter clientOut;
    private BufferedReader clientIn;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        testPort += 10;
        mockNode = new Serveur("MockNode", "localhost", testPort + 100);
        mockNode.start();

        // Créer un serveur temporaire pour les tests
        serverSocket = new ServerSocket(testPort);
        serverSocket.setSoTimeout(5000);

        CountDownLatch connectionLatch = new CountDownLatch(1);

        // Thread pour accepter la connexion
        Thread serverThread = new Thread(() -> {
            try {
                serverSideSocket = serverSocket.accept();
                gestionConnection = new GestionConnection(serverSideSocket, mockNode);
                connectionLatch.countDown();
            } catch (IOException e) {
                connectionLatch.countDown();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Connecter le client
        clientSocket = new Socket("localhost", testPort);
        clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
        clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        assertTrue(connectionLatch.await(3, TimeUnit.SECONDS), "La connexion doit être établie");
    }


    @AfterEach
    void tearDown() {
        try {
            if (mockNode != null) mockNode.shutdown();
            if (clientIn != null) clientIn.close();
            if (clientOut != null) clientOut.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            if (serverSideSocket != null && !serverSideSocket.isClosed()) serverSideSocket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            // Ignorer les erreurs de fermeture
        }
    }

    // ==================== Tests de création ====================

    @Test
    @DisplayName("Test création de GestionConnection")
    void testGestionConnectionCreation() {
        assertNotNull(gestionConnection, "GestionConnection doit être créé");
    }

    // ==================== Tests de setRemotePeerId / getRemotePeerId ====================

    @Test
    @DisplayName("Test setRemotePeerId et getRemotePeerId")
    void testSetAndGetRemotePeerId() {
        gestionConnection.setRemotePeerId("TestPeer");
        assertEquals("TestPeer", gestionConnection.getRemotePeerId());
    }

    @Test
    @DisplayName("Test getRemotePeerId avant initialisation")
    void testGetRemotePeerIdBeforeInit() {
        assertNull(gestionConnection.getRemotePeerId(), "RemotePeerId doit être null initialement");
    }

    @Test
    @DisplayName("Test setRemotePeerId avec null")
    void testSetRemotePeerIdNull() {
        gestionConnection.setRemotePeerId(null);
        assertNull(gestionConnection.getRemotePeerId());
    }

    // ==================== Tests de sendMessage ====================

    @Test
    @DisplayName("Test envoi de message non-MOVE (immédiat)")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendMessage() throws IOException {
        String testMessage = "PEER_LIST:J1,J2";

        gestionConnection.sendMessage(testMessage);

        String received = clientIn.readLine();
        assertEquals(testMessage, received, "Le message non-MOVE doit être reçu immédiatement");
    }

    @Test
    @DisplayName("Test envoi de message MOVE avec tick rate")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendMoveMessage() throws IOException, InterruptedException {
        String testMessage = "MOVE:J1:100,200";

        gestionConnection.sendMessage(testMessage);

        // Attendre le prochain tick (50ms max)
        Thread.sleep(100);

        String received = clientIn.readLine();
        assertEquals(testMessage, received, "Le message MOVE doit être reçu au prochain tick");
    }

    @Test
    @DisplayName("Test plusieurs messages MOVE rapides - seul le dernier est envoyé")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testMultipleMoveMessagesOnlyLastSent() throws IOException, InterruptedException {
        // Envoyer plusieurs messages MOVE rapidement
        gestionConnection.sendMessage("MOVE:J1:100,200");
        gestionConnection.sendMessage("MOVE:J1:150,250");
        gestionConnection.sendMessage("MOVE:J1:200,300");

        // Attendre le prochain tick
        Thread.sleep(100);

        // Seul le dernier message doit être reçu
        String received = clientIn.readLine();
        assertEquals("MOVE:J1:200,300", received, "Seul le dernier message MOVE doit être envoyé");

        // Vérifier qu'il n'y a pas d'autres messages en attente (avec timeout court)
        clientSocket.setSoTimeout(200);
        assertThrows(SocketTimeoutException.class, () -> clientIn.readLine(),
                "Aucun autre message ne doit être en attente");
    }

    @Test
    @DisplayName("Test envoi de plusieurs messages non-MOVE")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendMultipleMessages() throws IOException {
        gestionConnection.sendMessage("CONNECT:J1");
        gestionConnection.sendMessage("CONNECT:J2");
        gestionConnection.sendMessage("CONNECT:J3");

        assertEquals("CONNECT:J1", clientIn.readLine());
        assertEquals("CONNECT:J2", clientIn.readLine());
        assertEquals("CONNECT:J3", clientIn.readLine());
    }

    @Test
    @DisplayName("Test envoi de message vide")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendEmptyMessage() throws IOException {
        gestionConnection.sendMessage("");

        String received = clientIn.readLine();
        assertEquals("", received, "Un message vide doit être reçu comme chaîne vide");
    }

    // ==================== Tests de run (réception de messages) ====================

    @Test
    @DisplayName("Test réception de message de position")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testReceivePositionMessage() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        // Envoyer un message de position depuis le "client"
        clientOut.println("TestPlayer:150,250");

        // Attendre le traitement
        Thread.sleep(300);

        // Vérifier que le nœud a reçu la position
        var positions = mockNode.getPlayerPositions();
        assertTrue(positions.containsKey("TestPlayer"), "Le joueur TestPlayer doit être ajouté");

        int[] pos = positions.get("TestPlayer");
        assertEquals(150, pos[0], "Position X doit être 150");
        assertEquals(250, pos[1], "Position Y doit être 250");
    }

    @Test
    @DisplayName("Test réception de plusieurs messages")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testReceiveMultipleMessages() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        clientOut.println("Player1:100,100");
        clientOut.println("Player2:200,200");
        clientOut.println("Player3:300,300");

        Thread.sleep(500);

        var positions = mockNode.getPlayerPositions();
        assertTrue(positions.containsKey("Player1"));
        assertTrue(positions.containsKey("Player2"));
        assertTrue(positions.containsKey("Player3"));
    }

    // ==================== Tests de disconnect ====================

    @Test
    @DisplayName("Test déconnexion")
    void testDisconnect() {
        assertDoesNotThrow(() -> gestionConnection.disconnect(),
                "La déconnexion ne doit pas lever d'exception");
    }

    @Test
    @DisplayName("Test double déconnexion ne crash pas")
    void testDoubleDisconnect() {
        gestionConnection.disconnect();
        assertDoesNotThrow(() -> gestionConnection.disconnect(),
                "Une double déconnexion ne doit pas lever d'exception");
    }

    @Test
    @DisplayName("Test envoi de message après déconnexion")
    void testSendMessageAfterDisconnect() {
        gestionConnection.disconnect();
        // Ne devrait pas lever d'exception
        assertDoesNotThrow(() -> gestionConnection.sendMessage("Test"));
    }

    // ==================== Tests de messages mal formés ====================

    @Test
    @DisplayName("Test message mal formé ne crash pas")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMalformedMessage() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        // Envoyer des messages mal formés
        clientOut.println("invalid_message");
        clientOut.println("no_colon");
        clientOut.println(":no_player");
        clientOut.println("player:");

        Thread.sleep(300);

        // Ne doit pas crash - le thread doit toujours tourner
        assertTrue(connectionThread.isAlive(), "Le thread doit continuer après des messages invalides");
    }

    @Test
    @DisplayName("Test message avec coordonnées invalides")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testInvalidCoordinates() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        // Envoyer des coordonnées invalides
        clientOut.println("Player:abc,def");
        clientOut.println("Player:100");
        clientOut.println("Player:,200");

        Thread.sleep(300);

        // Le thread doit continuer
        assertTrue(connectionThread.isAlive());
    }

    // ==================== Tests de robustesse ====================

    @Test
    @DisplayName("Test envoi de message null")
    void testSendNullMessage() {
        assertDoesNotThrow(() -> gestionConnection.sendMessage(null));
    }

    @Test
    @DisplayName("Test envoi de message très long")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendVeryLongMessage() throws IOException {
        String longMessage = "Player:" + "1".repeat(10000) + ",200";
        gestionConnection.sendMessage(longMessage);

        String received = clientIn.readLine();
        assertEquals(longMessage, received);
    }

    @Test
    @DisplayName("Test envoi de caractères spéciaux")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendSpecialCharacters() throws IOException {
        String specialMessage = "Player:éàü日本語🎮";
        gestionConnection.sendMessage(specialMessage);

        String received = clientIn.readLine();
        assertEquals(specialMessage, received);
    }

    @Test
    @DisplayName("Test réception de coordonnées négatives")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testReceiveNegativeCoordinates() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        clientOut.println("Player:-100,-200");
        Thread.sleep(300);

        var positions = mockNode.getPlayerPositions();
        assertTrue(positions.containsKey("Player"));
        int[] pos = positions.get("Player");
        assertEquals(-100, pos[0]);
        assertEquals(-200, pos[1]);
    }

    @Test
    @DisplayName("Test réception de coordonnées zéro")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testReceiveZeroCoordinates() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        clientOut.println("Player:0,0");
        Thread.sleep(300);

        var positions = mockNode.getPlayerPositions();
        assertTrue(positions.containsKey("Player"));
        int[] pos = positions.get("Player");
        assertEquals(0, pos[0]);
        assertEquals(0, pos[1]);
    }

    @Test
    @DisplayName("Test réception de grandes coordonnées")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testReceiveLargeCoordinates() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        clientOut.println("Player:999999,999999");
        Thread.sleep(300);

        var positions = mockNode.getPlayerPositions();
        assertTrue(positions.containsKey("Player"));
        int[] pos = positions.get("Player");
        assertEquals(999999, pos[0]);
        assertEquals(999999, pos[1]);
    }

    @Test
    @DisplayName("Test mise à jour de position d'un joueur existant")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testUpdateExistingPlayerPosition() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        clientOut.println("Player:100,100");
        Thread.sleep(200);

        clientOut.println("Player:200,300");
        Thread.sleep(200);

        var positions = mockNode.getPlayerPositions();
        int[] pos = positions.get("Player");
        assertEquals(200, pos[0], "Position X doit être mise à jour");
        assertEquals(300, pos[1], "Position Y doit être mise à jour");
    }

    @Test
    @DisplayName("Test réception rapide de plusieurs messages")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRapidMessageReception() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        for (int i = 0; i < 50; i++) {
            clientOut.println("Player" + i + ":" + i + "," + (i * 2));
        }
        Thread.sleep(500);

        var positions = mockNode.getPlayerPositions();
        assertTrue(positions.size() >= 10, "Plusieurs joueurs doivent être enregistrés");
    }

    // ==================== Tests setRemotePeerId avancés ====================

    @Test
    @DisplayName("Test setRemotePeerId avec chaîne vide")
    void testSetRemotePeerIdEmpty() {
        gestionConnection.setRemotePeerId("");
        assertEquals("", gestionConnection.getRemotePeerId());
    }

    @Test
    @DisplayName("Test setRemotePeerId avec caractères spéciaux")
    void testSetRemotePeerIdSpecialChars() {
        gestionConnection.setRemotePeerId("Player-1_Test@123");
        assertEquals("Player-1_Test@123", gestionConnection.getRemotePeerId());
    }

    @Test
    @DisplayName("Test changement de remotePeerId")
    void testChangeRemotePeerId() {
        gestionConnection.setRemotePeerId("Peer1");
        assertEquals("Peer1", gestionConnection.getRemotePeerId());

        gestionConnection.setRemotePeerId("Peer2");
        assertEquals("Peer2", gestionConnection.getRemotePeerId());
    }

    // ==================== Tests de messages avec espaces ====================

    @Test
    @DisplayName("Test message avec espaces")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMessageWithSpaces() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        clientOut.println("Player : 100 , 200");
        Thread.sleep(300);

        // Le message avec espaces ne devrait pas être parsé correctement
        // mais ne devrait pas faire crasher
        assertTrue(connectionThread.isAlive());
    }

    @Test
    @DisplayName("Test message avec tabulations")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMessageWithTabs() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        clientOut.println("Player:\t100,\t200");
        Thread.sleep(300);

        assertTrue(connectionThread.isAlive());
    }

    // ==================== Tests de stabilité ====================

    @Test
    @DisplayName("Test stabilité après messages valides et invalides mélangés")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testStabilityMixedMessages() throws InterruptedException {
        Thread connectionThread = new Thread(gestionConnection);
        connectionThread.setDaemon(true);
        connectionThread.start();

        clientOut.println("Player1:100,200");
        clientOut.println("invalid");
        clientOut.println("Player2:300,400");
        clientOut.println(":no_player");
        clientOut.println("Player3:500,600");

        Thread.sleep(500);

        var positions = mockNode.getPlayerPositions();
        assertTrue(positions.containsKey("Player1"));
        assertTrue(positions.containsKey("Player2"));
        assertTrue(positions.containsKey("Player3"));
        assertTrue(connectionThread.isAlive());
    }

    // ==================== Tests de maillage complet à 3 joueurs ====================

    @Test
    @DisplayName("Test maillage complet à 3 joueurs - tous les joueurs se voient")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testFullMeshThreePlayers() throws InterruptedException, IOException {
        // Créer 3 serveurs (nœuds P2P) simulant 3 joueurs
        int portJ1 = testPort + 200;
        int portJ2 = testPort + 201;
        int portJ3 = testPort + 202;

        Serveur serveurJ1 = new Serveur("J1", "localhost", portJ1);
        Serveur serveurJ2 = new Serveur("J2", "localhost", portJ2);
        Serveur serveurJ3 = new Serveur("J3", "localhost", portJ3);

        try {
            // Démarrer les 3 serveurs
            serveurJ1.start();
            serveurJ2.start();
            serveurJ3.start();

            Thread.sleep(200); // Attendre que les serveurs démarrent

            // J2 se connecte à J1 (l'hôte)
            serveurJ2.connectToNode("J1", "localhost", portJ1);
            Thread.sleep(300);

            // J3 se connecte à J1 (l'hôte)
            serveurJ3.connectToNode("J1", "localhost", portJ1);
            Thread.sleep(500); // Attendre la propagation de la PEER_LIST

            // Chaque joueur envoie sa position
            serveurJ1.movePlayer(100, 100);
            serveurJ2.movePlayer(200, 200);
            serveurJ3.movePlayer(300, 300);

            Thread.sleep(500); // Attendre la propagation des positions

            // Vérifier que J1 (l'hôte) voit J2 et J3
            Map<String, int[]> positionsJ1 = serveurJ1.getPlayerPositions();
            assertTrue(positionsJ1.containsKey("J1"), "J1 doit avoir sa propre position");
            assertTrue(positionsJ1.containsKey("J2"), "J1 doit voir J2");
            assertTrue(positionsJ1.containsKey("J3"), "J1 doit voir J3");

            // Vérifier que J2 voit J1 et J3 (LE BUG POTENTIEL)
            Map<String, int[]> positionsJ2 = serveurJ2.getPlayerPositions();
            assertTrue(positionsJ2.containsKey("J2"), "J2 doit avoir sa propre position");
            assertTrue(positionsJ2.containsKey("J1"), "J2 doit voir J1 (l'hôte)");
            assertTrue(positionsJ2.containsKey("J3"), "J2 doit voir J3 (BUG si échoue: J2 ne voit que l'hôte)");

            // Vérifier que J3 voit J1 et J2 (LE BUG POTENTIEL)
            Map<String, int[]> positionsJ3 = serveurJ3.getPlayerPositions();
            assertTrue(positionsJ3.containsKey("J3"), "J3 doit avoir sa propre position");
            assertTrue(positionsJ3.containsKey("J1"), "J3 doit voir J1 (l'hôte)");
            assertTrue(positionsJ3.containsKey("J2"), "J3 doit voir J2 (BUG si échoue: J3 ne voit que l'hôte)");

        } finally {
            serveurJ1.shutdown();
            serveurJ2.shutdown();
            serveurJ3.shutdown();
        }
    }

    @Test
    @DisplayName("Test maillage complet - vérification des connexions bidirectionnelles")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testFullMeshBidirectionalConnections() throws InterruptedException {
        int portJ1 = testPort + 210;
        int portJ2 = testPort + 211;
        int portJ3 = testPort + 212;

        Serveur serveurJ1 = new Serveur("J1", "localhost", portJ1);
        Serveur serveurJ2 = new Serveur("J2", "localhost", portJ2);
        Serveur serveurJ3 = new Serveur("J3", "localhost", portJ3);

        try {
            serveurJ1.start();
            serveurJ2.start();
            serveurJ3.start();
            Thread.sleep(200);

            // J2 et J3 se connectent à J1
            serveurJ2.connectToNode("J1", "localhost", portJ1);
            serveurJ3.connectToNode("J1", "localhost", portJ1);
            Thread.sleep(1000); // Attendre que le maillage complet se forme

            // Vérifier le nombre de connexions de chaque nœud
            // En maillage complet avec 3 nœuds, chaque nœud doit avoir 2 connexions
            int connectionsJ1 = serveurJ1.getConnectedPeersList().size();
            int connectionsJ2 = serveurJ2.getConnectedPeersList().size();
            int connectionsJ3 = serveurJ3.getConnectedPeersList().size();

            assertEquals(2, connectionsJ1, "J1 (hôte) doit avoir 2 connexions (J2 et J3)");
            assertEquals(2, connectionsJ2, "J2 doit avoir 2 connexions (J1 et J3) - BUG si seulement 1");
            assertEquals(2, connectionsJ3, "J3 doit avoir 2 connexions (J1 et J2) - BUG si seulement 1");

        } finally {
            serveurJ1.shutdown();
            serveurJ2.shutdown();
            serveurJ3.shutdown();
        }
    }

    @Test
    @DisplayName("Test race condition - connexions rapides successives")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testRaceConditionRapidConnections() throws InterruptedException {
        int portJ1 = testPort + 220;
        int portJ2 = testPort + 221;
        int portJ3 = testPort + 222;

        Serveur serveurJ1 = new Serveur("J1", "localhost", portJ1);
        Serveur serveurJ2 = new Serveur("J2", "localhost", portJ2);
        Serveur serveurJ3 = new Serveur("J3", "localhost", portJ3);

        try {
            serveurJ1.start();
            serveurJ2.start();
            serveurJ3.start();
            Thread.sleep(100);

            // Connexions quasi-simultanées (simule le bug observé)
            serveurJ2.connectToNode("J1", "localhost", portJ1);
            serveurJ3.connectToNode("J1", "localhost", portJ1);

            // Attendre un peu plus pour la propagation
            Thread.sleep(1500);

            // Envoyer des positions depuis tous les joueurs
            for (int i = 0; i < 5; i++) {
                serveurJ1.movePlayer(100 + i, 100 + i);
                serveurJ2.movePlayer(200 + i, 200 + i);
                serveurJ3.movePlayer(300 + i, 300 + i);
                Thread.sleep(100);
            }

            Thread.sleep(500);

            // Vérifier que tout le monde voit tout le monde
            Map<String, int[]> posJ1 = serveurJ1.getPlayerPositions();
            Map<String, int[]> posJ2 = serveurJ2.getPlayerPositions();
            Map<String, int[]> posJ3 = serveurJ3.getPlayerPositions();

            // J1 doit voir 3 joueurs
            assertEquals(3, posJ1.size(), "J1 doit voir 3 joueurs (lui-même, J2, J3)");

            // J2 doit voir 3 joueurs (c'est ici que le bug se manifeste)
            assertEquals(3, posJ2.size(),
                    "J2 doit voir 3 joueurs - BUG CONFIRMÉ si seulement " + posJ2.size() +
                            " joueurs visibles: " + posJ2.keySet());

            // J3 doit voir 3 joueurs
            assertEquals(3, posJ3.size(),
                    "J3 doit voir 3 joueurs - BUG CONFIRMÉ si seulement " + posJ3.size() +
                            " joueurs visibles: " + posJ3.keySet());

        } finally {
            serveurJ1.shutdown();
            serveurJ2.shutdown();
            serveurJ3.shutdown();
        }
    }

    @Test
    @DisplayName("Test PEER_LIST est bien envoyé aux nouveaux connectés")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPeerListSentToNewPeers() throws InterruptedException {
        int portJ1 = testPort + 230;
        int portJ2 = testPort + 231;
        int portJ3 = testPort + 232;

        Serveur serveurJ1 = new Serveur("J1", "localhost", portJ1);
        Serveur serveurJ2 = new Serveur("J2", "localhost", portJ2);
        Serveur serveurJ3 = new Serveur("J3", "localhost", portJ3);

        try {
            serveurJ1.start();
            Thread.sleep(100);

            // J2 se connecte d'abord
            serveurJ2.start();
            serveurJ2.connectToNode("J1", "localhost", portJ1);
            Thread.sleep(500); // J2 est maintenant dans la PEER_LIST de J1

            // J3 se connecte après - il devrait recevoir J2 dans la PEER_LIST
            serveurJ3.start();
            serveurJ3.connectToNode("J1", "localhost", portJ1);
            Thread.sleep(1000); // Attendre la propagation

            // Envoyer des positions
            serveurJ1.movePlayer(100, 100);
            serveurJ2.movePlayer(200, 200);
            serveurJ3.movePlayer(300, 300);
            Thread.sleep(500);

            // J3 doit avoir reçu la PEER_LIST contenant J2 et s'y être connecté
            Map<String, int[]> posJ3 = serveurJ3.getPlayerPositions();
            assertTrue(posJ3.containsKey("J2"),
                    "J3 doit voir J2 grâce à la PEER_LIST reçue de J1. " +
                            "Joueurs visibles par J3: " + posJ3.keySet());

        } finally {
            serveurJ1.shutdown();
            serveurJ2.shutdown();
            serveurJ3.shutdown();
        }
    }
}
