package game;

import entite.Joueur;
import java.awt.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.List;

import monstre.Monstre;
import monstre.Target;

import moteur_graphique.BSP.BSPParcours;
import moteur_graphique.BSP.CollisionBSP;
import moteur_graphique.BSP.MapMur;
import moteur_graphique.CollisionStrategy;
import moteur_graphique.GameRenderer;
import moteur_graphique.MinimapRenderer;
import moteur_graphique.Window;
import moteur_graphique.raycasting.CollisionRaycasting;
import moteur_graphique.raycasting.MapBool;
import moteur_graphique.raycasting.Raycasting;

/**
 * Classe principale du jeu multijoueur. Coordonne les différents composants :
 * rendu, réseau, entrées, joueur. Implémente le pattern Façade pour simplifier
 * l'utilisation du jeu.
 */
public class MainGameMultiplayer implements GameLoopListener, NetworkListener {

    // === Composants du jeu ===
    private final CollisionStrategy collision;
    private final Joueur joueur;
    private final Window window;
    private final Input input;

    // === Contrôleurs ===
    private final MouseGestion mouseCaptureHandler;
    private final PlayerSpriteManager spriteManager;
    private final MonsterSpriteManager monsterSpriteManager;
    private final GameLoop gameLoop;

    private Thread monsterThread;

    // === Réseau ===
    private final GameNetworkAdapter network;

    /**
     * Constructeur du jeu multijoueur.
     *
     * @param playerId identifiant du joueur local
     * @param port port d'écoute pour les connexions entrantes
     * @param serverIp IP du serveur/pair à rejoindre (peut être null)
     * @param serverPort port du serveur/pair à rejoindre
     * @param useBSP true pour utiliser BSP, false pour Raycasting
     */
    public MainGameMultiplayer(String playerId, int port, String serverIp, int serverPort, boolean useBSP) {
        window = new Window(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        joueur = new Joueur(playerId, GameConfig.PLAYER_START_X, GameConfig.PLAYER_START_Y, GameConfig.PLAYER_START_ANGLE);
        input = new Input();

        GameRenderer renderer;
        Raycasting raycasting;

        if (useBSP) {
            // Mode BSP
            MapMur mapMur = new MapMur("assets/maps/mapBSP.txt");
            renderer = new BSPParcours(joueur, mapMur);
            collision = new CollisionBSP(mapMur);
            // On crée quand même le raycasting pour le sprite manager
            MapBool map = new MapBool("assets/maps/map.txt");
            raycasting = new Raycasting(map, joueur);
        } else {
            // Mode Raycasting
            MapBool map = new MapBool("assets/maps/map.txt");
            raycasting = new Raycasting(map, joueur);
            renderer = raycasting;
            collision = new CollisionRaycasting(map);
        }

        window.setRenderer(renderer);

        // Créer l'adaptateur réseau d'abord
        network = new GameNetworkAdapter(playerId, "localhost", port);
        network.setLocalPlayer(joueur);
        network.setNetworkListener(this);

        // Puis créer la minimap avec le network
        GameRenderer minimapRenderer = new MinimapRenderer(joueur, collision, network);
        window.setMinimapRenderer(minimapRenderer);

        window.addInputListener(input);

        mouseCaptureHandler = new MouseGestion(window, input);

        network.start();

        spriteManager = new PlayerSpriteManager(renderer, network);
        monsterSpriteManager = new MonsterSpriteManager(renderer, minimapRenderer);

        boolean isHost = (serverIp == null || serverIp.isEmpty() || serverPort <= 0);

        if (useBSP) {
                if (isHost) {
                List<Monstre> monsters = Arrays.asList(
                    new Monstre(10.0, 10.0, collision),
                    new Monstre(12.0, 10.0, collision),
                    new Monstre(10.0, 12.0, collision)
                );

                // Initialize a temporary target using the player's position
                Target playerTarget = new Target(joueur);

                for (Monstre m : monsters) {
                    m.setTarget(playerTarget);
                    // Initialiser les points de patrouille pour que l'automate puisse passer en PATROUILLE
                    m.setPointsPatrouille(new monstre.Noeud(m.getX(), m.getY()), new monstre.Noeud(m.getX() + 5.0, m.getY() + 5.0));
                    monsterSpriteManager.onMonsterMove(String.valueOf(m.hashCode()), m.getX(), m.getY());
                }

                monsterThread = new Thread(() -> {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            for (Monstre m : monsters) {
                                Joueur closestPlayer = joueur;
                                double minDistanceSq = Math.pow(joueur.getX() - m.getX(), 2) + Math.pow(joueur.getY() - m.getY(), 2);

                                for (Joueur remote : network.getRemotePlayers().values()) {
                                    double remoteDistSq = Math.pow(remote.getX() - m.getX(), 2) + Math.pow(remote.getY() - m.getY(), 2);
                                    if (remoteDistSq < minDistanceSq) {
                                        minDistanceSq = remoteDistSq;
                                        closestPlayer = remote;
                                    }
                                }

                                if (m.getTarget() == null || !m.getTarget().getId().equals(closestPlayer.getId())) {
                                    m.setTarget(new Target(closestPlayer));
                                }

                                m.update();
                                String monsterId = String.valueOf(m.hashCode());
                                monsterSpriteManager.onMonsterMove(monsterId, m.getX(), m.getY());
                                network.sendMonsterPosition(monsterId, m.getX(), m.getY());
                            }
                            Thread.sleep(50); // Mettre à jour à ~20 FPS (50ms)
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                monsterThread.start();
            }
        }

        gameLoop = new GameLoop(this);

        if (serverIp != null && !serverIp.isEmpty() && serverPort > 0) {
            network.connectToPlayer("Server", serverIp, serverPort);
        }

        window.addLogMessage("Connecté en tant que " + playerId, Color.GREEN);
        window.addLogMessage("TAB: Afficher joueurs | FPS en haut à gauche", Color.YELLOW);
    }

    /**
     * Démarre le jeu dans un nouveau thread.
     */
    public void start() {
        new Thread(gameLoop).start();
    }

    @Override
    public void update(double delta) {
        mouseCaptureHandler.update();

        double moveSpeed = 1.5 * delta;
        double rotSpeed = 2.0 * delta;

        boolean moved = handleMovement(moveSpeed);
        moved |= handleKeyboardRotation(rotSpeed);

        int deltaX = mouseCaptureHandler.handleMouseRotation();
        if (deltaX != 0) {
            joueur.setAngle(joueur.getAngle() + deltaX * GameConfig.MOUSE_SENSITIVITY);
            moved = true;
        }

        if (moved) {
            network.sendPlayerPosition();
        }

        updateScoreboard();

        spriteManager.update(delta);
    }

    @Override
    public void render() {
        window.draw();
    }

    @Override
    public void onShutdown() {
        if (monsterThread != null) {
            monsterThread.interrupt();
        }
        network.shutdown();
    }

    /**
     * Met à jour l'affichage du scoreboard.
     */
    private void updateScoreboard() {
        window.setShowScoreboard(input.isShowScoreboard());
        if (input.isShowScoreboard()) {
            List<String> remotePlayerNames = new ArrayList<>(network.getRemotePlayers().keySet());
            window.updatePlayerList(joueur.getId(), remotePlayerNames);
        }
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


    @Override
    public void setFPS(int fps) {
        window.setFPS(fps);
    }

    private boolean applyMovement(double dx, double dy) {
        boolean moved = false;
        double currentX = joueur.getX();
        double currentY = joueur.getY();
        double nextX = currentX + dx;
        double nextY = currentY + dy;

        double playerRadius = 0.3;

        if (!collision.isColliding(nextX, currentY, playerRadius)) {
            joueur.setX(nextX);
            currentX = nextX;
            moved = true;
        }

        if (!collision.isColliding(currentX, nextY, playerRadius)) {
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

    @Override
    public void onPlayerPositionUpdate(String playerId, double x, double y, double angle) {
        spriteManager.onPlayerPositionUpdate(playerId, x, y);
    }

    @Override
    public void onPlayerJoin(String playerId) {
        if (spriteManager.onPlayerJoin(playerId)) {
            window.addLogMessage(playerId + " a rejoint la partie", Color.GREEN);
        }
        network.sendPlayerPositionNow();
    }

    @Override
    public void onPlayerLeave(String playerId) {
        if (spriteManager.onPlayerLeave(playerId)) {
            window.addLogMessage(playerId + " a quitté la partie", Color.RED);
        }
    }

    @Override
    public void onMonsterMove(String monsterId, double x, double y) {
        monsterSpriteManager.onMonsterMove(monsterId, x, y);
    }

    /**
     * Arrête le jeu.
     */
    public void stop() {
        gameLoop.stop();
    }

    /**
     * Récupère l'adresse IP locale de la machine.
     *
     * @return l'adresse IP locale ou "localhost" si non trouvée
     */
    private static String getLocalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs
        }
        return "localhost";
    }

    /**
     * Point d'entrée principal du jeu.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== DOOM-LIKE MULTIJOUEUR P2P ===\n");

        // Choix du mode de rendu
        System.out.println("Mode de rendu:");
        System.out.println("  1. BSP (Binary Space Partitioning)");
        System.out.println("  2. Raycasting");
        System.out.print("Votre choix (1/2): ");
        String renderChoice = scanner.nextLine().trim();
        boolean useBSP = !renderChoice.equals("2");
        System.out.println("Mode sélectionné: " + (useBSP ? "BSP" : "Raycasting") + "\n");

        String localIP = getLocalIPAddress();
        System.out.println("Votre IP locale: " + localIP);
        System.out.println("(utilisez cette adresse pour que d'autres se connectent à vous)\n");

        System.out.print("Votre nom de joueur: ");
        String playerId = scanner.nextLine().trim();
        if (playerId.isEmpty()) {
            playerId = "Player" + System.currentTimeMillis() % 1000;
        }

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

            game = new MainGameMultiplayer(playerId, port, peerIp, peerPort, useBSP);
            System.out.println("\nConnexion au pair " + peerIp + ":" + peerPort);
            System.out.println("Le maillage P2P va se former automatiquement...");
        } else {
            game = new MainGameMultiplayer(playerId, port, null, 0, useBSP);
            System.out.println("\nEn attente de connexions sur le port " + port);
            System.out.println("Les autres joueurs peuvent se connecter à votre IP:port");
        }

        System.out.println("\nDémarrage du jeu...");
        System.out.println("Contrôles: ZQSD/Flèches pour bouger, Souris pour regarder");
        System.out.println("Tab: Scoreboard | Échap: Libérer/Capturer la souris\n");

        game.start();
    }
}
