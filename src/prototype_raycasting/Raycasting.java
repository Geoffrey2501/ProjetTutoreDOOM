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

public class Raycasting extends JFrame {

    private JPanel panelDessin;
    private boolean render;
    private Map map;
    private Joueur joueur;
    private int FOV = 60;
    private int NUM_RAYS = 1000;

    private List<Sprite> sprites = new CopyOnWriteArrayList<>();
    private double[] zBuffer;

    // Texture du mur
    private BufferedImage wallTexture;
    private int[] wallTexturePixels; // Pixels de la texture en tableau pour accès rapide
    private int texWidth = 64;
    private int texHeight = 64;

    // Buffer de rendu pour optimisation
    private BufferedImage screenBuffer;
    private int[] screenPixels;
    private int lastScreenWidth = 0;
    private int lastScreenHeight = 0;

    // Logs à afficher en haut à gauche
    private List<LogMessage> logMessages = new CopyOnWriteArrayList<>();
    private static final int MAX_LOGS = 5;
    private static final long LOG_DURATION_MS = 5000; // 5 secondes

    // Scoreboard (tableau des joueurs)
    private boolean showScoreboard = false;
    private List<String> playerList = new CopyOnWriteArrayList<>();
    private String localPlayerName = "";

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
        int colorIndex = Math.abs(playerName.hashCode()) % PLAYER_COLORS.length;
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
                wallTexture = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = wallTexture.createGraphics();
                g2d.drawImage(loaded, 0, 0, null);
                g2d.dispose();

                // Extraire les pixels dans un tableau pour accès ultra-rapide
                wallTexturePixels = new int[texWidth * texHeight];
                wallTexture.getRGB(0, 0, texWidth, texHeight, wallTexturePixels, 0, texWidth);

                System.out.println("Texture du mur chargée: " + texWidth + "x" + texHeight);
            } else {
                System.err.println("Fichier texture non trouvé: assets/wall.png - Utilisation des couleurs par défaut");
                wallTexture = null;
                wallTexturePixels = null;
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la texture: " + e.getMessage());
            wallTexture = null;
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
        //on code le raycasting ici
        //on utilise DDA pour trouver les murs

        boolean[][] mapData = map.getMap();
        int mapWidth = map.getWIDTH();
        int mapHeight = map.getHEIGHT();

        double joueurX = joueur.getX();
        double joueurY = joueur.getY();
        double joueurAngle = joueur.getAngle();

        int screenWidth = panelDessin.getWidth();
        int screenHeight = panelDessin.getHeight();

        if (screenWidth <= 0 || screenHeight <= 0) return;

        // Initialiser le buffer
        initScreenBuffer(screenWidth, screenHeight);

        // Initialiser le z-buffer
        zBuffer = new double[screenWidth];

        // Couleurs pour le ciel et le sol
        int skyColor = new Color(135, 206, 235).getRGB();
        int floorColor = new Color(105, 105, 105).getRGB();

        // Remplir le ciel et le sol dans le buffer
        int halfHeight = screenHeight / 2;
        for (int y = 0; y < screenHeight; y++) {
            int color = (y < halfHeight) ? skyColor : floorColor;
            int rowStart = y * screenWidth;
            for (int x = 0; x < screenWidth; x++) {
                screenPixels[rowStart + x] = color;
            }
        }

        //FOV (champ de vision) en radians - utilise le parametre FOV
        double fov = Math.toRadians(FOV); // Conversion des degrés en radians
        int numRays = NUM_RAYS; // Utilise le parametre NUM_RAYS

        // Couleurs par défaut pour les murs (sans texture)
        int wallColorLight = new Color(200, 100, 0).getRGB();
        int wallColorDark = new Color(150, 75, 0).getRGB();

        //lancer les rayons
        for (int i = 0; i < numRays; i++) {
            //calculer l'angle du rayon
            double rayAngle = joueurAngle - fov / 2 + (fov * i / numRays);

            //direction du rayon
            double rayDirX = Math.cos(rayAngle);
            double rayDirY = Math.sin(rayAngle);

            //DDA Algorithm
            double deltaDistX = Math.abs(1 / rayDirX);
            double deltaDistY = Math.abs(1 / rayDirY);

            int mapX = (int) joueurX;
            int mapY = (int) joueurY;

            double sideDistX, sideDistY;
            int stepX, stepY;

            //determiner la direction du pas et la distance initiale
            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (joueurX - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - joueurX) * deltaDistX;
            }

            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (joueurY - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - joueurY) * deltaDistY;
            }

            //effectuer le DDA
            boolean hit = false;
            boolean side = false; //false = cote X, true = cote Y

            while (!hit) {
                //avancer d'une case
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = false;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = true;
                }

                //verifier si on a touche un mur
                if (mapX < 0 || mapX >= mapWidth || mapY < 0 || mapY >= mapHeight) {
                    hit = true;
                } else if (mapData[mapY][mapX]) {
                    hit = true;
                }
            }

            //calculer la distance perpendiculaire au mur (pour éviter l'effet fish-eye)
            double perpWallDist;
            if (!side) {
                perpWallDist = (mapX - joueurX + (1 - stepX) / 2.0) / rayDirX;
            } else {
                perpWallDist = (mapY - joueurY + (1 - stepY) / 2.0) / rayDirY;
            }

            // Calculer la hauteur du mur à l'écran
            int lineHeight;
            if (perpWallDist > 0) {
                lineHeight = (int) (screenHeight / perpWallDist);
            } else {
                lineHeight = screenHeight;
            }

            //calculer les positions de début et fin du mur (valeurs non clampées pour le calcul de texture)
            int drawStartRaw = -lineHeight / 2 + screenHeight / 2;
            int drawEndRaw = lineHeight / 2 + screenHeight / 2;

            // Valeurs clampées pour le dessin réel
            int drawStart = Math.max(0, drawStartRaw);
            int drawEnd = Math.min(screenHeight - 1, drawEndRaw);

            //calculer les positions de début et fin du mur
            if (drawStart < 0) drawStart = 0;

            if (drawEnd >= screenHeight) drawEnd = screenHeight - 1;

            //calculer la position et largeur de chaque colonne pour éviter les gaps
            int x1 = (i * screenWidth) / numRays;
            int x2 = ((i + 1) * screenWidth) / numRays;
            int rayWidth = x2 - x1;

            // calculer la coordonnée Y de la texture
            double wallY;
            if (!side) {
                wallY = joueurY + perpWallDist * rayDirY;
            } else {
                wallY = joueurX + perpWallDist * rayDirX;
            }
            wallY -= Math.floor(wallY); // Garder uniquement la partie décimale

            // calculer la coordonnée Y dans la texture
            int texY = (int)(wallY * texHeight);
            if ((!side && rayDirX > 0) || (side && rayDirY < 0)) {
                texY = texHeight - texY - 1;
            }
            texY = Math.max(0, Math.min(texHeight - 1, texY));


            //dessiner les colonnes du mur
            for (int screenX = x1; screenX < x2 && screenX < screenWidth; screenX++) {
                zBuffer[screenX] = perpWallDist;

                if (wallTexturePixels != null) {
                    for (int y = drawStart; y <= drawEnd; y++) {
                        // calculer la position relative dans le mur (0.0 à 1.0)
                        double d = (double)(y - drawStartRaw) / (double)(drawEndRaw - drawStartRaw);

                        // convertir en coordonnée de texture
                        int texX = (int)(d * texWidth);
                        if (texX < 0) texX = 0;
                        if (texX >= texWidth) texX = texWidth - 1;

                        int color = wallTexturePixels[texY * texWidth + texX];

                        // assombrir les côtés Y
                        if (side) {
                            int r = ((color >> 16) & 0xFF) >> 1;
                            int gCol = ((color >> 8) & 0xFF) >> 1;
                            int b = (color & 0xFF) >> 1;
                            color = (0xFF << 24) | (r << 16) | (gCol << 8) | b;
                        }

                        screenPixels[y * screenWidth + screenX] = color;
                    }
                } else {
                    //sans texture
                    int color = side ? wallColorDark : wallColorLight;

                    for (int y = drawStart; y <= drawEnd; y++) {
                        screenPixels[y * screenWidth + screenX] = color;
                    }
                }
            }
        }

        //dessiner le buffer à l'écran
        g.drawImage(screenBuffer, 0, 0, null);

        //dessiner les sprites (utilise Graphics classique car plus complexe)
        dessinerSprites(g, screenWidth, screenHeight, joueurX, joueurY, joueurAngle, fov);

        //dessiner les logs en haut à gauche
        dessinerLogs(g);

        //dessiner le scoreboard si Tab est pressé
        if (showScoreboard) {
            dessinerScoreboard(g, screenWidth, screenHeight);
        }
    }

    private void dessinerSprites(Graphics g, int screenWidth, int screenHeight,
                                  double joueurX, double joueurY, double joueurAngle, double fov) {

        List<Sprite> sortedSprites = new ArrayList<>(sprites);
        sortedSprites.sort((a, b) -> {
            double distA = (a.getX() - joueurX) * (a.getX() - joueurX) + (a.getY() - joueurY) * (a.getY() - joueurY);
            double distB = (b.getX() - joueurX) * (b.getX() - joueurX) + (b.getY() - joueurY) * (b.getY() - joueurY);
            return Double.compare(distB, distA);
        });

        Graphics2D g2d = (Graphics2D) g;

        for (Sprite sprite : sortedSprites) {
            //position relative du sprite par rapport au joueur
            double spriteX = sprite.getX() - joueurX;
            double spriteY = sprite.getY() - joueurY;

            //calculer l'angle vers le sprite
            double spriteAngle = Math.atan2(spriteY, spriteX);
            double angleDiff = spriteAngle - joueurAngle;

            //normaliser l'angle entre -PI et PI
            while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
            while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

            //verifier si le sprite est dans le FOV
            if (Math.abs(angleDiff) > fov / 2 + 0.2) continue;

            //distance au sprite
            double spriteDistance = Math.sqrt(spriteX * spriteX + spriteY * spriteY);
            if (spriteDistance < 0.1) continue; // Trop proche

            //position X sur l'ecran
            int spriteScreenX = (int) ((0.5 + angleDiff / fov) * screenWidth);

            //taille du sprite à l'ecran
            int spriteHeight = (int) (screenHeight / spriteDistance);
            int spriteWidth = spriteHeight; // Sprite carré

            int drawStartY = screenHeight / 2 - spriteHeight / 2;
            int drawStartX = spriteScreenX - spriteWidth / 2;
            int drawEndX = spriteScreenX + spriteWidth / 2;

            //verifier si au moins une partie du sprite est visible (devant les murs)
            boolean visible = false;
            for (int stripe = Math.max(0, drawStartX); stripe < Math.min(screenWidth, drawEndX); stripe++) {
                if (spriteDistance < zBuffer[stripe]) {
                    visible = true;
                    break;
                }
            }
            if (!visible) continue;

            //dessiner le sprite avec clipping par colonne
            //on dessine colonne par colonne mais avec drawImage (beaucoup plus rapide)
            BufferedImage spriteImage = sprite.getImage();
            int imgWidth = spriteImage.getWidth();
            int imgHeight = spriteImage.getHeight();

            for (int stripe = Math.max(0, drawStartX); stripe < Math.min(screenWidth, drawEndX); stripe++) {
                //verifier le z-buffer
                if (spriteDistance < zBuffer[stripe]) {
                    //calculer quelle colonne de texture dessiner
                    int texX = (stripe - drawStartX) * imgWidth / spriteWidth;
                    if (texX < 0 || texX >= imgWidth) continue;

                    //dessiner cette colonne du sprite
                    g2d.drawImage(spriteImage,
                            stripe, drawStartY,           //destination top-left
                            stripe + 1, drawStartY + spriteHeight, //destination bottom-right
                            texX, 0,                      //source top-left
                            texX + 1, imgHeight,          //source bottom-right
                            null);
                }
            }

            //dessiner le pseudo au dessus du sprite
            String playerName = sprite.getPlayerName();
            if (playerName != null && !playerName.isEmpty()) {
                String displayName = truncateName(playerName);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(displayName);
                int textX = spriteScreenX - textWidth / 2;
                int textY = drawStartY - 10;

                //fond gris style pseudo minecraft
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(textX - 4, textY - fm.getAscent(), textWidth + 8, fm.getHeight() + 4);

                g2d.setColor(Color.WHITE);
                g2d.drawString(displayName, textX, textY);
            }
        }
    }

    private void dessinerLogs(Graphics g) {
        //supprimer les logs expirés
        logMessages.removeIf(LogMessage::isExpired);

        if (logMessages.isEmpty()) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
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
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String title = "JOUEURS EN LIGNE";
        int titleWidth = fmTitle.stringWidth(title);
        g2d.setColor(Color.WHITE);
        g2d.drawString(title, tableX + (tableWidth - titleWidth) / 2, tableY + 35);

        // Ligne de séparation sous le titre
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawLine(tableX + 20, tableY + headerHeight, tableX + tableWidth - 20, tableY + headerHeight);

        // Liste des joueurs
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        FontMetrics fm = g2d.getFontMetrics();

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
            Color textColor = new Color(
                Math.min(255, playerColor.getRed() + 55),
                Math.min(255, playerColor.getGreen() + 55),
                Math.min(255, playerColor.getBlue() + 55)
            );
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
