package Reseau;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests unitaires pour la classe ClientGame
 *
 * Teste la connexion, l'envoi de coordonnées et la réception de messages
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class ClientGameTest {
    private ServerSocket mockServerSocket;
    private Socket mockSocket;
    private ClientGame clientGame;
    private int testPort;
    private Thread serverThread;
    private PrintWriter serverOut;
    private BufferedReader serverIn;
    private CountDownLatch serverReadyLatch;
    private volatile boolean serverReady = false;

    @BeforeEach
    public void setUp() throws IOException {
        // Réinitialiser les variables pour chaque test
        serverReadyLatch = new CountDownLatch(1);
        serverReady = false;
        mockSocket = null;
        serverOut = null;
        serverIn = null;

        // Créer un serveur mock pour tester le client avec un port dynamique (0 = port libre)
        mockServerSocket = new ServerSocket(0);
        testPort = mockServerSocket.getLocalPort(); // Récupérer le port attribué
        mockServerSocket.setSoTimeout(5000); // Timeout de 5 secondes

        // Lancer le serveur mock dans un thread
        serverThread = new Thread(() -> {
            try {
                mockSocket = mockServerSocket.accept();
                serverOut = new PrintWriter(mockSocket.getOutputStream(), true);
                serverIn = new BufferedReader(new InputStreamReader(mockSocket.getInputStream()));

                // Lire l'ID du client
                String clientId = serverIn.readLine();

                // IMPORTANT: Envoyer une réponse immédiatement pour que le constructeur du client ne se bloque pas
                serverOut.println("CONNECTED:" + clientId);

                serverReady = true;
                serverReadyLatch.countDown();
            } catch (IOException e) {
                System.err.println("Erreur serveur mock: " + e.getMessage());
                serverReadyLatch.countDown(); // Décompter même en cas d'erreur pour ne pas bloquer les tests
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterEach
    public void tearDown() {
        // Déconnecter le client
        if (clientGame != null) {
            try {
                clientGame.disconnect();
            } catch (Exception e) {
                // Ignorer les erreurs de déconnexion
            }
        }

        // Fermer les streams du serveur mock
        if (serverIn != null) {
            try {
                serverIn.close();
            } catch (Exception e) {
                // Ignorer
            }
        }
        if (serverOut != null) {
            try {
                serverOut.close();
            } catch (Exception e) {
                // Ignorer
            }
        }

        // Fermer le socket client du serveur mock
        if (mockSocket != null && !mockSocket.isClosed()) {
            try {
                mockSocket.close();
            } catch (Exception e) {
                // Ignorer
            }
        }

        // Fermer le serveur socket
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            try {
                mockServerSocket.close();
            } catch (Exception e) {
                // Ignorer
            }
        }

        // Attendre que le thread serveur se termine
        if (serverThread != null && serverThread.isAlive()) {
            try {
                serverThread.join(1000); // Attendre max 1 seconde
            } catch (InterruptedException e) {
                // Ignorer
            }
        }

        // Petite pause pour nettoyer les ressources réseau
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // Ignorer
        }
    }


    /**
     * Test 1: Vérifier que le client se connecte au serveur
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testConnectionToServer() throws InterruptedException {
        // Créer un client et se connecter
        clientGame = new ClientGame("localhost", testPort, "Joueur1");

        // Attendre que le serveur accepte la connexion
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS), "Le serveur devrait accepter la connexion");

        // Vérifier que le client est bien créé
        assertNotNull(clientGame);
        assertEquals("Joueur1", clientGame.getPlayerId());
        assertEquals(0, clientGame.getPosX());
        assertEquals(0, clientGame.getPosY());
    }

    /**
     * Test 2: Vérifier que movePlayer met à jour les coordonnées locales
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMovePlayerUpdatesPosition() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        // Déplacer le joueur
        clientGame.movePlayer(100, 200);

        // Vérifier les coordonnées mises à jour localement
        assertEquals(100, clientGame.getPosX());
        assertEquals(200, clientGame.getPosY());
    }

    /**
     * Test 3: Vérifier que movePlayer envoie les coordonnées au serveur
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMovePlayerSendsToServer() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));


        // Déplacer le joueur
        clientGame.movePlayer(150, 250);
        Thread.sleep(200);

        // Vérifier que le serveur a reçu les coordonnées
        String message = serverIn.readLine();
        assertNotNull(message);
        assertEquals("150,250", message);
    }

    /**
     * Test 4: Vérifier sendAction
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testSendAction() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));


        // Envoyer une action
        clientGame.sendAction("SHOOT:300,150");
        Thread.sleep(200);

        // Vérifier que le serveur a reçu l'action
        String message = serverIn.readLine();
        assertNotNull(message);
        assertEquals("SHOOT:300,150", message);
    }

    /**
     * Test 5: Vérifier que getPlayerId retourne l'ID correct
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testGetPlayerId() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "TestPlayer");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        assertEquals("TestPlayer", clientGame.getPlayerId());
    }

    /**
     * Test 6: Vérifier que les coordonnées X et Y sont correctes
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testGetPositionCoordinates() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.movePlayer(500, 600);

        assertEquals(500, clientGame.getPosX());
        assertEquals(600, clientGame.getPosY());
    }

    /**
     * Test 7: Vérifier plusieurs mouvements successifs
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMultipleMoves() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));


        // Premier mouvement
        clientGame.movePlayer(100, 200);
        Thread.sleep(100);
        String msg1 = serverIn.readLine();
        assertEquals("100,200", msg1);

        // Deuxième mouvement
        clientGame.movePlayer(300, 400);
        Thread.sleep(100);
        String msg2 = serverIn.readLine();
        assertEquals("300,400", msg2);

        // Troisième mouvement
        clientGame.movePlayer(500, 600);
        Thread.sleep(100);
        String msg3 = serverIn.readLine();
        assertEquals("500,600", msg3);

        // Vérifier la position finale
        assertEquals(500, clientGame.getPosX());
        assertEquals(600, clientGame.getPosY());
    }

    /**
     * Test 8: Vérifier que les messages reçus sont traités
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testReceiveMessageFromServer() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        Thread.sleep(200);

        // Envoyer un message du serveur au client
        serverOut.println("UPDATE_GAME_STATE");
        Thread.sleep(200);

        // Vérifier que le client est toujours connecté
        assertNotNull(clientGame);
        assertEquals("Joueur1", clientGame.getPlayerId());
    }

    /**
     * Test 9: Vérifier les valeurs initiales
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testInitialValues() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "InitTest");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        assertEquals(0, clientGame.getPosX());
        assertEquals(0, clientGame.getPosY());
        assertEquals("InitTest", clientGame.getPlayerId());
    }

    /**
     * Test 10: Vérifier que disconnect ferme la connexion
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testDisconnect() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        assertNotNull(clientGame);

        // Déconnecter le client - ne devrait pas lancer d'exception
        assertDoesNotThrow(() -> clientGame.disconnect());
        Thread.sleep(200);

        // Le client devrait être toujours accessible même après déconnexion
        assertNotNull(clientGame);
        assertEquals("Joueur1", clientGame.getPlayerId());
    }

    /**
     * Test 11: Vérifier le déplacement avec valeurs négatives
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMovePlayerWithNegativeCoordinates() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.movePlayer(-100, -200);

        assertEquals(-100, clientGame.getPosX());
        assertEquals(-200, clientGame.getPosY());
    }

    /**
     * Test 12: Vérifier le déplacement avec coordonnées zéro
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMovePlayerToZero() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.movePlayer(500, 500);
        Thread.sleep(100);

        clientGame.movePlayer(0, 0);

        assertEquals(0, clientGame.getPosX());
        assertEquals(0, clientGame.getPosY());
    }

    /**
     * Test 13: Vérifier le déplacement avec grandes coordonnées
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMovePlayerWithLargeCoordinates() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.movePlayer(10000, 20000);

        assertEquals(10000, clientGame.getPosX());
        assertEquals(20000, clientGame.getPosY());
    }

    /**
     * Test 14: Vérifier plusieurs actions rapides
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testRapidActions() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));


        for (int i = 0; i < 10; i++) {
            clientGame.movePlayer(i * 10, i * 20);
            Thread.sleep(50);
        }

        assertEquals(90, clientGame.getPosX());
        assertEquals(180, clientGame.getPosY());
    }

    /**
     * Test 15: Vérifier la stabilité après de nombreuses opérations
     */
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    public void testStabilityAfterManyOperations() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));


        // Effectuer 50 mouvements (réduit pour éviter les timeouts)
        for (int i = 0; i < 50; i++) {
            clientGame.movePlayer(i, i);
            Thread.sleep(10);
        }

        // Vérifier la dernière position
        assertEquals(49, clientGame.getPosX());
        assertEquals(49, clientGame.getPosY());
    }

    // ==================== Tests supplémentaires ====================

    /**
     * Test 16: Vérifier sendAction avec message vide
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testSendEmptyAction() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.sendAction("");
        Thread.sleep(200);

        String message = serverIn.readLine();
        assertEquals("", message);
    }

    /**
     * Test 17: Vérifier sendAction avec caractères spéciaux
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testSendActionWithSpecialCharacters() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.sendAction("ACTION:!@#$%^&*()");
        Thread.sleep(200);

        String message = serverIn.readLine();
        assertEquals("ACTION:!@#$%^&*()", message);
    }

    /**
     * Test 18: Vérifier sendAction avec unicode
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testSendActionWithUnicode() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.sendAction("CHAT:Bonjour 日本語 🎮");
        Thread.sleep(200);

        String message = serverIn.readLine();
        assertEquals("CHAT:Bonjour 日本語 🎮", message);
    }

    /**
     * Test 19: Vérifier plusieurs actions rapides
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMultipleRapidActions() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.sendAction("ACTION1");
        clientGame.sendAction("ACTION2");
        clientGame.sendAction("ACTION3");
        Thread.sleep(200);

        assertEquals("ACTION1", serverIn.readLine());
        assertEquals("ACTION2", serverIn.readLine());
        assertEquals("ACTION3", serverIn.readLine());
    }

    /**
     * Test 20: Vérifier movePlayer avec coordonnées Integer.MAX_VALUE
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMovePlayerMaxInt() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.movePlayer(Integer.MAX_VALUE, Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, clientGame.getPosX());
        assertEquals(Integer.MAX_VALUE, clientGame.getPosY());
    }

    /**
     * Test 21: Vérifier movePlayer avec coordonnées Integer.MIN_VALUE
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMovePlayerMinInt() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.movePlayer(Integer.MIN_VALUE, Integer.MIN_VALUE);

        assertEquals(Integer.MIN_VALUE, clientGame.getPosX());
        assertEquals(Integer.MIN_VALUE, clientGame.getPosY());
    }

    /**
     * Test 22: Vérifier l'ID du joueur avec caractères spéciaux
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testPlayerIdWithSpecialChars() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Player-1_Test@123");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        assertEquals("Player-1_Test@123", clientGame.getPlayerId());
    }

    /**
     * Test 23: Vérifier l'ID du joueur avec unicode
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testPlayerIdWithUnicode() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur日本語");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        assertEquals("Joueur日本語", clientGame.getPlayerId());
    }

    /**
     * Test 24: Vérifier sendAction avec message très long
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testSendLongAction() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        String longAction = "A".repeat(5000);
        clientGame.sendAction(longAction);
        Thread.sleep(200);

        String message = serverIn.readLine();
        assertEquals(longAction, message);
    }

    /**
     * Test 25: Vérifier movePlayer envoie le bon format
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMovePlayerSendsCorrectFormat() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.movePlayer(-50, 75);
        Thread.sleep(200);

        String message = serverIn.readLine();
        assertEquals("-50,75", message);
    }

    /**
     * Test 26: Vérifier que disconnect peut être appelé plusieurs fois
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMultipleDisconnect() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        assertDoesNotThrow(() -> clientGame.disconnect());
        assertDoesNotThrow(() -> clientGame.disconnect());
        assertDoesNotThrow(() -> clientGame.disconnect());
    }

    /**
     * Test 27: Vérifier la stabilité avec différents types de messages
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMixedMessagesStability() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.movePlayer(100, 200);
        clientGame.sendAction("SHOOT");
        clientGame.movePlayer(150, 250);
        clientGame.sendAction("JUMP");
        clientGame.movePlayer(200, 300);

        Thread.sleep(300);

        assertEquals(200, clientGame.getPosX());
        assertEquals(300, clientGame.getPosY());
    }

    /**
     * Test 28: Vérifier les coordonnées après reset à zéro
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testResetPositionToZero() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        clientGame.movePlayer(1000, 2000);
        assertEquals(1000, clientGame.getPosX());
        assertEquals(2000, clientGame.getPosY());

        clientGame.movePlayer(0, 0);
        assertEquals(0, clientGame.getPosX());
        assertEquals(0, clientGame.getPosY());
    }

    /**
     * Test 29: Vérifier le comportement après réception de message serveur
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testBehaviorAfterServerMessage() throws InterruptedException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        // Envoyer plusieurs messages du serveur
        serverOut.println("OtherPlayer:100,200");
        serverOut.println("OtherPlayer:150,250");
        Thread.sleep(300);

        // Le client doit toujours fonctionner
        clientGame.movePlayer(500, 600);
        assertEquals(500, clientGame.getPosX());
        assertEquals(600, clientGame.getPosY());
    }

    /**
     * Test 30: Vérifier l'envoi de coordonnées après plusieurs mouvements
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testCoordinatesAfterManyMoves() throws InterruptedException, IOException {
        clientGame = new ClientGame("localhost", testPort, "Joueur1");
        assertTrue(serverReadyLatch.await(3, TimeUnit.SECONDS));

        // Simuler un mouvement continu
        for (int i = 0; i < 10; i++) {
            clientGame.movePlayer(i * 10, i * 10);
            Thread.sleep(20);
        }

        // Vérifier la position finale
        assertEquals(90, clientGame.getPosX());
        assertEquals(90, clientGame.getPosY());

        // Vérifier que tous les messages ont été envoyés
        for (int i = 0; i < 10; i++) {
            String msg = serverIn.readLine();
            assertNotNull(msg);
        }
    }
}

