package moteur_graphique.BSP;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.List;

public class Renderer {
    private BufferedImage buffer;
    private int bufferWidth;
    private int bufferHeight;

    /**
     * Rend la liste de murs (FourPoints) dans un buffer, puis affiche le buffer.
     */
    public void renderFourPointsList(Graphics g, int width, int height, List<FourPoints> murs) {
        //créer ou recréer le buffer si nécessaire
        if (buffer == null || bufferWidth != width || bufferHeight != height) {
            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            bufferWidth = width;
            bufferHeight = height;
        }

        //obtenir le Graphics2D du buffer
        Graphics2D g2d = buffer.createGraphics();

        //effacer le buffer
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        //dessiner chaque mur comme un polygone
        for (FourPoints mur : murs) {
            if(mur != null) {
            Polygon poly = new Polygon();
            //ordre des points : haut-gauche -> bas-gauche -> bas-droite -> haut-droite
            poly.addPoint((int) mur.x0, (int) mur.y0);
            poly.addPoint((int) mur.x1, (int) mur.y1);
            poly.addPoint((int) mur.x2, (int) mur.y2);
            poly.addPoint((int) mur.x3, (int) mur.y3);

            //remplir le polygone
            g2d.setColor(Color.GRAY);
            g2d.fillPolygon(poly);

            //dessiner le contour, juste pour debug et voir le decoupage des segments dans l'arbre bsp
            g2d.setColor(Color.WHITE);
            g2d.drawPolygon(poly);}
        }

        g2d.dispose();

        //rendre le buffer dans la fenêtre
        g.drawImage(buffer, 0, 0, null);
    }

    /**
     * Retourne le buffer actuel (utile si tu veux y accéder directement).
     */
    public BufferedImage getBuffer() {
        return buffer;
    }
}
