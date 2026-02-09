package game;

import moteur_graphique.Window;
import moteur_graphique.raycasting.MapBool;
import moteur_graphique.raycasting.Raycasting;
import entite.Joueur;
import entite.Sprite;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class MainGameMultiplayer implements Runnable, NetworkListener {

    private static final Logger LOGGER = Logger.getLogger(MainGameMultiplayer.class.getName());

    private final MapBool map;
    private final Joueur joueur;

    //séparation : Window pour l'UI/Fenêtre, GameRenderer pour le calcul/rendu
    private final Window window;
    private final Raycasting raycasting;

    private final Input input;
    private Robot robot;

    private final GameNetworkAdapter network;
    private final java.util.Map<String, Sprite> playerSprites;

    private boolean running;
    public static final int FPS = 60;
    public static final long OPTIMAL_TIME = 1_000_000_000 / FPS;

    private static final String PLAYER_SPRITE_PATH = "assets/sprites/jonesy.png";

    private boolean mouseCaptured = true;
    private boolean escapePressed = false;
    private final Cursor blankCursor;
    private final Cursor defaultCursor;

    private final Point centerPoint;

    public MainGameMultiplayer(String playerId, int port, String serverIp, int serverPort) {
        map = new MapBool("assets/maps/map.txt");
        joueur = new Joueur(playerId, 2.0, 2.0, 0.0);
        input = new Input();
        playerSprites = new ConcurrentHashMap<>();
        centerPoint = new Point();

        // 1. Initialisation du moteur de rendu (logique pure)
        raycasting = new Raycasting(map, joueur);

        // 2. Initialisation de la fenêtre (UI)
        // On définit une taille par défaut, par exemple 1280x720 ou 1920x1080
        window = new Window(1920, 1080);

        //GameRenderer r = new TopDownRenderer(map, joueur);
        //window.setRenderer(r);

        // 3. On lie le moteur à la fenêtre
        window.setRenderer(raycasting);

        // 4. Gestion des Inputs sur la fenêtre
        window.addInputListener(input);

        try {
            robot = new Robot();
        } catch (AWTException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Erreur lors de la création du Robot", e);
        }

        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
        defaultCursor = Cursor.getDefaultCursor();

        // Application du curseur sur la fenêtre
        window.setCursor(blankCursor);

        network = new GameNetworkAdapter(playerId, "localhost", port);
        network.setLocalPlayer(joueur);
        network.setNetworkListener(this);
        network.start();

        if (serverIp != null && !serverIp.isEmpty() && serverPort > 0) {
            network.connectToPlayer("Server", serverIp, serverPort);
        }

        // Log via la fenêtre
        window.addLogMessage("Connecté en tant que " + playerId, Color.GREEN);
    }

    @Override
    public void run() {
        running = true;
        long lastLoopTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            long updateLength = now - lastLoopTime;
            lastLoopTime = now;
            double delta = updateLength / 1_000_000_000.0;

            update(delta);

            // Rendu via la fenêtre
            window.draw();

            try {
                Thread.sleep(Math.max(0, (lastLoopTime - System.nanoTime() + OPTIMAL_TIME) / 1_000_000));
            } catch (InterruptedException e) {
                LOGGER.log(java.util.logging.Level.WARNING, "Thread interrompu", e);
                Thread.currentThread().interrupt();
            }
        }

        network.shutdown();
    }

    private void update(double delta) {
        handleMouseCaptureState();

        double moveSpeed = 1.5 * delta;
        double rotSpeed = 2.0 * delta;

        boolean moved = handleMovement(moveSpeed);
        moved |= handleKeyboardRotation(rotSpeed);
        moved |= handleMouseRotation();

        if (moved) {
            network.sendPlayerPosition();
        }

        // Gestion du scoreboard via Window
        window.setShowScoreboard(input.isShowScoreboard());
        if (input.isShowScoreboard()) {
            List<String> remotePlayerNames = new ArrayList<>(network.getRemotePlayers().keySet());
            window.updatePlayerList(joueur.getId(), remotePlayerNames);
        }

        updateRemotePlayerSprites(delta);
    }

    private boolean handleMovement(double moveSpeed) {
        double angle = joueur.getAngle();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double dx = 0;
        double dy = 0;
        boolean moved = false;

        if (input.isForward()) {
            dx += cos;
            dy += sin;
        }
        if (input.isBackward()) {
            dx -= cos;
            dy -= sin;
        }
        if (input.isStrafeLeft()) {
            dx += sin;
            dy -= cos;
        }
        if (input.isStrafeRight()) {
            dx -= sin;
            dy += cos;
        }

        if (dx != 0 || dy != 0) {
            dx *= moveSpeed;
            dy *= moveSpeed;
            moved = applyMovement(dx, dy);
        }

        return moved;
    }

    private boolean applyMovement(double dx, double dy) {
        boolean moved = false;
        double nextX = joueur.getX() + dx;
        double nextY = joueur.getY() + dy;

        if (!map.isWall((int) nextX, (int) joueur.getY())) {
            joueur.setX(nextX);
            moved = true;
        }

        if (!map.isWall((int) joueur.getX(), (int) nextY)) {
            joueur.setY(nextY);
            moved = true;
        }

        return moved;
    }

    private boolean handleKeyboardRotation(double rotSpeed) {
        double angle = joueur.getAngle();
        if (input.isTurnLeft()) {
            joueur.setAngle(angle - rotSpeed);
            return true;
        } else if (input.isTurnRight()) {
            joueur.setAngle(angle + rotSpeed);
            return true;
        }
        return false;
    }

    private boolean handleMouseRotation() {
        // On vérifie si la fenêtre est visible et active
        if (!mouseCaptured || !window.isVisible()) {
            return false;
        }

        // On utilise les dimensions de la Window
        int width = window.getWidth();
        int height = window.getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        int deltaX = input.getMouseX() - centerX;

        if (deltaX != 0) {
            joueur.setAngle(joueur.getAngle() + deltaX * 0.001);

            centerPoint.setLocation(centerX, centerY);
            // Conversion relative à la fenêtre
            SwingUtilities.convertPointToScreen(centerPoint, window);
            robot.mouseMove(centerPoint.x, centerPoint.y);

            input.setMouseX(centerX);
            input.setMouseY(centerY);
            return true;
        }

        return false;
    }

    private void handleMouseCaptureState() {
        if (input.isEscape()) {
            if (!escapePressed) {
                escapePressed = true;
                toggleMouseCapture(!mouseCaptured);
            }
        } else {
            escapePressed = false;
        }

        if (!mouseCaptured && input.isMouseLeftClicked()) {
            toggleMouseCapture(true);
            input.resetMouseLeftClicked();
        }
    }

    private void toggleMouseCapture(boolean capture) {
        mouseCaptured = capture;
        if (mouseCaptured) {
            window.setCursor(blankCursor);
            recenterMouse();
        } else {
            window.setCursor(defaultCursor);
        }
    }

    private void recenterMouse() {
        if (window.isVisible()) {
            centerPoint.setLocation(window.getWidth() / 2, window.getHeight() / 2);
            SwingUtilities.convertPointToScreen(centerPoint, window);
            robot.mouseMove(centerPoint.x, centerPoint.y);
            input.setMouseX(window.getWidth() / 2);
            input.setMouseY(window.getHeight() / 2);
        }
    }

    private void updateRemotePlayerSprites(double delta) {
        for (java.util.Map.Entry<String, Joueur> entry : network.getRemotePlayers().entrySet()) {
            String playerId = entry.getKey();
            Joueur remotePlayer = entry.getValue();

            remotePlayer.interpolate(delta);

            Sprite sprite = playerSprites.get(playerId);
            if (sprite != null) {
                sprite.setX(remotePlayer.getX());
                sprite.setY(remotePlayer.getY());
            }
        }
    }

    @Override
    public void onPlayerPositionUpdate(String playerId, double x, double y, double angle) {
        synchronized (playerSprites) {
            if (!playerSprites.containsKey(playerId)) {
                Joueur remotePlayer = network.getRemotePlayer(playerId);
                if (remotePlayer != null && remotePlayer.isPositionInitialized()) {
                    Sprite playerSprite = new Sprite(x, y, PLAYER_SPRITE_PATH, playerId);
                    playerSprites.put(playerId, playerSprite);
                    // Ajout du sprite au moteur de rendu (Raycasting)
                    raycasting.addSprite(playerSprite);
                }
            }
        }
    }

    @Override
    public void onPlayerJoin(String playerId) {
        synchronized (playerSprites) {
            if (playerSprites.containsKey(playerId)) {
                return;
            }

            Joueur remotePlayer = network.getRemotePlayer(playerId);
            if (remotePlayer != null && remotePlayer.isPositionInitialized()) {
                Sprite playerSprite = new Sprite(
                        remotePlayer.getX(),
                        remotePlayer.getY(),
                        PLAYER_SPRITE_PATH,
                        playerId
                );
                playerSprites.put(playerId, playerSprite);
                raycasting.addSprite(playerSprite);
            }
        }

        // Log sur la fenêtre
        window.addLogMessage(playerId + " a rejoint la partie", Color.GREEN);
        network.sendPlayerPositionNow();
    }

    @Override
    public void onPlayerLeave(String playerId) {
        Sprite sprite;
        synchronized (playerSprites) {
            sprite = playerSprites.remove(playerId);
        }
        if (sprite != null) {
            raycasting.removeSprite(sprite);
            // Log sur la fenêtre
            window.addLogMessage(playerId + " a quitté la partie", Color.RED);
        }
    }

    public void stop() {
        running = false;
    }

    private static String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer
        }
        return "localhost";
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== DOOM-LIKE MULTIJOUEUR P2P ===\n");

        String localIP = getLocalIPAddress();
        System.out.println("Votre IP locale: " + localIP);
        System.out.println("(utilisez cette adresse pour que d'autres se connectent à vous)\n");

        System.out.print("Votre nom de joueur: ");
        String playerId = scanner.nextLine().trim();
        if (playerId.isEmpty()) playerId = "Player" + System.currentTimeMillis() % 1000;

        System.out.print("Votre port (ex: 5001): ");
        int port = Integer.parseInt(scanner.nextLine().trim());

        System.out.println("\n=== Mode Peer-to-Peer (Maillage complet) ===");
        System.out.println("Vous pouvez vous connecter à un ou plusieurs joueurs.");
        System.out.println("Le réseau se synchronisera automatiquement (tous connectés à tous).\n");

        System.out.print("Voulez-vous rejoindre un joueur existant? (o/n): ");
        String wantToConnect = scanner.nextLine().trim().toLowerCase();

        MainGameMultiplayer game;

        if (wantToConnect.equals("o") || wantToConnect.equals("oui")) {
            System.out.print("IP du pair (ex: localhost ou 192.168.1.10): ");
            String peerIp = scanner.nextLine().trim();

            System.out.print("Port du pair: ");
            int peerPort = Integer.parseInt(scanner.nextLine().trim());

            game = new MainGameMultiplayer(playerId, port, peerIp, peerPort);
            System.out.println("\nConnexion au pair " + peerIp + ":" + peerPort);
            System.out.println("Le maillage P2P va se former automatiquement...");
        } else {
            game = new MainGameMultiplayer(playerId, port, null, 0);
            System.out.println("\nEn attente de connexions sur le port " + port);
            System.out.println("Les autres joueurs peuvent se connecter à votre IP:port");
        }

        System.out.println("\nDémarrage du jeu...");
        System.out.println("Contrôles: ZQSD/Flèches pour bouger, Souris pour regarder");
        System.out.println("Tab: Scoreboard | Échap: Libérer/Capturer la souris\n");

        new Thread(game).start();
    }
}