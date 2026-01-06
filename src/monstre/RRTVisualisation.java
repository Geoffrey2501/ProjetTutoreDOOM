package monstre;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class RRTVisualisation extends JPanel {
    private final Map map;
    private final List<Noeud> noeuds;
    private final List<Noeud> chemin;
    private final Noeud debut;
    private final Noeud fin;
    private Monstre monstre;
    private javax.swing.Timer animationTimer;

    public RRTVisualisation(Map map, List<Noeud> noeuds, Noeud debut, Noeud fin) {
        this.map = map;
        this.noeuds = noeuds;
        this.debut = debut;
        this.fin = fin;
        this.chemin = trouverChemin(debut, fin);
        setPreferredSize(new Dimension(map.getLargeur(), map.getHauteur()));
    }

    public RRTVisualisation(Map map, List<Noeud> noeuds, Noeud debut, Noeud fin, Monstre monstre) {
        this(map, noeuds, debut, fin);
        this.monstre = monstre;
    }

    public void startAnimation() {
        if (monstre == null) return;
        animationTimer = new javax.swing.Timer(16, e -> {  // ~60 FPS
            monstre.update();
            repaint();
            if (monstre.isArrived()) {
                animationTimer.stop();
                System.out.println("Monstre arrivé à destination !");
            }
        });
        animationTimer.start();
    }

    // Trouver le chemin du début à la fin via les parents (RRT*)
    private List<Noeud> trouverChemin(Noeud debut, Noeud fin) {
        List<Noeud> chemin = new ArrayList<>();
        if (fin == null || fin.getParent() == null) return chemin;

        Noeud courant = fin;
        while (courant != null) {
            chemin.add(0, courant);
            courant = courant.getParent();
        }
        return chemin;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fond blanc
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Dessiner les murs en noir (rectangles pleins)
        g2d.setColor(Color.BLACK);
        for (Mur mur : map.getMurs()) {
            int x = Math.min(mur.x1, mur.x2);
            int y = Math.min(mur.y1, mur.y2);
            int w = Math.abs(mur.x2 - mur.x1);
            int h = Math.abs(mur.y2 - mur.y1);
            g2d.fillRect(x, y, w, h);
        }

        // Dessiner l'arbre RRT* (via les parents) en gris
        g2d.setColor(new Color(180, 180, 180));
        g2d.setStroke(new BasicStroke(1));
        for (Noeud n : noeuds) {
            if (n.getParent() != null) {
                g2d.drawLine(n.getX(), n.getY(), n.getParent().getX(), n.getParent().getY());
            }
        }

        // Dessiner les nœuds (points bleus)
        g2d.setColor(new Color(100, 150, 255));
        for (Noeud n : noeuds) {
            g2d.fillOval(n.getX() - 3, n.getY() - 3, 6, 6);
        }

        // Dessiner le chemin optimal en rouge
        if (!chemin.isEmpty()) {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));
            for (int i = 0; i < chemin.size() - 1; i++) {
                Noeud a = chemin.get(i);
                Noeud b = chemin.get(i + 1);
                g2d.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
            }
        }

        // Point de départ (vert)
        if (debut != null) {
            g2d.setColor(Color.GREEN);
            g2d.fillOval(debut.getX() - 10, debut.getY() - 10, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Début", debut.getX() + 12, debut.getY() + 5);
        }

        // Point d'arrivée (orange)
        if (fin != null) {
            g2d.setColor(Color.ORANGE);
            g2d.fillOval(fin.getX() - 10, fin.getY() - 10, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Fin", fin.getX() + 12, fin.getY() + 5);
        }

        // Afficher le statut
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String statut = chemin.isEmpty() ? "Chemin NON trouvé" : "Chemin RRT* trouvé ! (" + chemin.size() + " nœuds)";
        g2d.drawString(statut, 10, 20);
        g2d.drawString("Nœuds explorés: " + noeuds.size(), 10, 40);

        // Afficher le coût du chemin
        if (!chemin.isEmpty() && fin != null) {
            g2d.drawString("Coût: " + String.format("%.1f", fin.getCout()), 10, 60);
        }

        // Dessiner le monstre avec Steering Behavior
        if (monstre != null) {
            drawMonstre(g2d);
        }
    }

    private void drawMonstre(Graphics2D g2d) {
        int mx = (int) monstre.getX();
        int my = (int) monstre.getY();
        double rotation = monstre.getRotation();

        // Dessiner le corps du monstre (cercle violet)
        g2d.setColor(new Color(128, 0, 128));
        g2d.fillOval(mx - 12, my - 12, 24, 24);

        // Dessiner la direction (flèche)
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(3));
        int arrowLength = 20;
        int arrowX = (int) (mx + Math.cos(rotation) * arrowLength);
        int arrowY = (int) (my + Math.sin(rotation) * arrowLength);
        g2d.drawLine(mx, my, arrowX, arrowY);

        // Dessiner le waypoint cible actuel
        Noeud currentWaypoint = monstre.getCurrentWaypoint();
        if (currentWaypoint != null) {
            g2d.setColor(new Color(255, 0, 255, 100));
            g2d.fillOval(currentWaypoint.getX() - 8, currentWaypoint.getY() - 8, 16, 16);
        }

        // Afficher la vitesse du monstre
        g2d.setColor(Color.BLACK);
        g2d.drawString(String.format("Vitesse: %.2f", monstre.getSpeed()), 10, 80);
        g2d.drawString(String.format("Waypoint: %d/%d", monstre.getWaypointIndex() + 1, monstre.getChemin().size()), 10, 100);
    }

    public static void afficher(Map map, List<Noeud> noeuds, Noeud debut, Noeud fin) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Visualisation RRT");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new RRTVisualisation(map, noeuds, debut, fin));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void afficherAvecMonstre(Map map, List<Noeud> noeuds, Noeud debut, Noeud fin, Monstre monstre) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Visualisation RRT* + Steering Behavior");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            RRTVisualisation panel = new RRTVisualisation(map, noeuds, debut, fin, monstre);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.startAnimation();
        });
    }
}

