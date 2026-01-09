package moteur_graphique;

import java.awt.Graphics;

public interface GameRenderer {
    /**
     * Dessine la scène de jeu (le monde 3D ou 2D)
     * @param g Le contexte graphique fourni par la fenêtre
     * @param width Largeur de la zone de dessin
     * @param height Hauteur de la zone de dessin
     */
    void render(Graphics g, int width, int height);
}