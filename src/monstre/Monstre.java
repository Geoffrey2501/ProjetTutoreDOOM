package monstre;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe représentant un monstre qui utilise RRT* pour le pathfinding
 * et Steering Behavior pour des mouvements naturels.
 */
public class Monstre {
    public static final int RAYON = 5;  // Rayon du monstre pour les collisions
    private double x;
    private double y;
    private final SteeringBehavior steering;
    private final List<Noeud> chemin;
    private int waypointIndex = 0;
    private double waypointTolerance = 15.0;
    private boolean arrived = false;
    private Map map;  // Référence à la map pour vérifier les collisions
    private int stuckCounter = 0;          // Compteur de frames bloqué
    private static final int STUCK_THRESHOLD = 40; // Frames avant de skip le waypoint
    private double prevX, prevY;           // Position précédente pour détecter le blocage

    public Monstre(double x, double y) {
        this.x = x;
        this.y = y;
        this.steering = new SteeringBehavior();
        this.chemin = new ArrayList<>();
    }

    public Monstre(double x, double y, Map map) {
        this(x, y);
        this.map = map;
    }

    /**
     * Définit le chemin à suivre depuis le noeud final RRT*
     * @param noeudFinal Le noeud d'arrivée avec les parents liés
     */
    public void setChemin(Noeud noeudFinal) {
        chemin.clear();
        waypointIndex = 0;
        arrived = false;
        stuckCounter = 0;
        steering.reset();

        if (noeudFinal == null) return;

        // Reconstruire le chemin depuis la fin vers le début
        Noeud current = noeudFinal;
        while (current != null) {
            chemin.add(0, current);
            current = current.getParent();
        }

        // Trouver le meilleur point d'entrée dans le chemin
        optimiserPointEntree();
    }

    /**
     * Trouve le waypoint optimal pour rejoindre le chemin.
     * Ignore les premiers waypoints si le monstre peut atteindre directement
     * un waypoint plus avancé sans traverser de mur.
     */
    private void optimiserPointEntree() {
        if (chemin.isEmpty() || map == null) return;

        int meilleurIndex = 0;
        double meilleureDistance = Double.MAX_VALUE;

        // Parcourir le chemin pour trouver le waypoint le plus avancé accessible
        for (int i = 0; i < chemin.size(); i++) {
            Noeud waypoint = chemin.get(i);
            double distance = Math.sqrt(Math.pow(waypoint.getX() - x, 2) + Math.pow(waypoint.getY() - y, 2));

            // Vérifier si on peut atteindre ce waypoint en ligne directe
            boolean accessible = !map.traverseMurAvecRayon((int) x, (int) y, waypoint.getX(), waypoint.getY(), RAYON);

            if (accessible) {
                // Privilégier les waypoints plus avancés dans le chemin (plus proches de la cible)
                // tout en restant à une distance raisonnable
                double score = distance - (i * 10); // Bonus pour les waypoints plus avancés
                if (score < meilleureDistance) {
                    meilleureDistance = score;
                    meilleurIndex = i;
                }
            }
        }

        waypointIndex = meilleurIndex;
    }

    /**
     * Mode poursuite directe : se dirige en ligne droite vers une cible
     * sans chemin RRT*. Utilisé en attendant que le RRT* soit calculé.
     */
    public void seekDirect(double targetX, double targetY) {
        double distance = Math.sqrt(Math.pow(targetX - x, 2) + Math.pow(targetY - y, 2));

        // Utiliser seekAggressive quand on est proche pour éviter les oscillations
        if (distance < 50) {
            steering.seekAggressive(x, y, targetX, targetY);
        } else {
            steering.seek(x, y, targetX, targetY);
        }

        // Force de séparation des murs
        if (map != null) {
            applyWallSeparation();
        }

        double newX = x + steering.getVelocityX();
        double newY = y + steering.getVelocityY();

        if (map != null) {
            if (!collidesWithWall(newX, newY)) {
                x = newX;
                y = newY;
            } else {
                boolean movedX = false;
                if (!collidesWithWall(newX, y)) {
                    x = newX;
                    movedX = true;
                }
                if (!collidesWithWall(movedX ? newX : x, newY)) {
                    y = newY;
                }
                if (!movedX) steering.killVelocityX();
            }
        } else {
            x = newX;
            y = newY;
        }
    }

    /**
     * Réinitialise le monstre pour un nouveau calcul de chemin.
     * Ne stoppe pas le mouvement en cours.
     */
    public void resetChemin() {
        chemin.clear();
        waypointIndex = 0;
        arrived = false;
        stuckCounter = 0;
    }

    /**
     * Vérifie si le monstre a un chemin à suivre
     */
    public boolean aChemin() {
        return !chemin.isEmpty() && !arrived;
    }

    /**
     * Met à jour la position du monstre avec Steering Behavior
     */
    public void update() {
        if (chemin.isEmpty() || waypointIndex >= chemin.size()) {
            arreter();
            return;
        }

        Noeud target = chemin.get(waypointIndex);
        double distance = Math.sqrt(Math.pow(target.getX() - x, 2) + Math.pow(target.getY() - y, 2));

        // Passer au waypoint suivant si assez proche
        if (distance < waypointTolerance && waypointIndex < chemin.size() - 1) {
            waypointIndex++;
            target = chemin.get(waypointIndex);
            distance = Math.sqrt(Math.pow(target.getX() - x, 2) + Math.pow(target.getY() - y, 2));
        }

        // Dernier waypoint : vérifier l'arrivée avec un seuil adapté à la vitesse
        boolean estDernierWaypoint = (waypointIndex == chemin.size() - 1);
        double seuilArrivee = Math.max(steering.getSpeed() + 1, 5.0); // Seuil dynamique

        if (estDernierWaypoint && distance < seuilArrivee) {
            arreter();
            // Snap à la position finale
            x = target.getX();
            y = target.getY();
            return;
        }

        // Utiliser arrive() pour le dernier waypoint (ralentissement), seek() sinon
        if (estDernierWaypoint) {
            steering.arrive(x, y, target.getX(), target.getY());
        } else {
            steering.seek(x, y, target.getX(), target.getY());
        }

        // Force de séparation des murs (repousse le monstre avant qu'il ne se colle)
        if (map != null) {
            applyWallSeparation();
        }

        // Sauvegarder la position avant déplacement (pour détection de blocage)
        prevX = x;
        prevY = y;

        // Calculer la nouvelle position
        double newX = x + steering.getVelocityX();
        double newY = y + steering.getVelocityY();

        // Vérifier les collisions avec la hitbox
        if (map != null) {
            if (!collidesWithWall(newX, newY)) {
                // Pas de collision : on avance normalement
                x = newX;
                y = newY;
            } else {
                // COLLISION ! Glissade indépendante sur chaque axe
                boolean movedX = false;
                boolean movedY = false;

                // Essai mouvement horizontal (glissade sur mur vertical)
                if (!collidesWithWall(newX, y)) {
                    x = newX;
                    movedX = true;
                }
                // Essai mouvement vertical (glissade sur mur horizontal)
                if (!collidesWithWall(movedX ? newX : x, newY)) {
                    y = newY;
                    movedY = true;
                }

                // Annuler les composantes de vélocité bloquées
                if (!movedX) steering.killVelocityX();
                if (!movedY) steering.killVelocityY();

                // Si complètement bloqué (coin)
                if (!movedX && !movedY) {
                    steering.reset();
                }
            }
        } else {
            x = newX;
            y = newY;
        }

        // Détection de blocage : si le monstre n'a presque pas bougé
        double deplacement = Math.sqrt(Math.pow(x - prevX, 2) + Math.pow(y - prevY, 2));
        if (deplacement < 0.3) {
            stuckCounter++;
        } else {
            stuckCounter = 0;
        }

        // Si bloqué trop longtemps, sauter au waypoint suivant
        if (stuckCounter >= STUCK_THRESHOLD && waypointIndex < chemin.size() - 1) {
            waypointIndex++;
            stuckCounter = 0;
            steering.reset();
        }
    }

    /**
     * Arrête le monstre : vélocité à zéro, marqué comme arrivé
     */
    private void arreter() {
        arrived = true;
        steering.reset();
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

    public void setMap(Map map) {
        this.map = map;
    }

    public SteeringBehavior getSteering() {
        return steering;
    }

    /**
     * Applique une force de séparation qui repousse le monstre des murs proches.
     * Sonde autour du monstre et pousse dans la direction opposée aux murs détectés.
     */
    private void applyWallSeparation() {
        double separationX = 0;
        double separationY = 0;
        double probeDistance = RAYON + 4; // Sonder un peu plus loin que la hitbox
        int numProbes = 8;

        for (int i = 0; i < numProbes; i++) {
            double angle = 2 * Math.PI * i / numProbes;
            int probeX = (int) (x + probeDistance * Math.cos(angle));
            int probeY = (int) (y + probeDistance * Math.sin(angle));

            if (map.estDansMur(probeX, probeY)) {
                // Pousser dans la direction opposée au mur
                separationX -= Math.cos(angle);
                separationY -= Math.sin(angle);
            }
        }

        // Normaliser et appliquer la force de séparation
        double mag = Math.sqrt(separationX * separationX + separationY * separationY);
        if (mag > 0) {
            double separationForce = 0.5; // Force de répulsion assez forte
            separationX = (separationX / mag) * separationForce;
            separationY = (separationY / mag) * separationForce;
            steering.applySeparation(separationX, separationY);
        }
    }

    /**
     * Vérifie si la position donnée entre en collision avec un mur
     * en tenant compte du rayon du monstre.
     */
    private boolean collidesWithWall(double posX, double posY) {
        if (map == null) return false;

        // Vérifier plusieurs points autour du cercle de la hitbox
        int numPoints = 8;
        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            int checkX = (int) (posX + RAYON * Math.cos(angle));
            int checkY = (int) (posY + RAYON * Math.sin(angle));
            if (map.estDansMur(checkX, checkY)) {
                return true;
            }
        }
        // Vérifier aussi le centre
        return map.estDansMur((int) posX, (int) posY);
    }
}

