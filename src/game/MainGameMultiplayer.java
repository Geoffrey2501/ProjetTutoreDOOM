package game;

import prototype_raycasting.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class MainGameMultiplayer implements Runnable, NetworkListener {

    private prototype_raycasting.Map map;
    private Joueur joueur;
    private Raycasting raycasting;
    private Input input;
    private Robot robot;

    private GameNetworkAdapter network;
    private final Map<String, Sprite> playerSprites;

    private boolean running;
    private final int FPS = 60;
    private final long OPTIMAL_TIME = 1_000_000_000 / FPS;

    private static final String PLAYER_SPRITE_PATH = "src/prototype_raycasting/sprites/jonesy.png";


    private boolean mouseCaptured = true;
    private boolean escapePressed = false;
    private Cursor blankCursor;
    private Cursor defaultCursor;

    private final Point centerPoint;

    public MainGameMultiplayer(String playerId, int port, String serverIp, int serverPort) {
        map = new prototype_raycasting.Map("src/prototype_raycasting/map/map.txt");
        joueur = new Joueur(playerId, 2.0, 2.0, 0.0);
        input = new Input();
        playerSprites = new ConcurrentHashMap<>();
        centerPoint = new Point();

        raycasting = new Raycasting(map, joueur);
        raycasting.addKeyListener(input);
        raycasting.addMouseListener(input);
        raycasting.addMouseMotionListener(input);
        raycasting.setFocusable(true);
        raycasting.requestFocus();
        raycasting.setVisible(true);

        // Désactiver le focus traversal pour capturer la touche Tab
        Input.disableFocusTraversal(raycasting);

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
        defaultCursor = Cursor.getDefaultCursor();
        raycasting.getContentPane().setCursor(blankCursor);

        network = new GameNetworkAdapter(playerId, "localhost", port);
        network.setLocalPlayer(joueur);
        network.setNetworkListener(this);
        network.start();

        if (serverIp != null && !serverIp.isEmpty() && serverPort > 0) {
            network.connectToPlayer("Server", serverIp, serverPort);
        }

        raycasting.addLogMessage("Connecté en tant que " + playerId, Color.GREEN);
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
            raycasting.render();

            try {
                Thread.sleep(Math.max(0, (lastLoopTime - System.nanoTime() + OPTIMAL_TIME) / 1_000_000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        network.shutdown();
    }

    private void update(double delta) {
        handleMouseCaptureState();

        double angle = joueur.getAngle();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double moveSpeed = 1.5 * delta;
        double rotSpeed = 2.0 * delta;

        double dx = 0;
        double dy = 0;
        boolean moved = false;

        if (input.forward) {
            dx += cos;
            dy += sin;
        }
        if (input.backward) {
            dx -= cos;
            dy -= sin;
        }
        if (input.strafeLeft) {
            dx += sin;
            dy -= cos;
        }
        if (input.strafeRight) {
            dx -= sin;
            dy += cos;
        }

        if (dx != 0 || dy != 0) {
            dx *= moveSpeed;
            dy *= moveSpeed;

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
        }

        if (input.turnLeft) {
            joueur.setAngle(angle - rotSpeed);
            moved = true;
        } else if (input.turnRight) {
            joueur.setAngle(angle + rotSpeed);
            moved = true;
        }

        if (mouseCaptured && raycasting.isShowing()) {
            int width = raycasting.getWidth();
            int height = raycasting.getHeight();
            int centerX = width / 2;
            int centerY = height / 2;

            int deltaX = input.mouseX - centerX;

            if (deltaX != 0) {
                joueur.setAngle(joueur.getAngle() + deltaX * 0.001);
                moved = true;

                centerPoint.setLocation(centerX, centerY);
                SwingUtilities.convertPointToScreen(centerPoint, raycasting);
                robot.mouseMove(centerPoint.x, centerPoint.y);

                input.mouseX = centerX;
                input.mouseY = centerY;
            }
        }

        if (moved){
            network.sendPlayerPosition();
        }

        // Gestion du scoreboard (touche Tab)
        raycasting.setShowScoreboard(input.showScoreboard);
        if (input.showScoreboard) {
            List<String> remotePlayerNames = new ArrayList<>(network.getRemotePlayers().keySet());
            raycasting.updatePlayerList(joueur.getId(), remotePlayerNames);
        }

        updateRemotePlayerSprites();
    }

    private void handleMouseCaptureState() {
        if (input.escape) {
            if (!escapePressed) {
                escapePressed = true;
                toggleMouseCapture(!mouseCaptured);
            }
        } else {
            escapePressed = false;
        }

        if (!mouseCaptured && input.mouseLeftClicked) {
            toggleMouseCapture(true);
            input.mouseLeftClicked = false;
        }
    }

    private void toggleMouseCapture(boolean capture) {
        mouseCaptured = capture;
        if (mouseCaptured) {
            raycasting.getContentPane().setCursor(blankCursor);
            recenterMouse();
        } else {
            raycasting.getContentPane().setCursor(defaultCursor);
        }
    }

    private void recenterMouse() {
        if (raycasting.isShowing()) {
            centerPoint.setLocation(raycasting.getWidth() / 2, raycasting.getHeight() / 2);
            SwingUtilities.convertPointToScreen(centerPoint, raycasting);
            robot.mouseMove(centerPoint.x, centerPoint.y);
            input.mouseX = raycasting.getWidth() / 2;
            input.mouseY = raycasting.getHeight() / 2;
        }
    }

    private void updateRemotePlayerSprites() {
        for (Map.Entry<String, Joueur> entry : network.getRemotePlayers().entrySet()) {
            String playerId = entry.getKey();
            Joueur remotePlayer = entry.getValue();

            Sprite sprite = playerSprites.get(playerId);
            if (sprite != null) {
                sprite.setX(remotePlayer.getX());
                sprite.setY(remotePlayer.getY());
            }
        }
    }

    @Override
    public void onPlayerPositionUpdate(String playerId, double x, double y, double angle) {
        // Ne rien faire ici - le sprite est créé dans onPlayerJoin
    }

    @Override
    public void onPlayerJoin(String playerId) {
        // Éviter les doublons
        if (playerSprites.containsKey(playerId)) {
            return;
        }

        Joueur remotePlayer = network.getRemotePlayer(playerId);
        if (remotePlayer != null) {
            Sprite playerSprite = new Sprite(remotePlayer.getX(), remotePlayer.getY(), PLAYER_SPRITE_PATH, playerId);
            playerSprites.put(playerId, playerSprite);
            raycasting.addSprite(playerSprite);
            raycasting.addLogMessage(playerId + " a rejoint la partie", Color.GREEN);
        }

        network.sendPlayerPositionNow();
    }

    @Override
    public void onPlayerLeave(String playerId) {
        Sprite sprite = playerSprites.remove(playerId);
        if (sprite != null) {
            raycasting.removeSprite(sprite);
            raycasting.addLogMessage(playerId + " a quitté la partie", Color.RED);
        }
    }

    public void stop() {
        running = false;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== DOOM-LIKE MULTIJOUEUR ===\n");

        System.out.print("Votre nom de joueur: ");
        String playerId = scanner.nextLine().trim();
        if (playerId.isEmpty()) playerId = "Player" + System.currentTimeMillis() % 1000;

        System.out.print("Votre port (ex: 5001): ");
        int port = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Êtes-vous l'hôte (premier joueur)? (o/n): ");
        String isHost = scanner.nextLine().trim().toLowerCase();

        String serverIp = null;
        int serverPort = 0;

        if (!isHost.equals("o") && !isHost.equals("oui")) {
            System.out.print("IP du serveur (ex: localhost ou 192.168.1.10): ");
            serverIp = scanner.nextLine().trim();

            System.out.print("Port du serveur: ");
            serverPort = Integer.parseInt(scanner.nextLine().trim());
        }

        System.out.println("\nDémarrage du jeu...");
        System.out.println("Contrôles: ZQSD/Flèches pour bouger, Souris pour regarder\n");

        MainGameMultiplayer game = new MainGameMultiplayer(playerId, port, serverIp, serverPort);
        new Thread(game).start();
    }
}

