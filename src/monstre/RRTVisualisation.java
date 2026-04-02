package monstre;

import entite.Joueur;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static monstre.Automate.Etat.*;

public class RRTVisualisation extends JPanel {
    private final Map map;
    private List<Noeud> noeuds;
    private List<Noeud> chemin;
    
    // Remplacer le monstre unique par une liste de monstres
    private List<Monstre> monstres = new ArrayList<>();

    // Joueurs à afficher
    private Joueur joueur1;
    private Joueur joueur2;

    // Points de patrouille à afficher
    private Noeud pointPatrouilleA;
    private Noeud pointPatrouilleB;

    // Marqueur de la cible (clic souris)
    private int cibleX = -1, cibleY = -1;
    private boolean afficherCible = false;

    // Cache pour ne pas redessiner le fond statique à chaque image (Optimisation)
    private BufferedImage backgroundCache;

    public RRTVisualisation(Map map, List<Monstre> monstres) {
        this.map = map;
        this.monstres = monstres;
        this.noeuds = new ArrayList<>();
        this.chemin = new ArrayList<>();

        // Configuration graphique
        this.setDoubleBuffered(true);
        this.setPreferredSize(new Dimension(map.getLargeur(), map.getHauteur()));
    }

    public RRTVisualisation(Map map, Monstre monstre) {
        this.map = map;
        this.monstres.add(monstre);
        this.noeuds = new ArrayList<>();
        this.chemin = new ArrayList<>();

        // Configuration graphique
        this.setDoubleBuffered(true);
        this.setPreferredSize(new Dimension(map.getLargeur(), map.getHauteur()));
    }

    public RRTVisualisation(Map map, List<Noeud> noeuds, Noeud debut, Noeud fin, Monstre monstre) {
        this.map = map;
        this.noeuds = noeuds;
        this.monstres.add(monstre);
        this.chemin = reconstruireChemin(fin);

        // Configuration graphique
        this.setDoubleBuffered(true);
        this.setPreferredSize(new Dimension(map.getLargeur(), map.getHauteur()));
    }

    /**
     * Met à jour le chemin affiché (peut être utilisé pour le monstre principal)
     */
    public void updateRRT(List<Noeud> noeuds, Noeud fin) {
        this.noeuds = noeuds;
        this.chemin = reconstruireChemin(fin);
        this.backgroundCache = null; // Invalider le cache
    }

    /**
     * Définit la position de la cible (clic souris) pour l'afficher.
     */
    public void setCible(int x, int y) {
        this.cibleX = x;
        this.cibleY = y;
        this.afficherCible = true;
        this.backgroundCache = null; // Invalider le cache
    }

    /**
     * Cache le marqueur de cible.
     */
    public void cacherCible() {
        this.afficherCible = false;
    }

    /**
     * Met à jour le chemin à afficher.
     */
    public void updateChemin(java.util.List<Noeud> nouveauChemin) {
        this.chemin = nouveauChemin;
        // Pas besoin d'invalider le cache car le chemin est dessiné dynamiquement
    }

    // Méthode appelée par le contrôleur pour mettre à jour l'affichage
    public void render() {
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 1. Dessiner le fond (Murs + Arbre RRT) - Utilise le cache
        drawStaticBackground(g2d);

        // 2. Dessiner le chemin actuel (Dynamique - en rouge)
        drawChemin(g2d);

        // 3. Dessiner les joueurs (Dynamique)
        drawJoueurs(g2d);

        // 4. Dessiner le monstre (Dynamique)
        if (monstres != null) {
            drawMonstre(g2d);
        }
    }

    /**
     * Dessine le chemin actuel du monstre.
     */
    private void drawChemin(Graphics2D g2d) {
        if (chemin != null && !chemin.isEmpty()) {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));
            for (int i = 0; i < chemin.size() - 1; i++) {
                g2d.drawLine(chemin.get(i).getX(), chemin.get(i).getY(),
                        chemin.get(i + 1).getX(), chemin.get(i + 1).getY());
            }
        }
    }

    private void drawStaticBackground(Graphics2D g2d) {
        // Si le cache n'existe pas ou si la fenêtre a changé de taille, on le recrée
        if (backgroundCache == null || backgroundCache.getWidth() != getWidth() || backgroundCache.getHeight() != getHeight()) {
            creerCacheFond();
        }
        g2d.drawImage(backgroundCache, 0, 0, null);
    }

    private void creerCacheFond() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        // Création d'une image compatible avec la carte graphique (VRAM)
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        backgroundCache = gc.createCompatibleImage(getWidth(), getHeight(), Transparency.OPAQUE);

        Graphics2D g = backgroundCache.createGraphics();

        // Fond blanc
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Murs
        g.setColor(Color.BLACK);
        for (MurTabBoolean mur : map.getMurs()) {
            int x = Math.min(mur.x1, mur.x2);
            int y = Math.min(mur.y1, mur.y2);
            g.fillRect(x, y, Math.abs(mur.x2 - mur.x1), Math.abs(mur.y2 - mur.y1));
        }

        // Arbre RRT et Noeuds
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (noeuds != null) {
            // Liens
            g.setColor(new Color(200, 200, 200));
            for (Noeud n : noeuds) {
                if (n.getParent() != null) {
                    g.drawLine(n.getX(), n.getY(), n.getParent().getX(), n.getParent().getY());
                }
            }
            // Points
            g.setColor(new Color(100, 150, 255));
            for (Noeud n : noeuds) {
                g.fillOval(n.getX() - 2, n.getY() - 2, 4, 4);
            }
        }


        // Marqueur de cible (croix verte)
        if (afficherCible && cibleX >= 0 && cibleY >= 0) {
            g.setColor(new Color(0, 200, 0));
            g.setStroke(new BasicStroke(3));
            int taille = 10;
            g.drawLine(cibleX - taille, cibleY - taille, cibleX + taille, cibleY + taille);
            g.drawLine(cibleX + taille, cibleY - taille, cibleX - taille, cibleY + taille);
            g.setColor(new Color(0, 200, 0, 80));
            g.fillOval(cibleX - 15, cibleY - 15, 30, 30);
        }
        g.dispose();
    }

    private void drawMonstre(Graphics2D g2d) {
        for (Monstre m : monstres) {
            if (m != null) {
                double x = m.getX();
                double y = m.getY();

                // Dessiner le champ de vision (rayon de détection)
                g2d.setColor(new Color(255, 0, 0, 30)); // Rouge très transparent
                int rayon = (int) m.getDistanceDetection();
                g2d.fillOval((int) x - rayon, (int) y - rayon, rayon * 2, rayon * 2);

                // Dessiner la zone d'alerte
                g2d.setColor(new Color(255, 200, 0, 15)); // Orange super transparent
                int rayonAlerte = 300;
                g2d.fillOval((int) x - rayonAlerte, (int) y - rayonAlerte, rayonAlerte * 2, rayonAlerte * 2);

                // Couleur en fonction de l'état
                Color couleurMonstre;
                Automate.Etat etat = m.getEtat();

                if (etat == Automate.Etat.POURSUITE) {
                    couleurMonstre = Color.RED; // Rouge si chasse
                } else if (etat == Automate.Etat.PATROUILLE) {
                    couleurMonstre = Color.ORANGE; // Orange si patrouille
                } else {
                    couleurMonstre = Color.GRAY; // Gris si attente
                }

                g2d.setColor(couleurMonstre);
                g2d.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));

                // Dessiner une ligne indiquant la direction
                g2d.setColor(Color.BLACK);
                double angleDirection = Math.toRadians(m.getRotation());
                int lineEndX = (int) (x + 15 * Math.cos(angleDirection));
                int lineEndY = (int) (y + 15 * Math.sin(angleDirection));
                g2d.drawLine((int) x, (int) y, lineEndX, lineEndY);

                // Texte de l'état
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString(etat != null ? etat.toString() : "N/A", (int) x - 15, (int) y - 10);
            }
        }
    }

    /**
     * Définit les joueurs à afficher sur la visualisation.
     */
    public void setJoueurs(Joueur joueur1, Joueur joueur2) {
        this.joueur1 = joueur1;
        this.joueur2 = joueur2;
    }

    /**
     * Définit un seul joueur à afficher.
     */
    public void setJoueur(Joueur joueur) {
        this.joueur1 = joueur;
        this.joueur2 = null;
    }

    /**
     * Définit les points de patrouille à afficher.
     */
    public void setPointsPatrouille(Noeud pointA, Noeud pointB) {
        this.pointPatrouilleA = pointA;
        this.pointPatrouilleB = pointB;
        this.backgroundCache = null; // Invalider le cache pour redessiner
    }

    /**
     * Dessine les joueurs sur la visualisation.
     */
    private void drawJoueurs(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Points de patrouille (A = cyan, B = orange)
        if (pointPatrouilleA != null) {
            int x = pointPatrouilleA.getX();
            int y = pointPatrouilleA.getY();
            g2d.setColor(Color.CYAN);
            g2d.fill(new Ellipse2D.Double(x - 10, y - 10, 20, 20));
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(new Ellipse2D.Double(x - 10, y - 10, 20, 20));
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("A", x - 4, y + 5);
        }

        if (pointPatrouilleB != null) {
            int x = pointPatrouilleB.getX();
            int y = pointPatrouilleB.getY();
            g2d.setColor(Color.ORANGE);
            g2d.fill(new Ellipse2D.Double(x - 10, y - 10, 20, 20));
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(new Ellipse2D.Double(x - 10, y - 10, 20, 20));
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("B", x - 4, y + 5);
        }

        // Joueur 1 (Rouge)
        if (joueur1 != null) {
            double x = joueur1.getX();
            double y = joueur1.getY();
            g2d.setColor(Color.RED);
            g2d.fill(new Ellipse2D.Double(x - 8, y - 8, 16, 16));
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("1", (int)x - 3, (int)y + 4);
        }

        // Joueur 2 (Bleu)
        if (joueur2 != null) {
            double x = joueur2.getX();
            double y = joueur2.getY();
            g2d.setColor(Color.BLUE);
            g2d.fill(new Ellipse2D.Double(x - 8, y - 8, 16, 16));
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("2", (int)x - 3, (int)y + 4);
        }

        // Afficher quelle cible est active (cercle autour)
        if (monstres != null && !monstres.isEmpty() && monstres.get(0).getTarget() != null) {
            Target target = monstres.get(0).getTarget();
            g2d.setColor(Color.GREEN);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval((int)target.getX() - 12, (int)target.getY() - 12, 24, 24);
        }
    }

    // Utilitaire pour transformer la liste chaînée de parents en liste simple
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

