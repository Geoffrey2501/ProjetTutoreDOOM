package game;

import entite.Joueur;
import java.awt.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.List;
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

    private volatile Thread monsterThread;
    private volatile List<MonsterState> monsters;
    private boolean isBSPMode;

    // === Réseau ===
    private final GameNetworkAdapter network;

    private static class MonsterState {
        String id;
        double[] pos;
        MonsterState(String id, double x, double y) {
            this.id = id;
            this.pos = new double[]{x, y};
        }
    }

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

        isBSPMode = useBSP;
        boolean isHost = (serverIp == null || serverIp.isEmpty() || serverPort <= 0);

        if (useBSP && isHost) {
            List<MonsterState> initialMonsters = Arrays.asList(
                new MonsterState("DarkJonesy_1", GameConfig.PLAYER_START_X + 5, GameConfig.PLAYER_START_Y + 5),
                new MonsterState("DarkJonesy_2", GameConfig.PLAYER_START_X - 5, GameConfig.PLAYER_START_Y + 5),
                new MonsterState("DarkJonesy_3", GameConfig.PLAYER_START_X + 5, GameConfig.PLAYER_START_Y - 5)
            );
            for (MonsterState m : initialMonsters) {
                monsterSpriteManager.onMonsterMove(m.id, m.pos[0], m.pos[1]);
                network.sendMonsterPosition(m.id, m.pos[0], m.pos[1]);
            }
            network.announceMonsterHost();
            startMonsterThread(initialMonsters);
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
        // Si on est l'hôte des monstres, on ré-annonce pour que le nouveau joueur le sache
        if (isBSPMode && joueur.getId().equals(network.getMonsterHostId())) {
            network.announceMonsterHost();
        }
    }

    @Override
    public void onPlayerLeave(String playerId) {
        if (spriteManager.onPlayerLeave(playerId)) {
            window.addLogMessage(playerId + " a quitté la partie", Color.RED);
        }
        // Host migration : si l'hôte des monstres vient de partir, on élit un nouveau
        if (isBSPMode && playerId.equals(network.getMonsterHostId())) {
            electNewMonsterHost();
        }
    }

    private void electNewMonsterHost() {
        List<String> allIds = new ArrayList<>();
        allIds.add(joueur.getId());
        allIds.addAll(network.getRemotePlayers().keySet());
        Collections.sort(allIds);

        String elected = allIds.get(0);
        if (elected.equals(joueur.getId())) {
            Map<String, double[]> lastPos = network.getLastMonsterPositions();
            List<MonsterState> resumedMonsters = new ArrayList<>();
            for (Map.Entry<String, double[]> entry : lastPos.entrySet()) {
                resumedMonsters.add(new MonsterState(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
            }
            network.announceMonsterHost();
            startMonsterThread(resumedMonsters);
            window.addLogMessage("Vous êtes maintenant l'hôte des monstres", Color.YELLOW);
        }
    }

    private void startMonsterThread(List<MonsterState> monsterList) {
        monsters = monsterList;
        if (monsterThread != null) {
            monsterThread.interrupt();
        }
        monsterThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                for (MonsterState m : monsters) {
                    double minDist = Double.MAX_VALUE;
                    Joueur target = null;

                    List<Joueur> allPlayers = new ArrayList<>();
                    allPlayers.add(joueur);
                    allPlayers.addAll(network.getRemotePlayers().values());

                    for (Joueur p : allPlayers) {
                        if (p == joueur || p.isPositionInitialized()) {
                            double dx = p.getX() - m.pos[0];
                            double dy = p.getY() - m.pos[1];
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d < minDist) {
                                minDist = d;
                                target = p;
                            }
                        }
                    }

                    if (target != null && minDist > 0.5) {
                        int maxMonsters = monsters.size();
                        int monsterIndex = monsters.indexOf(m);
                        double angleSlot = (2 * Math.PI / maxMonsters) * monsterIndex;
                        double distanceEncerclement = 2.0;

                        double slotX = target.getX() + Math.cos(angleSlot) * distanceEncerclement;
                        double slotY = target.getY() + Math.sin(angleSlot) * distanceEncerclement;

                        double sdx = slotX - m.pos[0];
                        double sdy = slotY - m.pos[1];
                        double sd = Math.sqrt(sdx * sdx + sdy * sdy);

                        double speed = 0.05;
                        if (sd > 0.1) {
                            double nextX = m.pos[0] + (sdx / sd) * speed;
                            double nextY = m.pos[1] + (sdy / sd) * speed;
                            double monsterRadius = 0.3;

                            if (!collision.isColliding(nextX, m.pos[1], monsterRadius)) {
                                m.pos[0] = nextX;
                            }
                            if (!collision.isColliding(m.pos[0], nextY, monsterRadius)) {
                                m.pos[1] = nextY;
                            }
                        }

                        monsterSpriteManager.onMonsterMove(m.id, m.pos[0], m.pos[1]);
                        network.sendMonsterPosition(m.id, m.pos[0], m.pos[1]);
                    }
                }
            }
        });
        monsterThread.setDaemon(true);
        monsterThread.start();
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
