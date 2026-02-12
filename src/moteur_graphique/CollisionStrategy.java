package moteur_graphique;

public interface CollisionStrategy {
    /**
     * Vérifie si la position (x, y) est en collision avec un mur.
     * @param x La position x à tester
     * @param y La position y à tester
     * @param radius Le rayon du joueur (utile pour le BSP, peut être ignoré pour le Raycasting simple)
     * @return true si la position est en collision avec un mur, false sinon
     */
    boolean isColliding(double x, double y, double radius);
}
