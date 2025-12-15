package prototype_raycasting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

public class Raycasting extends JFrame {

    private JPanel panelDessin;
    private boolean render;
    private Map map;
    private Joueur joueur;
    private List<Joueur> autresJoueurs; // Liste des autres joueurs à afficher
    private int FOV = 60;
    private int NUM_RAYS = 1000; //"definition" de la qualite du rendu

    public Raycasting(Map m, Joueur j) {
        //le titre de la fenetre
        super("Prototype Raycasting");

        this.map = m;
        this.joueur = j;
        this.autresJoueurs = new ArrayList<>();

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
        }

        //dessiner les autres joueurs (sprites)
        dessinerAutresJoueurs(g, screenWidth, screenHeight, joueurX, joueurY, joueurAngle);
    }

    //methode pour dessiner les autres joueurs comme des sprites
    private void dessinerAutresJoueurs(Graphics g, int screenWidth, int screenHeight, 
                                       double joueurX, double joueurY, double joueurAngle) {
        for (Joueur autreJoueur : autresJoueurs) {
            //calculer le vecteur relatif du joueur actuel vers l'autre joueur
            double dx = autreJoueur.getX() - joueurX;
            double dy = autreJoueur.getY() - joueurY;
            
            //calculer la distance
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            //ne pas dessiner si trop proche (éviter division par zéro et rendu bizarre)
            if (distance < 0.1) continue;
            
            //calculer l'angle vers l'autre joueur
            double angleVersJoueur = Math.atan2(dy, dx);
            
            //calculer l'angle relatif par rapport à la direction du joueur actuel
            double angleRelatif = angleVersJoueur - joueurAngle;
            
            //normaliser l'angle entre -PI et PI
            while (angleRelatif > Math.PI) angleRelatif -= 2 * Math.PI;
            while (angleRelatif < -Math.PI) angleRelatif += 2 * Math.PI;
            
            //vérifier si l'autre joueur est dans le champ de vision
            double fov = Math.toRadians(FOV);
            if (Math.abs(angleRelatif) > fov / 2) continue; //hors du champ de vision
            
            //calculer la position x à l'écran (basée sur l'angle relatif)
            //angleRelatif = 0 correspond au centre de l'écran
            double screenX = (angleRelatif / fov + 0.5) * screenWidth;
            
            //calculer la taille du sprite en fonction de la distance
            int spriteHeight = (int) (screenHeight / distance);
            int spriteWidth = spriteHeight / 2; //sprite plus étroit que haut
            
            //limiter la taille
            if (spriteHeight > screenHeight) spriteHeight = screenHeight;
            if (spriteWidth > screenWidth / 4) spriteWidth = screenWidth / 4;
            
            //calculer la position y (centré verticalement)
            int spriteY = (screenHeight - spriteHeight) / 2;
            int spriteX = (int) (screenX - spriteWidth / 2);
            
            //dessiner le sprite (simple rectangle coloré pour l'instant)
            g.setColor(new Color(255, 0, 0, 180)); //rouge semi-transparent
            g.fillRect(spriteX, spriteY, spriteWidth, spriteHeight);
            
            //ajouter un contour pour mieux voir
            g.setColor(new Color(255, 255, 0)); //jaune
            g.drawRect(spriteX, spriteY, spriteWidth, spriteHeight);
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

    //méthodes pour gérer les autres joueurs
    public void ajouterAutreJoueur(Joueur joueur) {
        if (!autresJoueurs.contains(joueur)) {
            autresJoueurs.add(joueur);
        }
    }

    public void retirerAutreJoueur(Joueur joueur) {
        autresJoueurs.remove(joueur);
    }

    public void viderAutresJoueurs() {
        autresJoueurs.clear();
    }

    public List<Joueur> getAutresJoueurs() {
        return new ArrayList<>(autresJoueurs);
    }
}
