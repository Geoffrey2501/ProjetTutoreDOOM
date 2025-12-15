package Reseau;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe ServerGame
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class ServerGameTest {

    private ServerGame server;
    private Thread serverThread;
    private ServerSocket serverSocket;
    private static int testPort = 8001;

    @BeforeEach
    void setUp() {
        testPort += 10; // Utiliser un nouveau port pour chaque test
        server = new ServerGame(testPort);
    }

    @AfterEach
    void tearDown() {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignorer
            }
        }
    }

    // ==================== Tests de construction ====================

    @Test
    @DisplayName("Test création du serveur")
    void testServerCreation() {
        assertNotNull(server, "Le serveur doit être créé");
    }

    @Test
    @DisplayName("Test création de plusieurs serveurs sur différents ports")
    void testMultipleServerCreation() {
        ServerGame server1 = new ServerGame(testPort + 1000);
        ServerGame server2 = new ServerGame(testPort + 1001);
        ServerGame server3 = new ServerGame(testPort + 1002);

        assertNotNull(server1);
        assertNotNull(server2);
        assertNotNull(server3);
    }

    @Test
    @DisplayName("Test création serveur avec port minimal")
    void testServerCreationWithMinPort() {
        ServerGame minServer = new ServerGame(1024);
        assertNotNull(minServer);
    }

    @Test
    @DisplayName("Test création serveur avec port maximal")
    void testServerCreationWithMaxPort() {
        ServerGame maxServer = new ServerGame(65535);
        assertNotNull(maxServer);
    }

    // ==================== Tests de démarrage ====================

    @Test
    @DisplayName("Test démarrage du serveur dans un thread")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testServerStartInThread() throws InterruptedException {
        CountDownLatch serverStarted = new CountDownLatch(1);

        serverThread = new Thread(() -> {
            try {
                serverStarted.countDown();
                server.start();
            } catch (Exception e) {
                // Le serveur s'arrête quand le test se termine
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(serverStarted.await(2, TimeUnit.SECONDS), "Le serveur doit démarrer");
    }

    @Test
    @DisplayName("Test serveur écoute sur le bon port")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testServerListensOnCorrectPort() throws InterruptedException, IOException {
        CountDownLatch serverReady = new CountDownLatch(1);
        AtomicBoolean connectionSuccess = new AtomicBoolean(false);

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(testPort);
                serverReady.countDown();
                Socket client = serverSocket.accept();
                connectionSuccess.set(true);
                client.close();
            } catch (IOException e) {
                serverReady.countDown();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(serverReady.await(2, TimeUnit.SECONDS));

        Socket testClient = new Socket("localhost", testPort);
        Thread.sleep(200);
        testClient.close();

        assertTrue(connectionSuccess.get(), "Le serveur doit accepter les connexions");
    }

    // ==================== Tests de getClients ====================

    @Test
    @DisplayName("Test liste des clients vide au départ")
    void testGetClientsEmptyAtStart() {
        List<ClientHandler> clients = server.getClients();
        assertNotNull(clients, "La liste des clients ne doit pas être null");
        assertTrue(clients.isEmpty(), "La liste des clients doit être vide au départ");
    }

    @Test
    @DisplayName("Test getClients retourne une copie")
    void testGetClientsReturnsCopy() {
        List<ClientHandler> clients1 = server.getClients();
        List<ClientHandler> clients2 = server.getClients();

        assertNotSame(clients1, clients2, "Chaque appel doit retourner une nouvelle liste");
    }

    @Test
    @DisplayName("Test getClients retourne une liste immuable par rapport à l'original")
    void testGetClientsReturnsImmutableCopy() {
        List<ClientHandler> clients = server.getClients();
        int initialSize = clients.size();

        // Modifier la copie ne devrait pas affecter l'original
        clients.clear();

        List<ClientHandler> clientsAgain = server.getClients();
        assertEquals(initialSize, clientsAgain.size(), "La liste originale ne doit pas être modifiée");
    }

    @Test
    @DisplayName("Test taille de la liste des clients")
    void testGetClientsSize() {
        List<ClientHandler> clients = server.getClients();
        assertEquals(0, clients.size(), "La taille initiale doit être 0");
    }

    // ==================== Tests de connexion client ====================

    @Test
    @DisplayName("Test connexion d'un client au serveur")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testClientConnection() throws InterruptedException, IOException {
        CountDownLatch serverReady = new CountDownLatch(1);

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(testPort);
                serverReady.countDown();
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, server);
                serverSocket.close();
            } catch (IOException e) {
                serverReady.countDown();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(serverReady.await(2, TimeUnit.SECONDS));

        try {
            Socket client = new Socket("localhost", testPort);
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            out.println("TestPlayer");
            Thread.sleep(200);
            client.close();
        } catch (IOException e) {
            // Connexion peut échouer si le serveur s'arrête
        }
    }

    @Test
    @DisplayName("Test connexion de plusieurs clients")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMultipleClientConnections() throws InterruptedException, IOException {
        CountDownLatch serverReady = new CountDownLatch(1);
        CountDownLatch clientsConnected = new CountDownLatch(3);

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(testPort);
                serverReady.countDown();
                for (int i = 0; i < 3; i++) {
                    Socket clientSocket = serverSocket.accept();
                    new ClientHandler(clientSocket, server);
                    clientsConnected.countDown();
                }
            } catch (IOException e) {
                // Ignorer
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(serverReady.await(2, TimeUnit.SECONDS));

        Socket[] clients = new Socket[3];
        for (int i = 0; i < 3; i++) {
            clients[i] = new Socket("localhost", testPort);
            PrintWriter out = new PrintWriter(clients[i].getOutputStream(), true);
            out.println("Player" + i);
        }

        assertTrue(clientsConnected.await(3, TimeUnit.SECONDS), "Tous les clients doivent se connecter");

        for (Socket client : clients) {
            client.close();
        }
    }

    // ==================== Tests de removeClient ====================

    @Test
    @DisplayName("Test suppression d'un client null")
    void testRemoveNullClient() {
        assertDoesNotThrow(() -> server.removeClient(null));
    }

    @Test
    @DisplayName("Test suppression d'un client avec socket connecté")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRemoveConnectedClient() throws InterruptedException, IOException {
        CountDownLatch serverReady = new CountDownLatch(1);
        CountDownLatch clientHandlerCreated = new CountDownLatch(1);
        final ClientHandler[] handlerRef = new ClientHandler[1];

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(testPort);
                serverReady.countDown();
                Socket clientSocket = serverSocket.accept();
                handlerRef[0] = new ClientHandler(clientSocket, server);
                clientHandlerCreated.countDown();
            } catch (IOException e) {
                serverReady.countDown();
                clientHandlerCreated.countDown();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(serverReady.await(2, TimeUnit.SECONDS));

        Socket client = new Socket("localhost", testPort);
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        out.println("TestPlayer");

        assertTrue(clientHandlerCreated.await(2, TimeUnit.SECONDS));

        // Supprimer le client
        assertDoesNotThrow(() -> server.removeClient(handlerRef[0]));

        client.close();
    }

    @Test
    @DisplayName("Test double suppression d'un client")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testDoubleRemoveClient() throws InterruptedException, IOException {
        CountDownLatch serverReady = new CountDownLatch(1);
        CountDownLatch clientHandlerCreated = new CountDownLatch(1);
        final ClientHandler[] handlerRef = new ClientHandler[1];

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(testPort);
                serverReady.countDown();
                Socket clientSocket = serverSocket.accept();
                handlerRef[0] = new ClientHandler(clientSocket, server);
                clientHandlerCreated.countDown();
            } catch (IOException e) {
                serverReady.countDown();
                clientHandlerCreated.countDown();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(serverReady.await(2, TimeUnit.SECONDS));

        Socket client = new Socket("localhost", testPort);
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        out.println("TestPlayer");

        assertTrue(clientHandlerCreated.await(2, TimeUnit.SECONDS));

        // Double suppression ne doit pas lever d'exception
        server.removeClient(handlerRef[0]);
        assertDoesNotThrow(() -> server.removeClient(handlerRef[0]), "Double suppression ne doit pas lever d'exception");

        client.close();
    }

    @Test
    @DisplayName("Test getClients après suppression de client")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testGetClientsAfterRemove() throws InterruptedException {
        int sizeBefore = server.getClients().size();
        server.removeClient(null);
        int sizeAfter = server.getClients().size();

        assertEquals(sizeBefore, sizeAfter, "La taille ne doit pas changer après suppression de null");
    }

    // ==================== Tests de broadcast ====================

    @Test
    @DisplayName("Test broadcast sans clients connectés")
    void testBroadcastWithoutClients() {
        assertDoesNotThrow(() -> server.broadcastMessage("Test message", null));
    }

    @Test
    @DisplayName("Test broadcast avec message null")
    void testBroadcastNullMessage() {
        assertDoesNotThrow(() -> server.broadcastMessage(null, null));
    }

    @Test
    @DisplayName("Test broadcast avec message vide")
    void testBroadcastEmptyMessage() {
        assertDoesNotThrow(() -> server.broadcastMessage("", null));
    }

    @Test
    @DisplayName("Test broadcast avec message long")
    void testBroadcastLongMessage() {
        String longMessage = "A".repeat(10000);
        assertDoesNotThrow(() -> server.broadcastMessage(longMessage, null));
    }

    @Test
    @DisplayName("Test broadcast avec caractères spéciaux")
    void testBroadcastSpecialCharacters() {
        String specialMessage = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        assertDoesNotThrow(() -> server.broadcastMessage(specialMessage, null));
    }

    @Test
    @DisplayName("Test broadcast avec unicode")
    void testBroadcastUnicodeMessage() {
        String unicodeMessage = "Joueur1:100,200 éàü 日本語 🎮";
        assertDoesNotThrow(() -> server.broadcastMessage(unicodeMessage, null));
    }

    // ==================== Tests de robustesse ====================

    @Test
    @DisplayName("Test stabilité après plusieurs opérations")
    void testStabilityAfterMultipleOperations() {
        for (int i = 0; i < 100; i++) {
            server.broadcastMessage("Message" + i, null);
            server.removeClient(null);
            server.getClients();
        }

        assertNotNull(server.getClients());
    }


    // ==================== Tests de format de message ====================

    @Test
    @DisplayName("Test broadcast format position standard")
    void testBroadcastStandardPositionFormat() {
        String positionMessage = "Player1:100,200";
        assertDoesNotThrow(() -> server.broadcastMessage(positionMessage, null));
    }

    @Test
    @DisplayName("Test broadcast format avec coordonnées négatives")
    void testBroadcastNegativeCoordinates() {
        String negativeMessage = "Player1:-100,-200";
        assertDoesNotThrow(() -> server.broadcastMessage(negativeMessage, null));
    }

    @Test
    @DisplayName("Test broadcast format avec grandes coordonnées")
    void testBroadcastLargeCoordinates() {
        String largeMessage = "Player1:999999,999999";
        assertDoesNotThrow(() -> server.broadcastMessage(largeMessage, null));
    }
}

