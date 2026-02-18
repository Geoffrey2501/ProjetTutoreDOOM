package moteur_graphique.BSP;

import entite.Joueur;
import entite.Sprite;
import game.GameConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Renderer {
    private BufferedImage buffer;

    public void renderWorld(Graphics g, int width, int height, List<Object[]> objects, Joueur joueur) {
        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height) {
            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D g2d = buffer.createGraphics();

        // 2. Qualité d'image : On active l'anti-aliasing et l'interpolation pour les objets lointains
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Effacer l'écran (Fond)
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        double fov = Math.toRadians(GameConfig.FOV);

        // 3. Dessin des objets (Ordre Back-to-Front fourni par BSPParcours)
        for (Object[] item : objects) {
            Object obj = item[0];

            if (obj instanceof FourPoints) {
                drawWall(g2d, (FourPoints) obj);
            } else if (obj instanceof Sprite) {
                drawSpriteAndPseudo(g2d, (Sprite) obj, width, height, joueur, fov);
            }
        }

        g2d.dispose();

        g.drawImage(buffer, 0, 0, null);
    }

    private void drawWall(Graphics2D g2d, FourPoints mur) {
        Polygon poly = new Polygon();
        poly.addPoint((int) mur.x0, (int) mur.y0);
        poly.addPoint((int) mur.x1, (int) mur.y1);
        poly.addPoint((int) mur.x2, (int) mur.y2);
        poly.addPoint((int) mur.x3, (int) mur.y3);

        g2d.setColor(Color.GRAY);
        g2d.fillPolygon(poly);

        g2d.setColor(Color.WHITE);
        g2d.drawPolygon(poly);
    }

    private void drawSpriteAndPseudo(Graphics2D g2d, Sprite sprite, int w, int h, Joueur j, double fov) {
        double dx = sprite.getX() - j.getX();
        double dy = sprite.getY() - j.getY();

        // Calcul de l'angle et distance
        double angleDiff = Math.atan2(dy, dx) - j.getAngle();
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        if (Math.abs(angleDiff) > fov) return;

        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.1 || dist > 5000) return;

        int spriteSize = (int) (h / dist);
        int screenX = (int) ((0.5 + angleDiff / fov) * w);
        int drawX = screenX - spriteSize / 2;
        int drawY = h / 2 - spriteSize / 2;

        // Dessin du Sprite
        g2d.drawImage(sprite.getImage(), drawX, drawY, spriteSize, spriteSize, null);

        // Dessin du Pseudo (si c'est un joueur)
        String name = sprite.getPlayerName();
        if (name != null && !name.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(name);
            int textX = screenX - textWidth / 2;
            int textY = drawY - 10;

            // Fond du pseudo (Style Minecraft)
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(textX - 4, textY - fm.getAscent(), textWidth + 8, fm.getHeight());

            g2d.setColor(Color.WHITE);
            g2d.drawString(name, textX, textY);
        }
    }
}