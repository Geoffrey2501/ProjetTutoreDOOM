package moteur_graphique.raycasting;

import moteur_graphique.GameRenderer; // Importer l'interface
import prototype_raycasting.Joueur;
import prototype_raycasting.Map;
import prototype_raycasting.Sprite;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Raycasting implements GameRenderer {

    private static final Logger LOGGER = Logger.getLogger(Raycasting.class.getName());

    private Map map;
    private Joueur joueur;
    private static final int FOV = 60;
    private static final int NUM_RAYS = 1000;

    private final List<Sprite> sprites = new CopyOnWriteArrayList<>();
    private double[] zBuffer;

    private int[] wallTexturePixels;
    private int texWidth = 64;
    private int texHeight = 64;

    // Buffer de rendu
    private BufferedImage screenBuffer;
    private int[] screenPixels;
    private int lastScreenWidth = 0;
    private int lastScreenHeight = 0;

    // Couleurs
    private static final int SKY_COLOR = new Color(135, 206, 235).getRGB();
    private static final int FLOOR_COLOR = new Color(105, 105, 105).getRGB();
    private static final int WALL_COLOR_LIGHT = new Color(200, 100, 0).getRGB();
    private static final int WALL_COLOR_DARK = new Color(150, 75, 0).getRGB();

    private static final String FONT_ARIAL = "Arial";

    // Struct interne
    private static class RayResult {
        double perpWallDist;
        boolean side;
        int stepX; int stepY;
        double rayDirX; double rayDirY;
    }

    public Raycasting(Map m, Joueur j) {
        this.map = m;
        this.joueur = j;
        loadWallTexture();
    }

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

    // --- IMPLEMENTATION DE L'INTERFACE ---

    @Override
    public void render(Graphics g, int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) return;

        // Préparer le buffer interne (Redimensionnement automatique si la fenêtre change)
        initScreenBuffer(screenWidth, screenHeight);

        // Reset Z-Buffer
        zBuffer = new double[screenWidth];

        // 1. Remplir le tableau de pixels (Ciel/Sol)
        dessinerFond(screenWidth, screenHeight);

        // 2. Raycasting (Murs)
        double fov = Math.toRadians(FOV);
        lancerRayons(screenWidth, screenHeight, fov);

        // 3. Copier les pixels calculés sur l'écran
        // C'est ici qu'on "donne" l'image à la fenêtre
        g.drawImage(screenBuffer, 0, 0, null);

        // 4. Sprites (doivent être dessinés par dessus l'image car ils ont de la transparence)
        // Note: Idéalement, les sprites devraient aussi écrire dans screenPixels,
        // mais utiliser Graphics2D ici est plus simple pour gérer la transparence PNG.
        dessinerSprites(g, screenWidth, screenHeight, joueur.getX(), joueur.getY(), joueur.getAngle(), fov);
    }

    private void initScreenBuffer(int width, int height) {
        if (screenBuffer == null || lastScreenWidth != width || lastScreenHeight != height) {
            screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            screenPixels = ((DataBufferInt) screenBuffer.getRaster().getDataBuffer()).getData();
            lastScreenWidth = width;
            lastScreenHeight = height;
        }
    }

    public void addSprite(Sprite sprite) { sprites.add(sprite); }
    public void removeSprite(Sprite sprite) { sprites.remove(sprite); }
}