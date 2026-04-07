package moteur_graphique.BSP;

import entite.Joueur;
import entite.Sprite;
import game.GameConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Renderer {
    private BufferedImage buffer;

    private static final Color SKY_COLOR   = new Color(135, 206, 235);
    private static final Color FLOOR_COLOR = new Color(105, 105, 105);

    public void renderWorld(Graphics g, int width, int height,
                            List<FourPoints> walls, List<Sprite> sprites,
                            Joueur joueur, double[] zBuffer) {
        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height) {
            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D g2d = buffer.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Ciel et sol
        int halfHeight = height / 2;
        g2d.setColor(SKY_COLOR);
        g2d.fillRect(0, 0, width, halfHeight);
        g2d.setColor(FLOOR_COLOR);
        g2d.fillRect(0, halfHeight, width, height - halfHeight);

        // Murs (Back-to-Front, algorithme du peintre)
        for (FourPoints fp : walls) {
            drawWall(g2d, fp);
        }

        // Sprites : triés du plus loin au plus proche, puis clippés colonne par colonne via z-buffer
        double fov = Math.toRadians(GameConfig.FOV);
        double jx = joueur.getX(), jy = joueur.getY();
        sprites.sort((a, b) -> {
            double dxa = a.getX() - jx, dya = a.getY() - jy;
            double dxb = b.getX() - jx, dyb = b.getY() - jy;
            return Double.compare(dxb * dxb + dyb * dyb, dxa * dxa + dya * dya);
        });
        for (Sprite sprite : sprites) {
            drawSpriteAndPseudo(g2d, sprite, width, height, joueur, fov, zBuffer);
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

    private void drawSpriteAndPseudo(Graphics2D g2d, Sprite sprite, int w, int h,
                                     Joueur j, double fov, double[] zBuffer) {
        double dx = sprite.getX() - j.getX();
        double dy = sprite.getY() - j.getY();

        double angleDiff = Math.atan2(dy, dx) - j.getAngle();
        while (angleDiff >  Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        if (Math.abs(angleDiff) > fov) return;

        // Profondeur caméra (même métrique que le z-buffer des murs)
        double cosA = Math.cos(j.getAngle());
        double sinA = Math.sin(j.getAngle());
        double spriteCameraZ = dx * cosA + dy * sinA;
        if (spriteCameraZ < 0.1) return;

        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 5000) return;

        int spriteSize = (int) (h / dist);
        if (spriteSize <= 0) return;

        int screenX = (int) ((0.5 + angleDiff / fov) * w);
        int drawX   = screenX - spriteSize / 2;
        int drawY   = h / 2   - spriteSize / 2;

        BufferedImage img = sprite.getImage();
        int imgW = img.getWidth();
        int imgH = img.getHeight();

        // Dessin par spans de colonnes consécutives visibles (évite un drawImage par colonne)
        boolean visible = false;
        int spanStart = -1;
        for (int col = drawX; col <= drawX + spriteSize; col++) {
            boolean draw = col < drawX + spriteSize
                    && col >= 0 && col < w
                    && spriteCameraZ < zBuffer[col];
            if (draw && spanStart < 0) {
                spanStart = col;
            } else if (!draw && spanStart >= 0) {
                // Fin du span : un seul drawImage pour toutes les colonnes consécutives
                int texX0 = (spanStart - drawX) * imgW / spriteSize;
                int texX1 = (col      - drawX) * imgW / spriteSize;
                texX0 = Math.max(0, Math.min(imgW, texX0));
                texX1 = Math.max(0, Math.min(imgW, texX1));
                if (texX1 > texX0) {
                    g2d.drawImage(img,
                            spanStart, drawY, col, drawY + spriteSize,
                            texX0, 0, texX1, imgH,
                            null);
                }
                visible = true;
                spanStart = -1;
            }
        }

        // Pseudo au-dessus du sprite (seulement si la colonne centrale passe le z-buffer)
        boolean centerVisible = screenX >= 0 && screenX < w && spriteCameraZ < zBuffer[screenX];
        if (visible && centerVisible) {
            String name = sprite.getPlayerName();
            if (name != null && !name.isEmpty()) {
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(name);
                int textX = screenX - textWidth / 2;
                int textY = drawY - 10;
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(textX - 4, textY - fm.getAscent(), textWidth + 8, fm.getHeight());
                g2d.setColor(Color.WHITE);
                g2d.drawString(name, textX, textY);
            }
        }
    }
}