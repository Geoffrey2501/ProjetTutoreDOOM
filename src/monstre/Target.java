package monstre;

import entite.Joueur;

/**
 * Classe représentant une cible (joueur) que le monstre peut voir et poursuivre.
 */
public class Target {

    private final Joueur joueur;
    private boolean visible;

    /**
     * Constructeur de la cible.
     * @param joueur Le joueur à cibler
     */
    public Target(Joueur joueur) {
        this.joueur = joueur;
        this.visible = true;
    }

    /**
     * Retourne la position X du joueur ciblé.
     */
    public double getX() {
        return joueur.getX();
    }

    /**
     * Retourne la position Y du joueur ciblé.
     */
    public double getY() {
        return joueur.getY();
    }

    /**
     * Retourne l'identifiant du joueur.
     */
    public String getId() {
        return joueur.getId();
    }

    /**
     * Calcule la distance entre cette cible et une position donnée.
     * @param fromX Position X d'origine
     * @param fromY Position Y d'origine
     * @return La distance euclidienne
     */
    public double distanceFrom(double fromX, double fromY) {
        double dx = getX() - fromX;
        double dy = getY() - fromY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Vérifie si la cible est dans le champ de vision du monstre.
     * @param fromX Position X du monstre
     * @param fromY Position Y du monstre
     * @param angleVision Angle de vision du monstre (en radians)
     * @param fov Champ de vision (Field of View) en radians
     * @param maxDistance Distance maximale de détection
     * @return true si la cible est visible
     */
    public boolean isInFieldOfView(double fromX, double fromY, double angleVision, double fov, double maxDistance) {
        // Vérifier la distance
        double distance = distanceFrom(fromX, fromY);
        if (distance > maxDistance) {
            return false;
        }

        // Calculer l'angle vers la cible
        double angleVersCible = Math.atan2(getY() - fromY, getX() - fromX);

        // Normaliser la différence d'angle entre -PI et PI
        double diffAngle = normalizeAngle(angleVersCible - angleVision);

        // Vérifier si la cible est dans le champ de vision
        return Math.abs(diffAngle) <= fov / 2;
    }

    /**
     * Normalise un angle entre -PI et PI.
     */
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    /**
     * Convertit cette cible en Noeud pour le pathfinding RRT*.
     * @return Un noeud à la position actuelle du joueur
     */
    public Noeud toNoeud() {
        return new Noeud((int) getX(), (int) getY());
    }

    /**
     * Retourne le joueur ciblé.
     */
    public Joueur getJoueur() {
        return joueur;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public String toString() {
        return "Target[" + getId() + "] (" + String.format("%.1f", getX()) + ", " + String.format("%.1f", getY()) + ")";
    }
}


