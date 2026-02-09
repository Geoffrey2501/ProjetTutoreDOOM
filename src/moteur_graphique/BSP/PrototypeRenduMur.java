package moteur_graphique.BSP;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class PrototypeRenduMur extends JFrame {
    double x0, y0, x1, y1; //mur
    String texture; //texture du mur
    double xj, yj; //joueur
    int fov; //champ de vision
    int largeurEcran, hauteurEcran; //dimensions de l'écran
    double angleVue; //angle de vue du joueur, en radians

    // Nombre de colonnes à rendre (résolution horizontale du rendu)
    private int numColumns;

    // Données de rendu calculées
    private double[] renderData;

    // Buffer de rendu optimisé
    private BufferedImage screenBuffer;
    private int[] screenPixels;
    private int lastScreenWidth = 0;
    private int lastScreenHeight = 0;

    // Couleurs pré-calculées (format RGB int)
    private static final int SKY_COLOR = new Color(50, 50, 80).getRGB();
    private static final int FLOOR_COLOR = new Color(80, 60, 40).getRGB();

    public PrototypeRenduMur(double x0, double y0, double x1, double y1, String texture,
                              double xj, double yj, double angleVue,
                              int fov, int largeurEcran, int hauteurEcran) {
        this(x0, y0, x1, y1, texture, xj, yj, angleVue, fov, largeurEcran, hauteurEcran, largeurEcran);
    }

    public PrototypeRenduMur(double x0, double y0, double x1, double y1, String texture,
                              double xj, double yj, double angleVue,
                              int fov, int largeurEcran, int hauteurEcran, int numColumns) {
        super("PrototypeRenduMur");
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.texture = texture;
        this.xj = xj;
        this.yj = yj;
        this.angleVue = angleVue;
        this.fov = fov;
        this.largeurEcran = largeurEcran;
        this.hauteurEcran = hauteurEcran;
        this.numColumns = numColumns;

        setSize(largeurEcran, hauteurEcran);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Ajouter le panel de dessin
        add(new MurPanel());
    }

    // Panel interne pour dessiner le mur
    private class MurPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;

            // Initialiser le buffer si nécessaire
            initScreenBuffer(width, height);

            // Remplir le fond (ciel + sol)
            dessinerFond(width, height);

            // Dessiner le mur dans le buffer
            if (renderData != null) {
                dessinerMur(width, height, renderData);
            }

            // Afficher le buffer à l'écran en une seule opération
            g.drawImage(screenBuffer, 0, 0, null);
        }

        private void initScreenBuffer(int width, int height) {
            if (screenBuffer == null || lastScreenWidth != width || lastScreenHeight != height) {
                screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                screenPixels = ((DataBufferInt) screenBuffer.getRaster().getDataBuffer()).getData();
                lastScreenWidth = width;
                lastScreenHeight = height;
            }
        }

        private void dessinerFond(int width, int height) {
            int halfHeight = height / 2;
            for (int y = 0; y < height; y++) {
                int color = (y < halfHeight) ? SKY_COLOR : FLOOR_COLOR;
                int rowStart = y * width;
                for (int x = 0; x < width; x++) {
                    screenPixels[rowStart + x] = color;
                }
            }
        }

        private void dessinerMur(int width, int height, double[] data) {
            double x_start = data[0];
            double x_end = data[1];
            double z_start = data[2];
            double z_end = data[3];

            int centreY = height / 2;
            double hauteurMur = 200.0; // hauteur "réelle" du mur

            // Précalculer 1/z pour interpolation correcte (pas de fisheye)
            double invZ_start = 1.0 / z_start;
            double invZ_end = 1.0 / z_end;

            // Largeur d'une colonne logique en pixels
            double columnWidth = (double) width / numColumns;

            // Déterminer les colonnes logiques à dessiner
            int colStart = Math.max(0, (int) (x_start / columnWidth));
            int colEnd = Math.min(numColumns - 1, (int) (x_end / columnWidth));

            for (int col = colStart; col <= colEnd; col++) {
                // Position centrale de cette colonne en pixels
                double xCenter = (col + 0.5) * columnWidth;

                // Vérifier que cette colonne est dans la plage du mur
                if (xCenter < x_start || xCenter > x_end) continue;

                // Interpolation en 1/z (perspective-correct)
                double t = (x_end != x_start) ? (xCenter - x_start) / (x_end - x_start) : 0;
                double invZ = invZ_start + t * (invZ_end - invZ_start);
                double z = 1.0 / invZ;

                // Hauteur projetée = hauteur réelle / profondeur
                double hauteurProjetee = hauteurMur / z;
                int demiHauteur = (int) (hauteurProjetee / 2);

                int yHaut = Math.max(0, centreY - demiHauteur);
                int yBas = Math.min(height - 1, centreY + demiHauteur);

                // Couleur qui s'assombrit avec la distance
                int intensite = (int) Math.max(50, Math.min(255, 255 - z * 20));
                int color = (intensite << 16) | ((intensite / 2) << 8) | (intensite / 3);

                // Calculer les bornes en pixels pour cette colonne
                int pixelXStart = (int) (col * columnWidth);
                int pixelXEnd = (int) Math.min(width - 1, (col + 1) * columnWidth - 1);

                // Écrire les pixels de la colonne dans le buffer
                for (int y = yHaut; y <= yBas; y++) {
                    int rowStart = y * width;
                    for (int x = pixelXStart; x <= pixelXEnd; x++) {
                        screenPixels[rowStart + x] = color;
                    }
                }
            }
        }
    }

    //on fait une methode qui fait le rendu d'un seul mur.
    public void rendreMur() {
        //cos et sin de l'angle de vue
        double cosA = Math.cos(angleVue);
        double sinA = Math.sin(angleVue);

        //transformer le mur en espace joueur
        double wx0 = x0 - xj;
        double wy0 = y0 - yj;
        double wx1 = x1 - xj;
        double wy1 = y1 - yj;

        //rotation inverse (monde -> caméra)
        double cx0 =  wx0 * cosA + wy0 * sinA;
        double cz0 = -wx0 * sinA + wy0 * cosA;
        double cx1 =  wx1 * cosA + wy1 * sinA;
        double cz1 = -wx1 * sinA + wy1 * cosA;

        // Rejet rapide (Si les deux points sont derrière le joueur)
        double epsilon = 0.1;
        if (cz0 < epsilon && cz1 < epsilon) {
            //on peut ignorer le mur
            System.out.println("Mur ignoré (derrière le joueur)");
            return;
        }

        // Clipping (Si un seul point est derrière)
        // Si point 0 derrière, on le coupe pour le ramener à z = 0.1
        if (cz0 < epsilon) {
            double t = (epsilon - cz0) / (cz1 - cz0);
            cz0 = epsilon;
            cx0 = cx0 + t * (cx1 - cx0);
        }

        if (cz1 < epsilon) {
            double t = (epsilon - cz1) / (cz0 - cz1);
            cz1 = epsilon;
            cx1 = cx1 + t * (cx0 - cx1);
        }

        //projection perspective
        double scale = (largeurEcran / 2.0) / Math.tan(Math.toRadians(fov) / 2.0);
        double sx0 = (cx0 / cz0) * scale + (largeurEcran / 2.0);
        double sx1 = (cx1 / cz1) * scale + (largeurEcran / 2.0);

        // Mise en ordre (gauche vers droite)
        // Il faut s'assurer que x_start < x_end pour la boucle de dessin
        double x_start, x_end, z_start, z_end;
        if (sx0 < sx1) {
            x_start = sx0;
            x_end = sx1;
            z_start = cz0;
            z_end = cz1;
        } else {
            x_start = sx1;
            x_end = sx0;
            z_start = cz1;
            z_end = cz0;
        }

        //profondeur du mur (painter)
        double depth = (cz0 + cz1) / 2.0;

        //stocker pour le painter's algorithm (ici, on envoie directement à une fonction paint le mur)
        System.out.println("Mur à rendre de x=" + x_start + " à x=" + x_end + " avec profondeur " + depth);

        renderData = new double[] { x_start, x_end, z_start, z_end, depth };
        repaint(); // Rafraîchir l'affichage
    }

    // Main pour tester
    public static void main(String[] args) {

        PrototypeRenduMur proto = new PrototypeRenduMur(
            -1, -1,   // x0, y0 du mur
            2, 3,    // x1, y1 du mur
            null,    // texture
            0, 0,    // position joueur
            0,       // angle (regarde vers x+)
            90,      // fov en degrés
            800, 600, // taille écran
                100     // nombre de colonnes à rendre
        );

        proto.setVisible(true);
        proto.rendreMur();
    }
}
