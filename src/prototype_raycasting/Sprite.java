package prototype_raycasting;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Sprite {
    private double x;
    private double y;
    private BufferedImage image;

    public Sprite(double x, double y, String imagePath) {
        this.x = x;
        this.y = y;
        try {
            this.image = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            this.image = createDefaultImage();
        }
    }

    public Sprite(double x, double y) {
        this.x = x;
        this.y = y;
        this.image = createDefaultImage();
    }

    private BufferedImage createDefaultImage() {
        //creer une image simple par défaut (cercle rouge sur fond transparent)
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.RED);
        g2d.fillOval(4, 4, 56, 56);
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillOval(20, 20, 10, 10); //oeil gauche
        g2d.fillOval(34, 20, 10, 10); //oeil droit
        g2d.dispose();
        return img;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public BufferedImage getImage() { return image; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
}
