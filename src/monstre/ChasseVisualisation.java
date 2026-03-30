package monstre;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Visualisation du mode Chasse : monstre + joueur + RRT*.
 */
public class ChasseVisualisation extends JPanel {
    private final Map map;
    private final Monstre monstre;
    private final Joueur joueur;
    private List<Noeud> noeuds;
    private List<Noeud> chemin;

    // Cache fond statique (murs uniquement, l'arbre RRT change souvent)
    private BufferedImage wallCache;

    public ChasseVisualisation(Map map, Monstre monstre, Joueur joueur) {
        this.map = map;
        this.monstre = monstre;
        this.joueur = joueur;
        this.noeuds = new ArrayList<>();
        this.chemin = new ArrayList<>();

        this.setDoubleBuffered(true);
        this.setPreferredSize(new Dimension(map.getLargeur(), map.getHauteur()));
        this.setFocusable(true);
    }

    /**
     * Met à jour l'arbre RRT et le chemin optimal.
     */
    public void updateRRT(List<Noeud> noeuds, Noeud fin) {
        this.noeuds = noeuds != null ? noeuds : new ArrayList<>();
        this.chemin = reconstruireChemin(fin);
    }

    public void clearRRT() {
        this.noeuds = new ArrayList<>();
        this.chemin = new ArrayList<>();
    }

    public void render() {
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Fond : murs (cache statique)
        drawWalls(g2d);

        // 2. Arbre RRT (semi-transparent pour ne pas gêner)
        drawRRT(g2d);

        // 3. Chemin optimal
        drawChemin(g2d);

        // 4. Joueur
        drawJoueur(g2d);

        // 5. Monstre
        drawMonstre(g2d);
    }

    private void drawWalls(Graphics2D g2d) {
        if (wallCache == null || wallCache.getWidth() != getWidth() || wallCache.getHeight() != getHeight()) {
            if (getWidth() <= 0 || getHeight() <= 0) return;
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            wallCache = gc.createCompatibleImage(getWidth(), getHeight(), Transparency.OPAQUE);
            Graphics2D wg = wallCache.createGraphics();
            wg.setColor(Color.WHITE);
            wg.fillRect(0, 0, getWidth(), getHeight());
            wg.setColor(Color.BLACK);
            for (MurTabBoolean mur : map.getMurs()) {
                int x = Math.min(mur.x1, mur.x2);
                int y = Math.min(mur.y1, mur.y2);
                wg.fillRect(x, y, Math.abs(mur.x2 - mur.x1), Math.abs(mur.y2 - mur.y1));
            }
            wg.dispose();
        }
        g2d.drawImage(wallCache, 0, 0, null);
    }

    private void drawRRT(Graphics2D g2d) {
        if (noeuds == null || noeuds.isEmpty()) return;

        // Liens (très léger)
        g2d.setColor(new Color(200, 200, 200, 60));
        g2d.setStroke(new BasicStroke(1));
        for (Noeud n : noeuds) {
            if (n.getParent() != null) {
                g2d.drawLine(n.getX(), n.getY(), n.getParent().getX(), n.getParent().getY());
            }
        }
    }

    private void drawChemin(Graphics2D g2d) {
        if (chemin == null || chemin.isEmpty()) return;
        g2d.setColor(new Color(255, 80, 80, 150));
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < chemin.size() - 1; i++) {
            g2d.drawLine(chemin.get(i).getX(), chemin.get(i).getY(),
                    chemin.get(i + 1).getX(), chemin.get(i + 1).getY());
        }
    }

    private void drawJoueur(Graphics2D g2d) {
        double jx = joueur.getX();
        double jy = joueur.getY();

        // Corps bleu
        g2d.setColor(new Color(30, 120, 255));
        g2d.fill(new Ellipse2D.Double(jx - 8, jy - 8, 16, 16));

        // Contour
        g2d.setColor(new Color(10, 60, 180));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new Ellipse2D.Double(jx - 8, jy - 8, 16, 16));
    }

    private void drawMonstre(Graphics2D g2d) {
        double mx = monstre.getX();
        double my = monstre.getY();

        // Corps violet
        g2d.setColor(new Color(180, 0, 0));
        g2d.fill(new Ellipse2D.Double(mx - 10, my - 10, 20, 20));

        // Direction
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2));
        double rot = monstre.getRotation();
        g2d.drawLine((int) mx, (int) my, (int) (mx + Math.cos(rot) * 15), (int) (my + Math.sin(rot) * 15));
    }

    private List<Noeud> reconstruireChemin(Noeud fin) {
        List<Noeud> liste = new ArrayList<>();
        Noeud courant = fin;
        while (courant != null) {
            liste.add(0, courant);
            courant = courant.getParent();
        }
        return liste;
    }
}
