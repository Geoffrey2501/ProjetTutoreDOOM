package prototype_raycasting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Raycasting extends JFrame {

    private JPanel panelDessin;
    private boolean render;
    private Map map;
    private Joueur joueur;
    private int FOV = 60;
    private int NUM_RAYS = 1000; //"definition" de la qualite du rendu

    private List<Sprite> sprites = new ArrayList<>();
    private double[] zBuffer; // Pour stocker les distances des murs

    public Raycasting(Map m, Joueur j) {
        //le titre de la fenetre
        super("Prototype Raycasting");

        this.map = m;
        this.joueur = j;

        //booleen pour ne pas rendre à l'initialisation du panel
        render = false;

        //créer le panel de dessin
        panelDessin = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(render) {
                    dessiner(g);
                }
            }
        };
        panelDessin.setBackground(Color.BLACK);
        add(panelDessin);

        //quitter l'application quand on ferme la fenetre
        WindowListener l = new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                System.exit(0);
            }
        };

        //ajouter le listener a la fenetre
        addWindowListener(l);
        setSize(1920, 1080);
        setVisible(true);
    }

    public void addSprite(Sprite sprite) {
        sprites.add(sprite);
    }

    public void removeSprite(Sprite sprite) {
        sprites.remove(sprite);
    }

    //methode qui effectue le dessin
    private void dessiner(Graphics g) {
        //on code le raycasting ici
        //on utilise DDA pour trouver les murs

        boolean[][] mapData = map.getMap();
        int mapWidth = map.getWIDTH();
        int mapHeight = map.getHEIGHT();

        double joueurX = joueur.getX();
        double joueurY = joueur.getY();
        double joueurAngle = joueur.getAngle();

        int screenWidth = panelDessin.getWidth();
        int screenHeight = panelDessin.getHeight();

        // Initialiser le z-buffer
        zBuffer = new double[screenWidth];

        //dessiner le ciel et le sol
        g.setColor(new Color(135, 206, 235)); // Bleu ciel
        g.fillRect(0, 0, screenWidth, screenHeight / 2);
        g.setColor(new Color(105, 105, 105)); // Gris sol
        g.fillRect(0, screenHeight / 2, screenWidth, screenHeight / 2);

        //FOV (champ de vision) en radians - utilise le parametre FOV
        double fov = Math.toRadians(FOV); // Conversion des degrés en radians
        int numRays = NUM_RAYS; // Utilise le parametre NUM_RAYS

        //lancer les rayons
        for (int i = 0; i < numRays; i++) {
            //calculer l'angle du rayon
            double rayAngle = joueurAngle - fov / 2 + (fov * i / numRays);

            //direction du rayon
            double rayDirX = Math.cos(rayAngle);
            double rayDirY = Math.sin(rayAngle);

            //DDA Algorithm
            double deltaDistX = Math.abs(1 / rayDirX);
            double deltaDistY = Math.abs(1 / rayDirY);

            int mapX = (int) joueurX;
            int mapY = (int) joueurY;

            double sideDistX, sideDistY;
            int stepX, stepY;

            //determiner la direction du pas et la distance initiale
            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (joueurX - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - joueurX) * deltaDistX;
            }

            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (joueurY - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - joueurY) * deltaDistY;
            }

            //effectuer le DDA
            boolean hit = false;
            boolean side = false; //false = cote X, true = cote Y

            while (!hit) {
                //avancer d'une case
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = false;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = true;
                }

                //verifier si on a touche un mur
                if (mapX < 0 || mapX >= mapWidth || mapY < 0 || mapY >= mapHeight) {
                    hit = true;
                } else if (mapData[mapY][mapX]) {
                    hit = true;
                }
            }

            //calculer la distance perpendiculaire au mur (pour éviter l'effet fish-eye)
            double perpWallDist;
            if (!side) {
                perpWallDist = (mapX - joueurX + (1 - stepX) / 2) / rayDirX;
            } else {
                perpWallDist = (mapY - joueurY + (1 - stepY) / 2) / rayDirY;
            }

            // Calculer la hauteur du mur à l'écran
            int lineHeight;
            if (perpWallDist > 0) {
                lineHeight = (int) (screenHeight / perpWallDist);
            } else {
                lineHeight = screenHeight;
            }

            //calculer les positions de début et fin du mur
            int drawStart = -lineHeight / 2 + screenHeight / 2;
            if (drawStart < 0) drawStart = 0;

            int drawEnd = lineHeight / 2 + screenHeight / 2;
            if (drawEnd >= screenHeight) drawEnd = screenHeight - 1;

            //choisir la couleur du mur (plus sombre pour les cotes Y)
            //pour ajouter un effet de texture simple
            Color wallColor;
            if (side) {
                wallColor = new Color(150, 75, 0); //marron fonce
            } else {
                wallColor = new Color(200, 100, 0); //marron clair
            }

            //dessiner la ligne verticale du mur
            //calculer la position et largeur de chaque colonne pour éviter les gaps
            int x1 = (i * screenWidth) / numRays;
            int x2 = ((i + 1) * screenWidth) / numRays;
            int rayWidth = x2 - x1;

            g.setColor(wallColor);
            g.fillRect(x1, drawStart, rayWidth, drawEnd - drawStart + 1);

            //remplir le z-buffer pour chaque pixel de cette colonne
            for (int x = x1; x < x2 && x < screenWidth; x++) {
                zBuffer[x] = perpWallDist;
            }
        }

        // Dessiner les sprites
        dessinerSprites(g, screenWidth, screenHeight, joueurX, joueurY, joueurAngle, fov);
    }

    private void dessinerSprites(Graphics g, int screenWidth, int screenHeight,
                                  double joueurX, double joueurY, double joueurAngle, double fov) {

        //trier les sprites par distance (du plus loin au plus proche)
        List<Sprite> sortedSprites = new ArrayList<>(sprites);
        sortedSprites.sort((a, b) -> {
            double distA = (a.getX() - joueurX) * (a.getX() - joueurX) + (a.getY() - joueurY) * (a.getY() - joueurY);
            double distB = (b.getX() - joueurX) * (b.getX() - joueurX) + (b.getY() - joueurY) * (b.getY() - joueurY);
            return Double.compare(distB, distA);
        });

        Graphics2D g2d = (Graphics2D) g;

        for (Sprite sprite : sortedSprites) {
            //position relative du sprite par rapport au joueur
            double spriteX = sprite.getX() - joueurX;
            double spriteY = sprite.getY() - joueurY;

            //calculer l'angle vers le sprite
            double spriteAngle = Math.atan2(spriteY, spriteX);
            double angleDiff = spriteAngle - joueurAngle;

            //normaliser l'angle entre -PI et PI
            while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
            while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

            //verifier si le sprite est dans le FOV
            if (Math.abs(angleDiff) > fov / 2 + 0.2) continue;

            //distance au sprite
            double spriteDistance = Math.sqrt(spriteX * spriteX + spriteY * spriteY);
            if (spriteDistance < 0.1) continue; // Trop proche

            //position X sur l'ecran
            int spriteScreenX = (int) ((0.5 + angleDiff / fov) * screenWidth);

            //taille du sprite à l'ecran
            int spriteHeight = (int) (screenHeight / spriteDistance);
            int spriteWidth = spriteHeight; // Sprite carré

            int drawStartY = screenHeight / 2 - spriteHeight / 2;
            int drawStartX = spriteScreenX - spriteWidth / 2;
            int drawEndX = spriteScreenX + spriteWidth / 2;

            //verifier si au moins une partie du sprite est visible (devant les murs)
            boolean visible = false;
            for (int stripe = Math.max(0, drawStartX); stripe < Math.min(screenWidth, drawEndX); stripe++) {
                if (spriteDistance < zBuffer[stripe]) {
                    visible = true;
                    break;
                }
            }
            if (!visible) continue;

            //dessiner le sprite avec clipping par colonne
            //on dessine colonne par colonne mais avec drawImage (beaucoup plus rapide)
            BufferedImage spriteImage = sprite.getImage();
            int imgWidth = spriteImage.getWidth();
            int imgHeight = spriteImage.getHeight();

            for (int stripe = Math.max(0, drawStartX); stripe < Math.min(screenWidth, drawEndX); stripe++) {
                //verifier le z-buffer
                if (spriteDistance < zBuffer[stripe]) {
                    //calculer quelle colonne de texture dessiner
                    int texX = (stripe - drawStartX) * imgWidth / spriteWidth;
                    if (texX < 0 || texX >= imgWidth) continue;

                    //dessiner cette colonne du sprite
                    g2d.drawImage(spriteImage,
                            stripe, drawStartY,           //destination top-left
                            stripe + 1, drawStartY + spriteHeight, //destination bottom-right
                            texX, 0,                      //source top-left
                            texX + 1, imgHeight,          //source bottom-right
                            null);
                }
            }
        }
    }

    //une methode qui permet de rendre une image avec un objet Map et un objet Joueur
    public void render() {
        //autoriser le dessin
        render = true;
        panelDessin.repaint();
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public void setJoueur(Joueur joueur) {
        this.joueur = joueur;
    }
}
