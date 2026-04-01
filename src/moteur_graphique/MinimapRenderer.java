package moteur_graphique;

import entite.Joueur;
import entite.Sprite;
import game.GameNetworkAdapter;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Renderer pour afficher une minimap vue de haut.
 * Affiche:
 * - Le joueur local en vert
 * - Les autres joueurs en couleur
 * - Les murs selon le type de collision
 */
public class MinimapRenderer implements GameRenderer {

    private final Joueur joueur;
    private final CollisionStrategy collision;
    private final GameNetworkAdapter network;

    // Paramètres de zoom et dimensions
    private static final float ZOOM = 5.0f; // pixels par unité de map
    private static final int MINIMAP_WIDTH = 200;
    private static final int MINIMAP_HEIGHT = 200;
    private static final int PLAYER_DOT_RADIUS = 4;

    // Palette de couleurs
    private static final Color WALL_COLOR = new Color(100, 100, 100);
    private static final Color FLOOR_COLOR = new Color(30, 30, 30);
    private static final Color PLAYER_COLOR = new Color(0, 255, 0);
    private static final Color DIRECTION_LINE_COLOR = new Color(200, 200, 0);

    private static final Color[] REMOTE_PLAYER_COLORS = {
            new Color(255, 100, 100), new Color(255, 200, 0),
            new Color(200, 100, 255), new Color(255, 150, 50), new Color(100, 255, 255),
            new Color(255, 100, 200), new Color(150, 255, 150)
    };

    // Paramètre de grille pour la détection des murs
    // Plus petit = plus détaillé (mais plus lent)
    private static final double WALL_GRID_STEP = 0.15;

    public MinimapRenderer(Joueur joueur, CollisionStrategy collision, GameNetworkAdapter network) {
        this.joueur = joueur;
        this.collision = collision;
        this.network = network;
    }

    @Override
    public void render(Graphics g, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Position de la minimap en haut à droite
        int minimapX = width - MINIMAP_WIDTH - 10;
        int minimapY = 10;

        // Dessiner le fond de la minimap
        g2d.setColor(FLOOR_COLOR);
        g2d.fillRect(minimapX, minimapY, MINIMAP_WIDTH, MINIMAP_HEIGHT);

        // Bordure
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(minimapX, minimapY, MINIMAP_WIDTH, MINIMAP_HEIGHT);

        // Créer un Graphics2D clippé pour la minimap
        Shape oldClip = g2d.getClip();
        g2d.clipRect(minimapX, minimapY, MINIMAP_WIDTH, MINIMAP_HEIGHT);

        // Décaler l'origine au centre de la minimap
        g2d.translate(minimapX + MINIMAP_WIDTH / 2.0, minimapY + MINIMAP_HEIGHT / 2.0);

        // Dessiner les murs (on teste la collision à différents points)
        drawWalls(g2d);

        // Dessiner le joueur local
        drawLocalPlayer(g2d);

        // Dessiner les autres joueurs
        drawRemotePlayers(g2d);

        // Restaurer les paramètres graphiques
        g2d.setClip(oldClip);
        g2d.translate(-(minimapX + MINIMAP_WIDTH / 2.0), -(minimapY + MINIMAP_HEIGHT / 2.0));

        // Afficher le label
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Minimap", minimapX + 5, minimapY + MINIMAP_HEIGHT + 25);
    }

    /**
     * Dessine une représentation simplifiée des murs sur la minimap
     */
    private void drawWalls(Graphics2D g2d) {
        g2d.setColor(WALL_COLOR);

        float playerRadius = 0.3f;
        
        // La minimap affiche une zone de 200/ZOOM x 200/ZOOM unités map
        double mapSize = MINIMAP_WIDTH / ZOOM;
        
        for (double relX = -mapSize / 2; relX < mapSize / 2; relX += WALL_GRID_STEP) {
            for (double relY = -mapSize / 2; relY < mapSize / 2; relY += WALL_GRID_STEP) {
                // Coordonnées absolutes en map
                double mapX = joueur.getX() + relX;
                double mapY = joueur.getY() + relY;

                // Tester la collision à ce point
                if (collision.isColliding(mapX, mapY, playerRadius)) {
                    // Convertir en coordonnées d'écran
                    int screenX = (int)(relX * ZOOM);
                    int screenY = (int)(relY * ZOOM);
                    
                    // Dessiner les blocs de mur (un peu plus grands)
                    g2d.fillRect(screenX - 1, screenY - 1, 3, 3);
                }
            }
        }
    }

    /**
     * Dessine le joueur local sur la minimap
     */
    private void drawLocalPlayer(Graphics2D g2d) {
        // Cercle vert pour le joueur
        g2d.setColor(PLAYER_COLOR);
        g2d.fillOval(-PLAYER_DOT_RADIUS, -PLAYER_DOT_RADIUS, PLAYER_DOT_RADIUS * 2, PLAYER_DOT_RADIUS * 2);

        // Ligne indiquant la direction du regard
        double angle = joueur.getAngle();
        int lineLength = 15;
        int endX = (int) (lineLength * Math.cos(angle));
        int endY = (int) (lineLength * Math.sin(angle));

        g2d.setColor(DIRECTION_LINE_COLOR);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(0, 0, endX, endY);
    }

    /**
     * Dessine les autres joueurs sur la minimap
     */
    private void drawRemotePlayers(Graphics2D g2d) {
        Map<String, double[]> remotePlayers = network.getRemotePlayersPositions();
        int colorIndex = 0;

        for (Map.Entry<String, double[]> entry : remotePlayers.entrySet()) {
            double[] pos = entry.getValue();
            if (pos != null && pos.length >= 2) {
                double remoteX = pos[0];
                double remoteY = pos[1];

                // Convertir en coordonnées relatives au joueur local
                double relX = (remoteX - joueur.getX()) * ZOOM;
                double relY = (remoteY - joueur.getY()) * ZOOM;

                // Vérifier que c'est dans la zone de la minimap
                if (Math.abs(relX) < MINIMAP_WIDTH && Math.abs(relY) < MINIMAP_HEIGHT) {
                    Color playerColor = REMOTE_PLAYER_COLORS[colorIndex % REMOTE_PLAYER_COLORS.length];
                    g2d.setColor(playerColor);
                    g2d.fillOval((int) relX - PLAYER_DOT_RADIUS, (int) relY - PLAYER_DOT_RADIUS,
                            PLAYER_DOT_RADIUS * 2, PLAYER_DOT_RADIUS * 2);

                    // Petit cercle autour
                    g2d.setColor(new Color(playerColor.getRed(), playerColor.getGreen(), playerColor.getBlue(), 100));
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawOval((int) relX - PLAYER_DOT_RADIUS - 3, (int) relY - PLAYER_DOT_RADIUS - 3,
                            (PLAYER_DOT_RADIUS + 3) * 2, (PLAYER_DOT_RADIUS + 3) * 2);
                }

                colorIndex++;
            }
        }
    }


    @Override
    public void addSprite(Sprite sprite) {
        // La minimap n'affiche pas les sprites pour le moment
    }

    @Override
    public void removeSprite(Sprite sprite) {
        // La minimap n'affiche pas les sprites pour le moment
    }
}

