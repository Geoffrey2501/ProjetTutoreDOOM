package game;

import prototype_raycasting.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Version multijoueur du jeu Doom-like
 * Fusionne le prototype raycasting avec le système réseau P2P
 */
public class MainGameMultiplayer implements Runnable, NetworkListener {

    private prototype_raycasting.Map map;
    private Joueur joueur;
    private Raycasting raycasting;
    private Input input;
    private Robot robot;

    // Réseau
    private GameNetworkAdapter network;
    private Map<String, Sprite> playerSprites; // Sprites des autres joueurs

    private boolean running;
    private final int FPS = 60;
    private final long OPTIMAL_TIME = 1_000_000_000 / FPS;

    // Chemin du sprite pour les autres joueurs
    private static final String PLAYER_SPRITE_PATH = "src/prototype_raycasting/sprites/jonesy.png";

    // Optimisation réseau
    private int frameCounter = 0;
    private static final int NETWORK_UPDATE_INTERVAL = 3; // Envoyer toutes les 3 frames (20 fps au lieu de 60)
    private static final int SPRITE_UPDATE_INTERVAL = 2; // Mettre à jour les sprites toutes les 2 frames

    // Cache du centre de l'écran
    private int cachedCenterX = -1;
    private int cachedCenterY = -1;

    public MainGameMultiplayer(String playerId, int port, String serverIp, int serverPort) {
        // Initialisation de la carte et du joueur
        map = new prototype_raycasting.Map("src/prototype_raycasting/map/map.txt");
        joueur = new Joueur(playerId, 2.0, 2.0, 0.0);
        input = new Input();
        playerSprites = new ConcurrentHashMap<>();

        // Initialisation du raycasting
        raycasting = new Raycasting(map, joueur);
        raycasting.addKeyListener(input);
        raycasting.addMouseListener(input);
        raycasting.addMouseMotionListener(input);
        raycasting.setFocusable(true);
        raycasting.requestFocus();
        raycasting.setVisible(true);

        // Curseur invisible
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
            cursorImg, new Point(0, 0), "blank cursor");
        raycasting.getContentPane().setCursor(blankCursor);

        // Initialisation du réseau
        network = new GameNetworkAdapter(playerId, "localhost", port);
        network.setLocalPlayer(joueur);
        network.setNetworkListener(this);
        network.start();

        // Connexion au serveur si spécifié
        if (serverIp != null && !serverIp.isEmpty() && serverPort > 0) {
            System.out.println("Connexion à " + serverIp + ":" + serverPort + "...");
            network.connectToPlayer("Server", serverIp, serverPort);
        }

        System.out.println("[" + playerId + "] Jeu multijoueur démarré sur le port " + port);
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

        // Arrêt propre
        network.shutdown();
    }

    private void update(double delta) {
        frameCounter++;

        double moveSpeed = 1.5 * delta;
        double rotSpeed = 2.0 * delta;

        double dx = 0;
        double dy = 0;
        boolean moved = false;

        // Déplacement
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

        // Détection de collision (optimisé)
        if (dx != 0 && !map.isWall((int)(joueur.getX() + dx), (int)joueur.getY())) {
            joueur.setX(joueur.getX() + dx);
        }
        if (dy != 0 && !map.isWall((int)joueur.getX(), (int)(joueur.getY() + dy))) {
            joueur.setY(joueur.getY() + dy);
        }

        // Rotation clavier
        if (input.turnLeft) {
            joueur.setAngle(joueur.getAngle() - rotSpeed);
            moved = true;
        }
        if (input.turnRight) {
            joueur.setAngle(joueur.getAngle() + rotSpeed);
            moved = true;
        }

        // Rotation souris (cache le centre pour éviter de le recalculer)
        if (raycasting.isShowing()) {
            // Recalculer le centre seulement si nécessaire
            if (cachedCenterX < 0) {
                cachedCenterX = raycasting.getWidth() / 2;
                cachedCenterY = raycasting.getHeight() / 2;
            }

            int deltaX = input.mouseX - cachedCenterX;

            if (deltaX != 0) {
                joueur.setAngle(joueur.getAngle() + deltaX * 0.001);
                moved = true;

                Point centerScreen = new Point(cachedCenterX, cachedCenterY);
                SwingUtilities.convertPointToScreen(centerScreen, raycasting);
                robot.mouseMove(centerScreen.x, centerScreen.y);
                input.mouseX = cachedCenterX;
                input.mouseY = cachedCenterY;
            }
        }

        // Envoyer la position seulement toutes les N frames (réduit le trafic réseau)
        if (moved && (frameCounter % NETWORK_UPDATE_INTERVAL == 0)) {
            network.sendPlayerPosition();
        }

        // Mettre à jour les sprites des autres joueurs moins souvent
        if (frameCounter % SPRITE_UPDATE_INTERVAL == 0) {
            updateRemotePlayerSprites();
        }
    }

    /**
     * Met à jour les positions des sprites des autres joueurs
     * Optimisé : met à jour seulement si la position a changé significativement
     */
    private void updateRemotePlayerSprites() {
        for (Map.Entry<String, Joueur> entry : network.getRemotePlayers().entrySet()) {
            String playerId = entry.getKey();
            Joueur remotePlayer = entry.getValue();

            Sprite sprite = playerSprites.get(playerId);
            if (sprite != null) {
                double newX = remotePlayer.getX();
                double newY = remotePlayer.getY();

                // Mettre à jour seulement si la position a changé de plus de 0.01
                // (évite les mises à jour inutiles pour des micro-mouvements)
                double dx = Math.abs(sprite.getX() - newX);
                double dy = Math.abs(sprite.getY() - newY);

                if (dx > 0.01 || dy > 0.01) {
                    sprite.setX(newX);
                    sprite.setY(newY);
                }
            }
        }
    }

    // =========================================================================
    // NetworkListener callbacks
    // =========================================================================

    @Override
    public void onPlayerPositionUpdate(String playerId, double x, double y, double angle) {
        // La position est déjà mise à jour dans le Joueur distant
        // Les sprites sont mis à jour dans updateRemotePlayerSprites()
    }

    @Override
    public void onPlayerJoin(String playerId) {
        System.out.println("[GAME] Nouveau joueur: " + playerId);

        // Créer un sprite pour le nouveau joueur
        Joueur remotePlayer = network.getRemotePlayer(playerId);
        if (remotePlayer != null) {
            Sprite playerSprite = new Sprite(remotePlayer.getX(), remotePlayer.getY(), PLAYER_SPRITE_PATH);
            playerSprites.put(playerId, playerSprite);
            raycasting.addSprite(playerSprite);
        }
    }

    @Override
    public void onPlayerLeave(String playerId) {
        System.out.println("[GAME] Joueur parti: " + playerId);

        // Retirer le sprite du joueur
        Sprite sprite = playerSprites.remove(playerId);
        if (sprite != null) {
            raycasting.removeSprite(sprite);
        }
    }

    public void stop() {
        running = false;
    }

    // =========================================================================
    // Point d'entrée
    // =========================================================================

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== DOOM-LIKE MULTIJOUEUR ===\n");

        // Demander les informations de connexion
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
        System.out.println("Contrôles: ZQSD/Flèches pour bouger, Souris pour regarder");
        System.out.println("============================================\n");

        // Démarrer le jeu
        MainGameMultiplayer game = new MainGameMultiplayer(playerId, port, serverIp, serverPort);
        new Thread(game).start();
    }
}

