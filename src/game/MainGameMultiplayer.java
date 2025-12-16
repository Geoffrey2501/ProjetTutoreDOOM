package game;

import prototype_raycasting.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
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
    private Map<String, Sprite> playerSprites;

    private boolean running;
    private final int FPS = 60;
    private final long OPTIMAL_TIME = 1_000_000_000 / FPS;

    private static final String PLAYER_SPRITE_PATH = "src/prototype_raycasting/sprites/jonesy.png";


    public MainGameMultiplayer(String playerId, int port, String serverIp, int serverPort) {
        map = new prototype_raycasting.Map("src/prototype_raycasting/map/map.txt");
        joueur = new Joueur(playerId, 2.0, 2.0, 0.0);
        input = new Input();
        playerSprites = new ConcurrentHashMap<>();

        raycasting = new Raycasting(map, joueur);
        raycasting.addKeyListener(input);
        raycasting.addMouseListener(input);
        raycasting.addMouseMotionListener(input);
        raycasting.setFocusable(true);
        raycasting.requestFocus();
        raycasting.setVisible(true);

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
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
        double moveSpeed = 1.5 * delta;
        double rotSpeed = 2.0 * delta;

        double dx = 0;
        double dy = 0;
        boolean moved = false;

        if (input.forward) {
            dx += Math.cos(joueur.getAngle()) * moveSpeed;
            dy += Math.sin(joueur.getAngle()) * moveSpeed;
            moved = true;
        }
        if (input.backward) {
            dx -= Math.cos(joueur.getAngle()) * moveSpeed;
            dy -= Math.sin(joueur.getAngle()) * moveSpeed;
            moved = true;
        }
        if (input.strafeLeft) {
            dx += Math.sin(joueur.getAngle()) * moveSpeed;
            dy -= Math.cos(joueur.getAngle()) * moveSpeed;
            moved = true;
        }
        if (input.strafeRight) {
            dx -= Math.sin(joueur.getAngle()) * moveSpeed;
            dy += Math.cos(joueur.getAngle()) * moveSpeed;
            moved = true;
        }

        if (!map.isWall((int)(joueur.getX() + dx), (int)joueur.getY())) {
            joueur.setX(joueur.getX() + dx);
        }
        if (!map.isWall((int)joueur.getX(), (int)(joueur.getY() + dy))) {
            joueur.setY(joueur.getY() + dy);
        }

        if (input.turnLeft) {
            joueur.setAngle(joueur.getAngle() - rotSpeed);
            moved = true;
        }
        if (input.turnRight) {
            joueur.setAngle(joueur.getAngle() + rotSpeed);
            moved = true;
        }

        if (raycasting.isShowing()) {
            int centerX = raycasting.getWidth() / 2;
            int centerY = raycasting.getHeight() / 2;
            int currentMouseX = input.mouseX;
            int deltaX = currentMouseX - centerX;

            if (deltaX != 0) {
                joueur.setAngle(joueur.getAngle() + deltaX * 0.001);
                moved = true;

                Point centerScreen = new Point(centerX, centerY);
                SwingUtilities.convertPointToScreen(centerScreen, raycasting);
                robot.mouseMove(centerScreen.x, centerScreen.y);
                input.mouseX = centerX;
                input.mouseY = centerY;
            }
        }

        // Envoyer la position à chaque frame si le joueur a bougé
        // Le tick rate dans GestionConnection (16ms) optimisera l'envoi automatiquement
        if (moved) {
            network.sendPlayerPosition();
        }

        updateRemotePlayerSprites();
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

