package game;

import moteur_graphique.BSP.BSPParcours;
import moteur_graphique.BSP.CollisionBSP;
import moteur_graphique.BSP.MapMur;
import moteur_graphique.CollisionStrategy;
import moteur_graphique.GameRenderer;
import moteur_graphique.Window;
import moteur_graphique.raycasting.MapBool;
import moteur_graphique.raycasting.Raycasting;
import entite.Joueur;

import java.awt.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Classe principale du jeu multijoueur.
 * Coordonne les différents composants : rendu, réseau, entrées, joueur.
 * Implémente le pattern Façade pour simplifier l'utilisation du jeu.
 */
public class MainGameMultiplayer implements GameLoopListener, NetworkListener {

    private static final Logger LOGGER = Logger.getLogger(MainGameMultiplayer.class.getName());

    // === Composants du jeu ===
    private final MapBool map;
    private MapMur mapMur;

    private final CollisionStrategy collision;

    private final Joueur joueur;
    private final Window window;
    private final Raycasting raycasting;
    private final Input input;

    // === Contrôleurs ===
    private final PlayerController playerController;
    private final MouseGestion mouseCaptureHandler;
    private final PlayerSpriteManager spriteManager;
    private final GameLoop gameLoop;

    // === Réseau ===
    private final GameNetworkAdapter network;

    /**
     * Constructeur du jeu multijoueur.
     * @param playerId identifiant du joueur local
     * @param port port d'écoute pour les connexions entrantes
     * @param serverIp IP du serveur/pair à rejoindre (peut être null)
     * @param serverPort port du serveur/pair à rejoindre
     */
    public MainGameMultiplayer(String playerId, int port, String serverIp, int serverPort) {
        // 1. Initialisation de la carte et du joueur
        map = new MapBool(GameConfig.MAP_PATH);
        joueur = new Joueur(playerId, GameConfig.PLAYER_START_X, GameConfig.PLAYER_START_Y, GameConfig.PLAYER_START_ANGLE);
        input = new Input();

        // 2. Initialisation du moteur de rendu
        raycasting = new Raycasting(map, joueur);

        window = new Window(1920, 1080);
        // 2. Initialisation de la fenêtre (UI)
        // On définit une taille par défaut, par exemple 1280x720 ou 1920x1080

//        GameRenderer r = raycasting;
//        collision = new CollisionRaycasting(map);

        mapMur = new MapMur("assets/maps/mapBSP.txt");

        GameRenderer r = new BSPParcours(joueur, this.mapMur);
        collision = new CollisionBSP(mapMur);

        // 3. On lie le moteur à la fenêtre
        window.setRenderer(r);

        // 4. Gestion des Inputs sur la fenêtre

        window.addInputListener(input);

        // 4. Initialisation des contrôleurs
        playerController = new PlayerController(joueur, map, input);
        mouseCaptureHandler = new MouseGestion(window, input);

        // 5. Initialisation du réseau
        network = new GameNetworkAdapter(playerId, "localhost", port);
        network.setLocalPlayer(joueur);
        network.setNetworkListener(this);
        network.start();

        // 6. Initialisation du gestionnaire de sprites
        spriteManager = new PlayerSpriteManager(raycasting, network);

        // 7. Initialisation de la boucle de jeu
        gameLoop = new GameLoop(this);

        // 8. Connexion au serveur/pair si spécifié
        if (serverIp != null && !serverIp.isEmpty() && serverPort > 0) {
            network.connectToPlayer("Server", serverIp, serverPort);
        }

        // Log de connexion
        window.addLogMessage("Connecté en tant que " + playerId, Color.GREEN);
    }

    /**
     * Démarre le jeu dans un nouveau thread.
     */
    public void start() {
        new Thread(gameLoop).start();
    }

    @Override
    public void update(double delta) {
        // Mise à jour de la capture de la souris
        mouseCaptureHandler.update();

        double moveSpeed = 1.5 * delta;
        double rotSpeed = 2.0 * delta;

        // Mise à jour des mouvements du joueur
        boolean moved = handleMovement(moveSpeed);
        moved |= handleKeyboardRotation(rotSpeed);

        // Gestion de la rotation à la souris
        int deltaX = mouseCaptureHandler.handleMouseRotation();
        if (deltaX != 0) {
            playerController.applyMouseRotation(deltaX);
            moved = true;
        }

        // Envoi de la position si le joueur a bougé
        if (moved) {
            network.sendPlayerPosition();
        }

        // Gestion du scoreboard
        updateScoreboard();

        // Mise à jour des sprites des joueurs distants
        spriteManager.update(delta);
    }

    @Override
    public void render() {
        window.draw();
    }

    @Override
    public void onShutdown() {
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

    private boolean applyMovement(double dx, double dy) {
        boolean moved = false;
        double currentX = joueur.getX();
        double currentY = joueur.getY();
        double nextX = currentX + dx;
        double nextY = currentY + dy;

        double playerRadius = 0.3;

        if(!collision.isColliding(nextX, currentY, playerRadius)) {
            joueur.setX(nextX);
            currentX = nextX;
            moved = true;
        }

        if(!collision.isColliding(currentX, nextY, playerRadius)) {
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

    /**
     * Arrête le jeu.
     */
    public void stop() {
        gameLoop.stop();
    }


    /**
     * Récupère l'adresse IP locale de la machine.
     * @return l'adresse IP locale ou "localhost" si non trouvée
     */
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

        game.start();
    }
}