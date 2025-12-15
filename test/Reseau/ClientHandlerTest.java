package Reseau;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe ClientHandler
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class ClientHandlerTest {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Socket serverSideSocket;
    private ClientHandler clientHandler;
    private ServerGame mockServer;
    private static int testPort = 9001;

    private PrintWriter clientOut;
    private BufferedReader clientIn;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        testPort += 10;
        mockServer = new ServerGame(testPort + 100); // Port différent pour le mock

        // Créer un serveur temporaire pour les tests
        serverSocket = new ServerSocket(testPort);
        serverSocket.setSoTimeout(5000);

        CountDownLatch connectionLatch = new CountDownLatch(1);

        // Thread pour accepter la connexion
        Thread serverThread = new Thread(() -> {
            try {
                serverSideSocket = serverSocket.accept();
                clientHandler = new ClientHandler(serverSideSocket, mockServer);
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
    @DisplayName("Test création du ClientHandler")
    void testClientHandlerCreation() {
        assertNotNull(clientHandler, "Le ClientHandler doit être créé");
    }

    // ==================== Tests de communication ====================

    @Test
    @DisplayName("Test envoi de message au client")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendMessage() throws IOException {
        String testMessage = "Hello Client!";

        clientHandler.sendMessage(testMessage);

        String received = clientIn.readLine();
        assertEquals(testMessage, received, "Le client doit recevoir le message envoyé");
    }

    @Test
    @DisplayName("Test envoi de plusieurs messages")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendMultipleMessages() throws IOException {
        clientHandler.sendMessage("Message1");
        clientHandler.sendMessage("Message2");
        clientHandler.sendMessage("Message3");

        assertEquals("Message1", clientIn.readLine());
        assertEquals("Message2", clientIn.readLine());
        assertEquals("Message3", clientIn.readLine());
    }

    // ==================== Tests de run (traitement des messages) ====================

    @Test
    @DisplayName("Test exécution du handler avec ID joueur")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testRunWithPlayerId() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        // Envoyer l'ID du joueur
        clientOut.println("TestPlayer");

        // Attendre la réponse
        String response = clientIn.readLine();
        assertTrue(response.contains("CONNECTED"), "Le serveur doit confirmer la connexion");

        // Vérifier l'ID du joueur
        Thread.sleep(100);
        assertEquals("TestPlayer", clientHandler.getPlayerId());
    }

    @Test
    @DisplayName("Test traitement des coordonnées")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testProcessCoordinates() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        // Envoyer l'ID puis les coordonnées
        clientOut.println("TestPlayer");
        clientIn.readLine(); // Lire la réponse CONNECTED

        clientOut.println("100,200");
        Thread.sleep(200);

        assertEquals(100, clientHandler.getPosX(), "Position X doit être mise à jour");
        assertEquals(200, clientHandler.getPosY(), "Position Y doit être mise à jour");
    }

    @Test
    @DisplayName("Test traitement de coordonnées multiples")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testProcessMultipleCoordinates() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println("50,75");
        Thread.sleep(100);
        assertEquals(50, clientHandler.getPosX());
        assertEquals(75, clientHandler.getPosY());

        clientOut.println("200,300");
        Thread.sleep(100);
        assertEquals(200, clientHandler.getPosX());
        assertEquals(300, clientHandler.getPosY());
    }

    // ==================== Tests des getters ====================

    @Test
    @DisplayName("Test getPlayerId avant initialisation")
    void testGetPlayerIdBeforeInit() {
        assertNull(clientHandler.getPlayerId(), "PlayerId doit être null avant initialisation");
    }

    @Test
    @DisplayName("Test getPosX initial")
    void testGetPosXInitial() {
        assertEquals(0, clientHandler.getPosX(), "Position X initiale doit être 0");
    }

    @Test
    @DisplayName("Test getPosY initial")
    void testGetPosYInitial() {
        assertEquals(0, clientHandler.getPosY(), "Position Y initiale doit être 0");
    }

    // ==================== Tests de parsing d'action invalide ====================

    @Test
    @DisplayName("Test parsing d'action invalide ne crash pas")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testInvalidActionParsing() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        // Envoyer une action invalide
        clientOut.println("invalid_action");
        Thread.sleep(200);

        // Les positions doivent rester à 0
        assertEquals(0, clientHandler.getPosX());
        assertEquals(0, clientHandler.getPosY());
    }

    @Test
    @DisplayName("Test action avec format incorrect")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testMalformedAction() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        // Envoyer des actions mal formées
        clientOut.println("abc,def");
        clientOut.println("100");
        clientOut.println(",200");

        Thread.sleep(200);

        // Pas de crash, positions restent à 0
        assertEquals(0, clientHandler.getPosX());
        assertEquals(0, clientHandler.getPosY());
    }

    // ==================== Tests de coordonnées spéciales ====================

    @Test
    @DisplayName("Test coordonnées négatives")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testNegativeCoordinates() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println("-100,-200");
        Thread.sleep(200);

        assertEquals(-100, clientHandler.getPosX());
        assertEquals(-200, clientHandler.getPosY());
    }

    @Test
    @DisplayName("Test coordonnées zéro")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testZeroCoordinates() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println("100,200");
        Thread.sleep(100);

        clientOut.println("0,0");
        Thread.sleep(200);

        assertEquals(0, clientHandler.getPosX());
        assertEquals(0, clientHandler.getPosY());
    }

    @Test
    @DisplayName("Test grandes coordonnées")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testLargeCoordinates() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println("999999,888888");
        Thread.sleep(200);

        assertEquals(999999, clientHandler.getPosX());
        assertEquals(888888, clientHandler.getPosY());
    }

    // ==================== Tests de robustesse ====================

    @Test
    @DisplayName("Test message vide")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testEmptyMessage() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println("");
        Thread.sleep(200);

        assertTrue(handlerThread.isAlive());
        assertEquals(0, clientHandler.getPosX());
        assertEquals(0, clientHandler.getPosY());
    }

    @Test
    @DisplayName("Test messages rapides successifs")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRapidMessages() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        for (int i = 0; i < 50; i++) {
            clientOut.println(i + "," + (i * 2));
        }
        Thread.sleep(500);

        // Dernière position
        assertEquals(49, clientHandler.getPosX());
        assertEquals(98, clientHandler.getPosY());
    }

    @Test
    @DisplayName("Test message avec espaces")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testMessageWithSpaces() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println(" 100 , 200 ");
        Thread.sleep(200);

        // Le handler doit continuer sans crash
        assertTrue(handlerThread.isAlive());
    }

    @Test
    @DisplayName("Test action avec caractères spéciaux")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSpecialCharacters() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println("!@#$%^&*()");
        Thread.sleep(200);

        assertTrue(handlerThread.isAlive());
        assertEquals(0, clientHandler.getPosX());
        assertEquals(0, clientHandler.getPosY());
    }

    @Test
    @DisplayName("Test mélange messages valides et invalides")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMixedValidInvalidMessages() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println("100,200");
        Thread.sleep(100);
        assertEquals(100, clientHandler.getPosX());
        assertEquals(200, clientHandler.getPosY());

        clientOut.println("invalid");
        Thread.sleep(100);
        // Position inchangée
        assertEquals(100, clientHandler.getPosX());
        assertEquals(200, clientHandler.getPosY());

        clientOut.println("300,400");
        Thread.sleep(100);
        assertEquals(300, clientHandler.getPosX());
        assertEquals(400, clientHandler.getPosY());
    }


    // ==================== Tests de sendMessage ====================

    @Test
    @DisplayName("Test envoi de message long")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendLongMessage() throws IOException {
        String longMessage = "A".repeat(10000);
        clientHandler.sendMessage(longMessage);

        String received = clientIn.readLine();
        assertEquals(longMessage, received);
    }

    @Test
    @DisplayName("Test envoi de message vide")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendEmptyMessage() throws IOException {
        clientHandler.sendMessage("");

        String received = clientIn.readLine();
        assertEquals("", received);
    }

    @Test
    @DisplayName("Test envoi de message avec unicode")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testSendUnicodeMessage() throws IOException {
        String unicodeMessage = "Position: éàü 日本語 🎮";
        clientHandler.sendMessage(unicodeMessage);

        String received = clientIn.readLine();
        assertEquals(unicodeMessage, received);
    }

    // ==================== Tests de stabilité avancés ====================

    @Test
    @DisplayName("Test handler stable après nombreuses opérations")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testStabilityAfterManyOperations() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("StabilityTestPlayer");
        clientIn.readLine();

        // Envoyer 100 messages variés
        for (int i = 0; i < 100; i++) {
            if (i % 3 == 0) {
                clientOut.println(i + "," + i);
            } else if (i % 3 == 1) {
                clientOut.println("invalid" + i);
            } else {
                clientOut.println(""); // Message vide
            }
        }
        Thread.sleep(500);

        assertTrue(handlerThread.isAlive(), "Le handler doit rester stable");
    }

    @Test
    @DisplayName("Test ID joueur avec caractères spéciaux")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testPlayerIdWithSpecialChars() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("Player-1_Test@123");
        String response = clientIn.readLine();
        Thread.sleep(100);

        assertEquals("Player-1_Test@123", clientHandler.getPlayerId());
        assertTrue(response.contains("CONNECTED"));
    }

    @Test
    @DisplayName("Test coordonnées avec Integer.MAX_VALUE")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testMaxIntCoordinates() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println(Integer.MAX_VALUE + "," + Integer.MAX_VALUE);
        Thread.sleep(200);

        assertEquals(Integer.MAX_VALUE, clientHandler.getPosX());
        assertEquals(Integer.MAX_VALUE, clientHandler.getPosY());
    }

    @Test
    @DisplayName("Test coordonnées avec Integer.MIN_VALUE")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testMinIntCoordinates() throws InterruptedException, IOException {
        Thread handlerThread = new Thread(clientHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();

        clientOut.println("TestPlayer");
        clientIn.readLine();

        clientOut.println(Integer.MIN_VALUE + "," + Integer.MIN_VALUE);
        Thread.sleep(200);

        assertEquals(Integer.MIN_VALUE, clientHandler.getPosX());
        assertEquals(Integer.MIN_VALUE, clientHandler.getPosY());
    }
}

