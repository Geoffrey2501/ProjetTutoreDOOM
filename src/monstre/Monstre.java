package monstre;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe représentant un monstre qui utilise RRT* pour le pathfinding
 * et Steering Behavior pour des mouvements naturels.
 */
public class Monstre {
    public static final int RAYON = 0;  // Rayon du monstre pour les collisions
    private double x;
    private double y;
    private final SteeringBehavior steering;
    private final List<Noeud> chemin;
    private int waypointIndex = 0;
    private double waypointTolerance = 15.0;
    private boolean arrived = false;

    public Monstre(double x, double y) {
        this.x = x;
        this.y = y;
        this.steering = new SteeringBehavior();
        this.chemin = new ArrayList<>();
    }

    /**
     * Définit le chemin à suivre depuis le noeud final RRT*
     * @param noeudFinal Le noeud d'arrivée avec les parents liés
     */
    public void setChemin(Noeud noeudFinal) {
        chemin.clear();
        waypointIndex = 0;
        arrived = false;
        steering.reset();

        if (noeudFinal == null) return;

        // Reconstruire le chemin depuis la fin vers le début
        Noeud current = noeudFinal;
        while (current != null) {
            chemin.add(0, current);
            current = current.getParent();
        }
    }

    /**
     * Met à jour la position du monstre avec Steering Behavior
     */
    public void update() {
        if (chemin.isEmpty() || waypointIndex >= chemin.size()) {
            arrived = true;
            return;
        }

        Noeud target = chemin.get(waypointIndex);
        double distance = Math.sqrt(Math.pow(target.getX() - x, 2) + Math.pow(target.getY() - y, 2));

        // Passer au waypoint suivant si assez proche
        if (distance < waypointTolerance && waypointIndex < chemin.size() - 1) {
            waypointIndex++;
            target = chemin.get(waypointIndex);
        }

        // Utiliser arrive pour le dernier waypoint, seek sinon
        if (waypointIndex == chemin.size() - 1) {
            steering.arrive(x, y, target.getX(), target.getY());
        } else {
            steering.seek(x, y, target.getX(), target.getY());
        }

        // Appliquer la vélocité
        x += steering.getVelocityX();
        y += steering.getVelocityY();

        // Vérifier si arrivé à destination
        if (waypointIndex == chemin.size() - 1 && distance < 5) {
            arrived = true;
        }
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRotation() { return steering.getRotation(); }
    public double getSpeed() { return steering.getSpeed(); }
    public boolean isArrived() { return arrived; }
    public List<Noeud> getChemin() { return chemin; }
    public int getWaypointIndex() { return waypointIndex; }

    /**
     * Retourne le waypoint actuel ciblé
     */
    public Noeud getCurrentWaypoint() {
        if (chemin.isEmpty() || waypointIndex >= chemin.size()) {
            return null;
        }
        return chemin.get(waypointIndex);
    }

    // Configuration
    public void setWaypointTolerance(double tolerance) {
        this.waypointTolerance = tolerance;
    }

    public SteeringBehavior getSteering() {
        return steering;
    }
}

