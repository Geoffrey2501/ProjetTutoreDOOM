package prototype_raycasting;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Raycasting extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(Raycasting.class.getName());

    private JPanel panelDessin;
    private boolean render;
    private Map map;
    private Joueur joueur;
    private static final int FOV = 60;
    private static final int NUM_RAYS = 1000;

    private final transient List<Sprite> sprites = new CopyOnWriteArrayList<>();
    private double[] zBuffer;

    // Texture du mur
    private int[] wallTexturePixels; // Pixels de la texture en tableau pour accès rapide
    private int texWidth = 64;
    private int texHeight = 64;

    // Buffer de rendu pour optimisation
    private transient BufferedImage screenBuffer;
    private int[] screenPixels;
    private int lastScreenWidth = 0;
    private int lastScreenHeight = 0;

    // Logs à afficher en haut à gauche
    private final transient List<LogMessage> logMessages = new CopyOnWriteArrayList<>();
    private static final int MAX_LOGS = 5;
    private static final long LOG_DURATION_MS = 5000; // 5 secondes

    // Scoreboard (tableau des joueurs)
    private boolean showScoreboard = false;
    private final List<String> playerList = new CopyOnWriteArrayList<>();
    private String localPlayerName = "";

    // Constante pour la police
    private static final String FONT_ARIAL = "Arial";

    // Couleurs constantes pour le rendu
    private static final int SKY_COLOR = new Color(135, 206, 235).getRGB();
    private static final int FLOOR_COLOR = new Color(105, 105, 105).getRGB();
    private static final int WALL_COLOR_LIGHT = new Color(200, 100, 0).getRGB();
    private static final int WALL_COLOR_DARK = new Color(150, 75, 0).getRGB();

    // Classe interne pour stocker les résultats du DDA
    private static class RayResult {
        double perpWallDist;
        boolean side;
        int stepX;
        int stepY;
        double rayDirX;
        double rayDirY;
    }

    // Palette de couleurs pour les joueurs distants
    private static final Color[] PLAYER_COLORS = {
        new Color(0, 150, 255),   // Bleu
        new Color(255, 100, 100), // Rouge
        new Color(255, 200, 0),   // Jaune/Or
        new Color(200, 100, 255), // Violet
        new Color(255, 150, 50),  // Orange
        new Color(100, 255, 255), // Cyan
        new Color(255, 100, 200), // Rose
        new Color(150, 255, 150), // Vert clair
    };

    /**
     * Obtenir la couleur d'un joueur basée sur son nom
     */
    private Color getPlayerColor(String playerName) {
        if (playerName.equals(localPlayerName)) {
            return new Color(0, 200, 0); // Vert pour le joueur local
        }
        // Utiliser le hashcode du nom pour avoir une couleur cohérente
        int colorIndex = (playerName.hashCode() & 0x7FFFFFFF) % PLAYER_COLORS.length;
        return PLAYER_COLORS[colorIndex];
    }

    /**
     * Tronquer un nom s'il dépasse la longueur maximale
     */
    private static final int MAX_NAME_LENGTH = 20;

    private String truncateName(String name) {
        if (name == null) return "";
        if (name.length() <= MAX_NAME_LENGTH) {
            return name;
        }
        return name.substring(0, MAX_NAME_LENGTH - 3) + "...";
    }

    private static class LogMessage {
        String text;
        long timestamp;
        Color color;

        LogMessage(String text, Color color) {
            this.text = text;
            this.color = color;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > LOG_DURATION_MS;
        }
    }

    public Raycasting(Map m, Joueur j) {
        super("Prototype Raycasting");

        this.map = m;
        this.joueur = j;
        render = false;

        // Charger la texture du mur
        loadWallTexture();

        panelDessin = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(render) {
                    dessiner(g);
                }
            }
        };
        panelDessin.setBackground(Color.BLACK);
        add(panelDessin);

        WindowListener l = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e){
                System.exit(0);
            }
        };

        addWindowListener(l);
        setSize(1920, 1080);
        setVisible(true);
    }

    public void addSprite(Sprite sprite) {
        sprites.add(sprite);
    }

    public void removeSprite(Sprite sprite) {
        sprites.remove(sprite);
    }

    /**
     * Ajouter un message de log à afficher
     */
    public void addLogMessage(String message, Color color) {
        logMessages.add(new LogMessage(message, color));
        // Limiter le nombre de logs
        while (logMessages.size() > MAX_LOGS) {
            logMessages.remove(0);
        }
    }

    public void addLogMessage(String message) {
        addLogMessage(message, Color.RED);
    }

    /**
     * Définir si le scoreboard doit être affiché
     */
    public void setShowScoreboard(boolean show) {
        this.showScoreboard = show;
    }

    /**
     * Mettre à jour la liste des joueurs pour le scoreboard
     */
    public void updatePlayerList(String localPlayer, List<String> remotePlayers) {
        this.localPlayerName = localPlayer;
        this.playerList.clear();
        this.playerList.add(localPlayer); // Le joueur local en premier
        this.playerList.addAll(remotePlayers);
    }

    /**
     * Charger la texture du mur depuis le fichier assets/wall.png
     */
    private void loadWallTexture() {
        try {
            File textureFile = new File("assets/textures/wall.jpg");
            if (textureFile.exists()) {
                BufferedImage loaded = ImageIO.read(textureFile);
                // Convertir en TYPE_INT_ARGB pour accès rapide aux pixels
                texWidth = loaded.getWidth();
                texHeight = loaded.getHeight();
                BufferedImage wallTexture = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = wallTexture.createGraphics();
                g2d.drawImage(loaded, 0, 0, null);
                g2d.dispose();

                // Extraire les pixels dans un tableau pour accès ultra-rapide
                wallTexturePixels = new int[texWidth * texHeight];
                wallTexture.getRGB(0, 0, texWidth, texHeight, wallTexturePixels, 0, texWidth);

                LOGGER.log(Level.INFO, "Texture du mur chargée: {0}x{1}", new Object[]{texWidth, texHeight});
            } else {
                LOGGER.warning("Fichier texture non trouvé: assets/wall.png - Utilisation des couleurs par défaut");
                wallTexturePixels = null;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Erreur lors du chargement de la texture: " + e.getMessage());
            wallTexturePixels = null;
        }
    }

    /**
     * Initialiser ou redimensionner le buffer de rendu
     */
    private void initScreenBuffer(int width, int height) {
        if (screenBuffer == null || lastScreenWidth != width || lastScreenHeight != height) {
            screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            screenPixels = ((DataBufferInt) screenBuffer.getRaster().getDataBuffer()).getData();
            lastScreenWidth = width;
            lastScreenHeight = height;
        }
    }

    private void dessiner(Graphics g) {
        int screenWidth = panelDessin.getWidth();
        int screenHeight = panelDessin.getHeight();

        if (screenWidth <= 0 || screenHeight <= 0) return;

        // Initialiser les buffers
        initScreenBuffer(screenWidth, screenHeight);
        zBuffer = new double[screenWidth];

        // Dessiner le fond (ciel et sol)
        dessinerFond(screenWidth, screenHeight);

        // Lancer les rayons et dessiner les murs
        double fov = Math.toRadians(FOV);
        lancerRayons(screenWidth, screenHeight, fov);

        // Dessiner le buffer à l'écran
        g.drawImage(screenBuffer, 0, 0, null);

        // Dessiner les sprites
        dessinerSprites(g, screenWidth, screenHeight, joueur.getX(), joueur.getY(), joueur.getAngle(), fov);

        // Dessiner les logs et le scoreboard
        dessinerLogs(g);
        if (showScoreboard) {
            dessinerScoreboard(g, screenWidth, screenHeight);
        }
    }

    /**
     * Dessiner le fond (ciel en haut, sol en bas)
     */
    private void dessinerFond(int screenWidth, int screenHeight) {
        int halfHeight = screenHeight / 2;
        for (int y = 0; y < screenHeight; y++) {
            int color = (y < halfHeight) ? SKY_COLOR : FLOOR_COLOR;
            int rowStart = y * screenWidth;
            for (int x = 0; x < screenWidth; x++) {
                screenPixels[rowStart + x] = color;
            }
        }
    }

    /**
     * Lancer tous les rayons et dessiner les murs
     */
    private void lancerRayons(int screenWidth, int screenHeight, double fov) {
        double joueurX = joueur.getX();
        double joueurY = joueur.getY();
        double joueurAngle = joueur.getAngle();

        for (int i = 0; i < NUM_RAYS; i++) {
            double rayAngle = joueurAngle - fov / 2 + (fov * i / NUM_RAYS);
            RayResult result = executerDDA(rayAngle, joueurX, joueurY);
            dessinerColonneMur(i, screenWidth, screenHeight, result, joueurX, joueurY);
        }
    }

    /**
     * Exécuter l'algorithme DDA pour un rayon
     */
    private RayResult executerDDA(double rayAngle, double joueurX, double joueurY) {
        boolean[][] mapData = map.getGrid();
        int mapWidth = map.getWIDTH();
        int mapHeight = map.getHeight();

        RayResult result = new RayResult();
        result.rayDirX = Math.cos(rayAngle);
        result.rayDirY = Math.sin(rayAngle);

        double deltaDistX = Math.abs(1 / result.rayDirX);
        double deltaDistY = Math.abs(1 / result.rayDirY);

        int mapX = (int) joueurX;
        int mapY = (int) joueurY;

        double sideDistX = calculerSideDistX(result, joueurX, mapX, deltaDistX);
        double sideDistY = calculerSideDistY(result, joueurY, mapY, deltaDistY);

        // Effectuer le DDA
        boolean hit = false;
        while (!hit) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX;
                mapX += result.stepX;
                result.side = false;
            } else {
                sideDistY += deltaDistY;
                mapY += result.stepY;
                result.side = true;
            }

            hit = isHit(mapX, mapY, mapWidth, mapHeight, mapData);
        }

        // Calculer la distance perpendiculaire
        result.perpWallDist = calculerPerpWallDist(result, mapX, mapY, joueurX, joueurY);
        return result;
    }

    private double calculerSideDistX(RayResult result, double joueurX, int mapX, double deltaDistX) {
        if (result.rayDirX < 0) {
            result.stepX = -1;
            return (joueurX - mapX) * deltaDistX;
        } else {
            result.stepX = 1;
            return (mapX + 1.0 - joueurX) * deltaDistX;
        }
    }

    private double calculerSideDistY(RayResult result, double joueurY, int mapY, double deltaDistY) {
        if (result.rayDirY < 0) {
            result.stepY = -1;
            return (joueurY - mapY) * deltaDistY;
        } else {
            result.stepY = 1;
            return (mapY + 1.0 - joueurY) * deltaDistY;
        }
    }

    private boolean isHit(int mapX, int mapY, int mapWidth, int mapHeight, boolean[][] mapData) {
        if (mapX < 0 || mapX >= mapWidth || mapY < 0 || mapY >= mapHeight) {
            return true;
        }
        return mapData[mapY][mapX];
    }

    private double calculerPerpWallDist(RayResult result, int mapX, int mapY, double joueurX, double joueurY) {
        if (!result.side) {
            return (mapX - joueurX + (1 - result.stepX) / 2.0) / result.rayDirX;
        } else {
            return (mapY - joueurY + (1 - result.stepY) / 2.0) / result.rayDirY;
        }
    }

    /**
     * Dessiner une colonne de mur pour un rayon
     */
    private void dessinerColonneMur(int rayIndex, int screenWidth, int screenHeight, RayResult result,
                                     double joueurX, double joueurY) {
        int lineHeight = (result.perpWallDist > 0) ? (int) (screenHeight / result.perpWallDist) : screenHeight;

        int drawStartRaw = -lineHeight / 2 + screenHeight / 2;
        int drawEndRaw = lineHeight / 2 + screenHeight / 2;
        int drawStart = Math.max(0, drawStartRaw);
        int drawEnd = Math.min(screenHeight - 1, drawEndRaw);

        int x1 = (rayIndex * screenWidth) / NUM_RAYS;
        int x2 = ((rayIndex + 1) * screenWidth) / NUM_RAYS;

        int texY = calculerTexY(result, joueurX, joueurY);

        for (int screenX = x1; screenX < x2 && screenX < screenWidth; screenX++) {
            zBuffer[screenX] = result.perpWallDist;
            dessinerPixelsColonne(screenX, screenWidth, drawStart, drawEnd, drawStartRaw, drawEndRaw, texY, result.side);
        }
    }

    private int calculerTexY(RayResult result, double joueurX, double joueurY) {
        double wallY = result.side
            ? joueurX + result.perpWallDist * result.rayDirX
            : joueurY + result.perpWallDist * result.rayDirY;
        wallY -= Math.floor(wallY);

        int texY = (int) (wallY * texHeight);
        if ((!result.side && result.rayDirX > 0) || (result.side && result.rayDirY < 0)) {
            texY = texHeight - texY - 1;
        }
        return Math.clamp(texY, 0, texHeight - 1);
    }

    private void dessinerPixelsColonne(int screenX, int screenWidth, int drawStart, int drawEnd,
                                        int drawStartRaw, int drawEndRaw, int texY, boolean side) {
        if (wallTexturePixels != null) {
            dessinerColonneTexturee(screenX, screenWidth, drawStart, drawEnd, drawStartRaw, drawEndRaw, texY, side);
        } else {
            dessinerColonneCouleur(screenX, screenWidth, drawStart, drawEnd, side);
        }
    }

    private void dessinerColonneTexturee(int screenX, int screenWidth, int drawStart, int drawEnd,
                                          int drawStartRaw, int drawEndRaw, int texY, boolean side) {
        for (int y = drawStart; y <= drawEnd; y++) {
            double d = (double) (y - drawStartRaw) / (double) (drawEndRaw - drawStartRaw);
            int texX = Math.clamp((int) (d * texWidth), 0, texWidth - 1);
            int color = wallTexturePixels[texY * texWidth + texX];

            if (side) {
                color = assombrirCouleur(color);
            }
            screenPixels[y * screenWidth + screenX] = color;
        }
    }

    private void dessinerColonneCouleur(int screenX, int screenWidth, int drawStart, int drawEnd, boolean side) {
        int color = side ? WALL_COLOR_DARK : WALL_COLOR_LIGHT;
        for (int y = drawStart; y <= drawEnd; y++) {
            screenPixels[y * screenWidth + screenX] = color;
        }
    }

    private int assombrirCouleur(int color) {
        int r = ((color >> 16) & 0xFF) >> 1;
        int g = ((color >> 8) & 0xFF) >> 1;
        int b = (color & 0xFF) >> 1;
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private void dessinerSprites(Graphics g, int screenWidth, int screenHeight,
                                  double joueurX, double joueurY, double joueurAngle, double fov) {

        List<Sprite> sortedSprites = trierSpritesParDistance(joueurX, joueurY);
        Graphics2D g2d = (Graphics2D) g;

        for (Sprite sprite : sortedSprites) {
            dessinerUnSprite(g2d, sprite, screenWidth, screenHeight, joueurX, joueurY, joueurAngle, fov);
        }
    }

    private List<Sprite> trierSpritesParDistance(double joueurX, double joueurY) {
        List<Sprite> sortedSprites = new ArrayList<>(sprites);
        sortedSprites.sort((a, b) -> {
            double distA = (a.getX() - joueurX) * (a.getX() - joueurX) + (a.getY() - joueurY) * (a.getY() - joueurY);
            double distB = (b.getX() - joueurX) * (b.getX() - joueurX) + (b.getY() - joueurY) * (b.getY() - joueurY);
            return Double.compare(distB, distA);
        });
        return sortedSprites;
    }

    private void dessinerUnSprite(Graphics2D g2d, Sprite sprite, int screenWidth, int screenHeight,
                                   double joueurX, double joueurY, double joueurAngle, double fov) {
        //position relative du sprite par rapport au joueur
        double spriteX = sprite.getX() - joueurX;
        double spriteY = sprite.getY() - joueurY;

        //calculer l'angle vers le sprite
        double angleDiff = normaliserAngle(Math.atan2(spriteY, spriteX) - joueurAngle);

        //verifier si le sprite est dans le FOV
        if (Math.abs(angleDiff) > fov / 2 + 0.2) return;

        //distance au sprite
        double spriteDistance = Math.sqrt(spriteX * spriteX + spriteY * spriteY);
        if (spriteDistance < 0.1) return; // Trop proche

        //position X sur l'ecran
        int spriteScreenX = (int) ((0.5 + angleDiff / fov) * screenWidth);

        //taille du sprite à l'ecran (carré)
        int spriteSize = (int) (screenHeight / spriteDistance);

        int drawStartY = screenHeight / 2 - spriteSize / 2;
        int drawStartX = spriteScreenX - spriteSize / 2;
        int drawEndX = spriteScreenX + spriteSize / 2;

        //verifier si au moins une partie du sprite est visible (devant les murs)
        if (!isSpriteVisible(spriteDistance, drawStartX, drawEndX, screenWidth)) return;

        //dessiner le sprite avec clipping par colonne
        dessinerSpriteImage(g2d, sprite, spriteDistance, spriteSize, drawStartX, drawStartY, drawEndX, screenWidth);

        //dessiner le pseudo au-dessus du sprite
        dessinerPseudo(g2d, sprite.getPlayerName(), spriteScreenX, drawStartY);
    }

    private double normaliserAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private boolean isSpriteVisible(double spriteDistance, int drawStartX, int drawEndX, int screenWidth) {
        for (int stripe = Math.max(0, drawStartX); stripe < Math.min(screenWidth, drawEndX); stripe++) {
            if (spriteDistance < zBuffer[stripe]) {
                return true;
            }
        }
        return false;
    }

    private void dessinerSpriteImage(Graphics2D g2d, Sprite sprite, double spriteDistance,
                                      int spriteSize, int drawStartX, int drawStartY, int drawEndX, int screenWidth) {
        BufferedImage spriteImage = sprite.getImage();
        int imgWidth = spriteImage.getWidth();
        int imgHeight = spriteImage.getHeight();

        for (int stripe = Math.max(0, drawStartX); stripe < Math.min(screenWidth, drawEndX); stripe++) {
            if (spriteDistance < zBuffer[stripe]) {
                int texX = (stripe - drawStartX) * imgWidth / spriteSize;
                if (texX >= 0 && texX < imgWidth) {
                    g2d.drawImage(spriteImage,
                            stripe, drawStartY,
                            stripe + 1, drawStartY + spriteSize,
                            texX, 0,
                            texX + 1, imgHeight,
                            null);
                }
            }
        }
    }

    private void dessinerPseudo(Graphics2D g2d, String playerName, int spriteScreenX, int drawStartY) {
        if (playerName == null || playerName.isEmpty()) return;

        String displayName = truncateName(playerName);
        g2d.setFont(new Font(FONT_ARIAL, Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(displayName);
        int textX = spriteScreenX - textWidth / 2;
        int textY = drawStartY - 10;

        //fond gris style pseudo Minecraft
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(textX - 4, textY - fm.getAscent(), textWidth + 8, fm.getHeight() + 4);

        g2d.setColor(Color.WHITE);
        g2d.drawString(displayName, textX, textY);
    }

    private void dessinerLogs(Graphics g) {
        //supprimer les logs expirés
        logMessages.removeIf(LogMessage::isExpired);

        if (logMessages.isEmpty()) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font(FONT_ARIAL, Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();

        int y = 30;
        for (LogMessage log : logMessages) {
            //petit fondu des logs avant disparition pour le style
            long age = System.currentTimeMillis() - log.timestamp;
            float alpha = 1.0f;
            if (age > LOG_DURATION_MS - 1000) {
                alpha = (LOG_DURATION_MS - age) / 1000.0f;
            }

            g2d.setColor(new Color(0, 0, 0, (int)(150 * alpha)));
            g2d.fillRect(10, y - fm.getAscent(), fm.stringWidth(log.text) + 10, fm.getHeight() + 4);

            Color textColor = new Color(
                log.color.getRed(),
                log.color.getGreen(),
                log.color.getBlue(),
                (int)(255 * alpha)
            );
            g2d.setColor(textColor);
            g2d.drawString(log.text, 15, y);

            y += fm.getHeight() + 8;
        }
    }

    /**
     * Dessiner le scoreboard (tableau des joueurs) au centre de l'écran
     */
    private void dessinerScoreboard(Graphics g, int screenWidth, int screenHeight) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dimensions du tableau
        int tableWidth = 400;
        int rowHeight = 40;
        int headerHeight = 50;
        int tableHeight = headerHeight + (playerList.size() * rowHeight) + 20;

        // Position centrée
        int tableX = (screenWidth - tableWidth) / 2;
        int tableY = (screenHeight - tableHeight) / 2;

        // Fond semi-transparent
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(tableX, tableY, tableWidth, tableHeight, 20, 20);

        // Bordure
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(tableX, tableY, tableWidth, tableHeight, 20, 20);

        // Titre
        g2d.setFont(new Font(FONT_ARIAL, Font.BOLD, 24));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String title = "JOUEURS EN LIGNE";
        int titleWidth = fmTitle.stringWidth(title);
        g2d.setColor(Color.WHITE);
        g2d.drawString(title, tableX + (tableWidth - titleWidth) / 2, tableY + 35);

        // Ligne de séparation sous le titre
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawLine(tableX + 20, tableY + headerHeight, tableX + tableWidth - 20, tableY + headerHeight);

        // Liste des joueurs
        g2d.setFont(new Font(FONT_ARIAL, Font.PLAIN, 18));

        int y = tableY + headerHeight + 30;
        int index = 1;

        for (String playerName : playerList) {
            // Indicateur pour le joueur local
            boolean isLocal = playerName.equals(localPlayerName);
            Color playerColor = getPlayerColor(playerName);

            // Numéro du joueur
            g2d.setColor(new Color(150, 150, 150));
            g2d.drawString(String.valueOf(index) + ".", tableX + 30, y);

            // Icône joueur (petit cercle coloré)
            g2d.setColor(playerColor);
            g2d.fillOval(tableX + 60, y - 12, 15, 15);

            // Nom du joueur
            // Version plus claire de la couleur pour le texte
            int red = Math.min(playerColor.getRed() + 55, 255);
            int green = Math.min(playerColor.getGreen() + 55, 255);
            int blue = Math.min(playerColor.getBlue() + 55, 255);
            Color textColor = new Color(red, green, blue);
            g2d.setColor(textColor);

            if (isLocal) {
                g2d.drawString(truncateName(playerName) + " (vous)", tableX + 85, y);
            } else {
                g2d.drawString(truncateName(playerName), tableX + 85, y);
            }

            y += rowHeight;
            index++;
        }
    }

    //une methode qui permet de rendre une image avec un objet Map et un objet Joueur
    public void render() {
        //autoriser le dessin
        render = true;
        panelDessin.repaint();
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public void setJoueur(Joueur joueur) {
        this.joueur = joueur;
    }
}
