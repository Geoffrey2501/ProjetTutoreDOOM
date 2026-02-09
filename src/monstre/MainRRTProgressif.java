package monstre;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main qui affiche la construction progressive de l'arbre RRT*.
 * Permet de visualiser en temps réel comment l'algorithme explore l'espace
 * et optimise les connexions pour trouver le chemin optimal.
 */
public class MainRRTProgressif extends JPanel {

    // Configuration de la map
    private final Map map;
    private final int largeur = 800;
    private final int hauteur = 800;

    // Paramètres RRT*
    private static final int MAX_ITERATIONS = 500;
    private static final int MAX_DISTANCE_POINT = 50;
    private static final int RAYON_RECHERCHE = 80;

    // Points de départ et d'arrivée
    private final Noeud debut;
    private final Noeud fin;

    // Liste des noeuds de l'arbre
    private final ArrayList<Noeud> noeuds;

    // Compteur d'itérations
    private int iteration = 0;

    // Chemin trouvé (mis à jour au fur et à mesure)
    private List<Noeud> cheminActuel;

    // Timer pour l'animation
    private Timer timer;

    // Dernier noeud ajouté (pour le surligner)
    private Noeud dernierNoeud = null;

    // Meilleur noeud vers la fin trouvé
    private Noeud meilleurVersLaFin = null;

    // État de l'animation (pause/arrêt)
    private boolean enPause = false;
    private boolean arrete = false;

    // Pour visualiser les rewirings (optimisations)
    private List<Noeud> noeudsRewires = new ArrayList<>();  // Noeuds qui viennent d'être réoptimisés
    private int compteurRewires = 0;  // Nombre total de rewires
    private int flashRewire = 0;  // Compteur pour l'effet de flash

    // Pour visualiser le point aléatoire généré
    private int[] pointAleatoire = null;  // Coordonnées du point aléatoire actuel
    private int flashPointAleatoire = 0;  // Compteur pour l'effet de flash du point

    public MainRRTProgressif() {
        // Initialisation de la map et des points
        this.map = new Map(largeur, hauteur);
        this.debut = new Noeud(50, 50);
        this.debut.setCout(0);
        this.fin = new Noeud(750, 750);
        this.noeuds = new ArrayList<>();
        this.noeuds.add(debut);
        this.cheminActuel = new ArrayList<>();

        setPreferredSize(new Dimension(largeur, hauteur));
        setBackground(Color.WHITE);

        // Permettre au panel de recevoir les événements clavier
        setFocusable(true);

        // Gestion des touches clavier
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        // Barre espace : pause/reprendre
                        togglePause();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        // Échap : arrêter complètement
                        arreterAnimation();
                        break;
                    case KeyEvent.VK_R:
                        // R : redémarrer
                        redemarrer();
                        break;
                }
            }
        });
    }

    /**
     * Met en pause ou reprend l'animation
     */
    private void togglePause() {
        if (timer == null) return;

        enPause = !enPause;
        if (enPause) {
            timer.stop();
            System.out.println("Animation en pause. Appuyez sur ESPACE pour reprendre.");
        } else {
            timer.start();
            System.out.println("Animation reprise.");
        }
        repaint();
    }

    /**
     * Arrête complètement l'animation
     */
    private void arreterAnimation() {
        if (timer != null) {
            timer.stop();
            arrete = true;
            System.out.println("Animation arrêtée. Appuyez sur R pour redémarrer.");
            repaint();
        }
    }

    /**
     * Redémarre l'animation depuis le début
     */
    private void redemarrer() {
        if (timer != null) {
            timer.stop();
        }

        // Réinitialiser les données
        noeuds.clear();
        debut.setCout(0);
        debut.setParent(null);
        noeuds.add(debut);
        fin.setParent(null);
        fin.setCout(Double.MAX_VALUE);
        cheminActuel.clear();
        iteration = 0;
        dernierNoeud = null;
        meilleurVersLaFin = null;
        enPause = false;
        arrete = false;
        noeudsRewires.clear();
        compteurRewires = 0;
        flashRewire = 0;
        pointAleatoire = null;
        flashPointAleatoire = 0;

        System.out.println("Animation redémarrée.");
        demarrerAnimation();
    }

    /**
     * Démarre l'animation progressive de RRT*
     */
    public void demarrerAnimation() {
        // Timer qui ajoute un noeud toutes les 250ms (plus lent pour mieux visualiser)
        timer = new Timer(250, e -> {
            if (iteration < MAX_ITERATIONS) {
                // Effectuer une itération de RRT*
                effectuerUneIteration();
                iteration++;

                // Mettre à jour le chemin actuel
                mettreAJourChemin();

                // Redessiner
                repaint();
            } else {
                // Arrêter l'animation
                timer.stop();
                System.out.println("RRT* terminé après " + iteration + " itérations");
                System.out.println("Nombre de noeuds: " + noeuds.size());
                if (fin.getParent() != null) {
                    System.out.println("Chemin trouvé! Coût: " + String.format("%.1f", fin.getCout()));
                } else {
                    System.out.println("Aucun chemin trouvé.");
                }
            }
        });
        timer.start();
    }

    /**
     * Effectue une seule itération de l'algorithme RRT*
     */
    private void effectuerUneIteration() {
        // 1. Générer un point aléatoire (avec biais vers la fin)
        int[] coordAleatoires = getCoordonneesAleatoires();

        // Sauvegarder le point aléatoire pour l'affichage
        pointAleatoire = coordAleatoires;
        flashPointAleatoire = 8;  // Flash pendant 8 frames

        // 2. Trouver le noeud le plus proche
        Noeud plusProche = trouverPlusProche(coordAleatoires[0], coordAleatoires[1]);

        // 3. Créer un nouveau noeud vers le point aléatoire
        Noeud nouveau = creerNoeudVers(plusProche, coordAleatoires[0], coordAleatoires[1]);

        // Si le noeud est invalide (dans un mur ou segment traverse un mur), on passe
        if (nouveau == null) {
            return;
        }

        // 4. Trouver le meilleur parent dans le voisinage (RRT*)
        Noeud meilleurParent = trouverMeilleurParent(nouveau);
        if (meilleurParent == null) {
            meilleurParent = plusProche;
        }

        // 5. Connecter au meilleur parent
        double coutNouveau = meilleurParent.getCout() + calculerDistance(meilleurParent, nouveau);
        nouveau.setParent(meilleurParent);
        nouveau.setCout(coutNouveau);
        nouveau.ajouterVoisin(meilleurParent);
        meilleurParent.ajouterVoisin(nouveau);
        noeuds.add(nouveau);

        // Sauvegarder le dernier noeud ajouté pour le surligner
        dernierNoeud = nouveau;

        // 6. Optimiser les connexions des noeuds voisins (rewiring RRT*)
        optimiserConnexionNoeuds(nouveau);

        // 7. Vérifier si on peut atteindre la fin
        double distanceFin = calculerDistance(nouveau, fin);
        if (distanceFin <= MAX_DISTANCE_POINT) {
            // Vérifier que le segment ne traverse pas un mur
            if (!map.traverseMur(nouveau.getX(), nouveau.getY(), fin.getX(), fin.getY())) {
                double coutViaNouveau = nouveau.getCout() + distanceFin;
                if (meilleurVersLaFin == null || coutViaNouveau < fin.getCout()) {
                    fin.setParent(nouveau);
                    fin.setCout(coutViaNouveau);
                    if (meilleurVersLaFin == null) {
                        noeuds.add(fin);
                        nouveau.ajouterVoisin(fin);
                        fin.ajouterVoisin(nouveau);
                    }
                    meilleurVersLaFin = nouveau;
                }
            }
        }
    }

    /**
     * Génère des coordonnées aléatoires avec un biais vers la fin
     */
    private int[] getCoordonneesAleatoires() {
        int randX, randY;
        if (Math.random() < 0.1) {  // 10% de chance d'aller vers la fin
            randX = fin.getX();
            randY = fin.getY();
        } else {
            do {
                randX = (int) (Math.random() * map.getLargeur());
                randY = (int) (Math.random() * map.getHauteur());
            } while (map.estDansMur(randX, randY));
        }
        return new int[] {randX, randY};
    }

    /**
     * Trouve le noeud le plus proche d'un point donné
     */
    private Noeud trouverPlusProche(int x, int y) {
        Noeud plusProche = null;
        double distanceMin = Double.MAX_VALUE;
        for (Noeud n : noeuds) {
            double distance = Math.sqrt(Math.pow(n.getX() - x, 2) + Math.pow(n.getY() - y, 2));
            if (distance < distanceMin) {
                distanceMin = distance;
                plusProche = n;
            }
        }
        return plusProche;
    }

    /**
     * Crée un nouveau noeud à partir d'un noeud existant vers un point cible
     */
    private Noeud creerNoeudVers(Noeud depuis, int versX, int versY) {
        double dx = versX - depuis.getX();
        double dy = versY - depuis.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        int newX, newY;
        if (distance <= MAX_DISTANCE_POINT) {
            newX = versX;
            newY = versY;
        } else {
            newX = (int) (depuis.getX() + (dx / distance) * MAX_DISTANCE_POINT);
            newY = (int) (depuis.getY() + (dy / distance) * MAX_DISTANCE_POINT);
        }

        // Vérifier si le nouveau noeud est dans un mur
        if (map.estDansMur(newX, newY)) {
            return null;
        }

        // Vérifier si le segment traverse un mur
        if (map.traverseMur(depuis.getX(), depuis.getY(), newX, newY)) {
            return null;
        }

        return new Noeud(newX, newY);
    }

    /**
     * RRT* : Trouve le meilleur parent parmi les noeuds voisins
     */
    private Noeud trouverMeilleurParent(Noeud nouveau) {
        Noeud meilleurParent = null;
        double meilleurCout = Double.MAX_VALUE;

        for (Noeud n : noeuds) {
            double distance = calculerDistance(n, nouveau);
            if (distance <= RAYON_RECHERCHE) {
                // Vérifier que le segment ne traverse pas un mur
                if (!map.traverseMur(n.getX(), n.getY(), nouveau.getX(), nouveau.getY())) {
                    double coutPotentiel = n.getCout() + distance;
                    if (coutPotentiel < meilleurCout) {
                        meilleurCout = coutPotentiel;
                        meilleurParent = n;
                    }
                }
            }
        }
        return meilleurParent;
    }

    /**
     * RRT* : Rewiring - optimise les connexions des noeuds voisins
     * @return true si au moins un rewiring a été effectué
     */
    private void optimiserConnexionNoeuds(Noeud nouveau) {
        noeudsRewires.clear();  // Réinitialiser la liste des rewires

        for (Noeud n : noeuds) {
            if (n != nouveau && n != debut) {
                double distance = calculerDistance(nouveau, n);
                if (distance <= RAYON_RECHERCHE) {
                    // Vérifier que le segment ne traverse pas un mur
                    if (!map.traverseMur(nouveau.getX(), nouveau.getY(), n.getX(), n.getY())) {
                        double nouveauCout = nouveau.getCout() + distance;
                        if (nouveauCout < n.getCout()) {
                            // Reconnecter n via nouveau (meilleur chemin trouvé)
                            n.setParent(nouveau);
                            n.setCout(nouveauCout);

                            // Ajouter à la liste des noeuds réoptimisés
                            noeudsRewires.add(n);
                            compteurRewires++;
                        }
                    }
                }
            }
        }

        // Si des rewires ont eu lieu, déclencher l'effet flash
        if (!noeudsRewires.isEmpty()) {
            flashRewire = 10;  // Flash pendant 10 frames
        }
    }

    /**
     * Calcule la distance euclidienne entre deux noeuds
     */
    private double calculerDistance(Noeud a, Noeud b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    /**
     * Met à jour le chemin actuel depuis le début jusqu'à la fin
     */
    private void mettreAJourChemin() {
        cheminActuel.clear();
        if (fin.getParent() == null) return;

        Noeud courant = fin;
        while (courant != null) {
            cheminActuel.add(0, courant);
            courant = courant.getParent();
        }
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
        for (MurTabBoolean mur : map.getMurs()) {
            int x = Math.min(mur.x1, mur.x2);
            int y = Math.min(mur.y1, mur.y2);
            int w = Math.abs(mur.x2 - mur.x1);
            int h = Math.abs(mur.y2 - mur.y1);
            g2d.fillRect(x, y, w, h);
        }

        // Dessiner l'arbre RRT* (via les parents) en gris clair
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(1));
        for (Noeud n : noeuds) {
            if (n.getParent() != null) {
                g2d.drawLine(n.getX(), n.getY(), n.getParent().getX(), n.getParent().getY());
            }
        }

        // Dessiner les noeuds (points bleus)
        g2d.setColor(new Color(100, 150, 255));
        for (Noeud n : noeuds) {
            g2d.fillOval(n.getX() - 3, n.getY() - 3, 6, 6);
        }

        // Dessiner le point aléatoire généré (en jaune avec effet flash)
        if (pointAleatoire != null && flashPointAleatoire > 0) {
            int alpha = (int) (255 * (flashPointAleatoire / 8.0));
            int taille = 6 + (8 - flashPointAleatoire);

            // Cercle jaune pour le point aléatoire
            g2d.setColor(new Color(255, 255, 0, alpha));
            g2d.fillOval(pointAleatoire[0] - taille, pointAleatoire[1] - taille, taille * 2, taille * 2);

            // Contour rouge
            g2d.setColor(new Color(255, 100, 0, alpha));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(pointAleatoire[0] - taille, pointAleatoire[1] - taille, taille * 2, taille * 2);

            // Ligne pointillée vers le noeud le plus proche (si existe)
            if (dernierNoeud != null && dernierNoeud.getParent() != null) {
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 5}, 0));
                g2d.setColor(new Color(255, 200, 0, alpha));
                g2d.drawLine(pointAleatoire[0], pointAleatoire[1],
                            dernierNoeud.getParent().getX(), dernierNoeud.getParent().getY());
            }

            // Décrémenter le compteur de flash
            flashPointAleatoire--;
        }

        // Surligner le dernier noeud ajouté en cyan
        if (dernierNoeud != null) {
            g2d.setColor(Color.CYAN);
            g2d.fillOval(dernierNoeud.getX() - 6, dernierNoeud.getY() - 6, 12, 12);
            // Dessiner le segment vers son parent en cyan aussi
            if (dernierNoeud.getParent() != null) {
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(dernierNoeud.getX(), dernierNoeud.getY(),
                            dernierNoeud.getParent().getX(), dernierNoeud.getParent().getY());
            }
        }

        // Dessiner les noeuds réoptimisés (rewiring) en magenta avec effet flash
        if (flashRewire > 0 && !noeudsRewires.isEmpty()) {
            // Effet de pulsation basé sur flashRewire
            int alpha = (int) (255 * (flashRewire / 10.0));
            g2d.setColor(new Color(255, 0, 255, alpha));
            g2d.setStroke(new BasicStroke(4));

            for (Noeud n : noeudsRewires) {
                // Dessiner un cercle plus grand autour du noeud réoptimisé
                int taille = 8 + (10 - flashRewire);
                g2d.fillOval(n.getX() - taille, n.getY() - taille, taille * 2, taille * 2);

                // Dessiner le nouveau segment optimisé
                if (n.getParent() != null) {
                    g2d.drawLine(n.getX(), n.getY(), n.getParent().getX(), n.getParent().getY());
                }
            }

            // Décrémenter le compteur de flash
            flashRewire--;
        }

        // Dessiner le chemin optimal en vert
        if (!cheminActuel.isEmpty()) {
            g2d.setColor(new Color(0, 200, 0));
            g2d.setStroke(new BasicStroke(3));
            for (int i = 0; i < cheminActuel.size() - 1; i++) {
                Noeud a = cheminActuel.get(i);
                Noeud b = cheminActuel.get(i + 1);
                g2d.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
            }
        }

        // Point de départ (vert foncé)
        g2d.setColor(new Color(0, 150, 0));
        g2d.fillOval(debut.getX() - 12, debut.getY() - 12, 24, 24);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString("D", debut.getX() - 4, debut.getY() + 4);

        // Point d'arrivée (orange)
        g2d.setColor(Color.ORANGE);
        g2d.fillOval(fin.getX() - 12, fin.getY() - 12, 24, 24);
        g2d.setColor(Color.BLACK);
        g2d.drawString("F", fin.getX() - 4, fin.getY() + 4);

        // Afficher les informations
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Itération: " + iteration + "/" + MAX_ITERATIONS, 10, 20);
        g2d.drawString("Noeuds: " + noeuds.size(), 10, 40);

        // Afficher le nombre de rewirings (optimisations)
        g2d.setColor(new Color(200, 0, 200));
        g2d.drawString("Rewirings: " + compteurRewires, 150, 40);

        // Statut du chemin
        if (fin.getParent() != null) {
            g2d.setColor(new Color(0, 150, 0));
            g2d.drawString("Chemin trouvé! Coût: " + String.format("%.1f", fin.getCout()), 10, 60);
        } else {
            g2d.setColor(Color.RED);
            g2d.drawString("Recherche en cours...", 10, 60);
        }

        // Légende
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        int legendeY = hauteur - 110;

        g2d.setColor(Color.CYAN);
        g2d.fillOval(10, legendeY, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Dernier noeud", 25, legendeY + 9);

        g2d.setColor(new Color(100, 150, 255));
        g2d.fillOval(10, legendeY + 15, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Noeuds explorés", 25, legendeY + 24);

        g2d.setColor(new Color(0, 200, 0));
        g2d.fillRect(10, legendeY + 33, 10, 4);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Chemin optimal actuel", 25, legendeY + 39);

        g2d.setColor(new Color(200, 200, 200));
        g2d.fillRect(10, legendeY + 48, 10, 4);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Arbre RRT*", 25, legendeY + 54);

        g2d.setColor(new Color(255, 0, 255));
        g2d.fillOval(10, legendeY + 63, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Rewiring (optimisation)", 25, legendeY + 72);

        g2d.setColor(new Color(255, 255, 0));
        g2d.fillOval(10, legendeY + 78, 10, 10);
        g2d.setColor(new Color(255, 100, 0));
        g2d.drawOval(10, legendeY + 78, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Point aléatoire", 25, legendeY + 87);

        // Afficher les instructions clavier
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString("ESPACE: Pause | ÉCHAP: Arrêter | R: Redémarrer", largeur - 250, 20);

        // Afficher l'état de pause ou d'arrêt
        if (enPause) {
            g2d.setColor(new Color(255, 200, 0, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            String pauseText = "PAUSE";
            int textWidth = g2d.getFontMetrics().stringWidth(pauseText);
            g2d.drawString(pauseText, (largeur - textWidth) / 2, hauteur / 2);
        } else if (arrete) {
            g2d.setColor(new Color(255, 0, 0, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            String arretText = "ARRÊTÉ";
            int textWidth = g2d.getFontMetrics().stringWidth(arretText);
            g2d.drawString(arretText, (largeur - textWidth) / 2, hauteur / 2);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            String instructions = "Appuyez sur R pour redémarrer";
            int instrWidth = g2d.getFontMetrics().stringWidth(instructions);
            g2d.drawString(instructions, (largeur - instrWidth) / 2, hauteur / 2 + 30);
        }
    }

    /**
     * Point d'entrée principal
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("RRT* - Construction Progressive");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            MainRRTProgressif panel = new MainRRTProgressif();
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Démarrer l'animation après un court délai
            Timer startTimer = new Timer(500, e -> {
                panel.demarrerAnimation();
                ((Timer) e.getSource()).stop();
            });
            startTimer.start();
        });
    }
}

