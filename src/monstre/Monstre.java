package monstre;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe représentant un monstre qui utilise RRT* pour le pathfinding
 * et Steering Behavior pour des mouvements naturels.
 * Implémente un automate à 3 états : Attente, Patrouille, Poursuite.
 */
public class Monstre {

    public static final int RAYON = 5;  // Rayon du monstre pour les collisions

    public enum Etat {
        ATTENTE,      // En attente d'une action
        PATROUILLE,   // Patrouille entre point A et B
        POURSUITE     // Poursuite d'une cible
    }

    private double x;
    private double y;
    private final SteeringBehavior steering;
    private final List<Noeud> chemin;
    private int waypointIndex = 0;
    private double waypointTolerance = 15.0;
    private boolean arrived = false;
    private Map map;

    // Détection de blocage
    private int stuckCounter = 0;
    private static final int STUCK_THRESHOLD = 40;
    private double prevX, prevY;

    // Attributs de l'automate
    private Etat etatActuel = Etat.ATTENTE;
    private long tempsAttente = 0;
    private long delaiAttente = 2000;
    private long dernierTemps = System.currentTimeMillis();

    // Points de patrouille
    private Noeud pointA;
    private Noeud pointB;
    private boolean versPointB = true;

    // Cible pour la poursuite
    private Noeud cible;
    private double distanceDetection = 150.0;

    // ==================== CONSTRUCTEURS ====================

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

    // ==================== MÉTHODES DE MOUVEMENT (MainChasse) ====================

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
     * Vérifie si le monstre a un chemin à suivre
     */
    public boolean aChemin() {
        return !chemin.isEmpty() && !arrived;
    }

    /**
     * Réinitialise le monstre pour un nouveau calcul de chemin.
     */
    public void resetChemin() {
        chemin.clear();
        waypointIndex = 0;
        arrived = false;
        stuckCounter = 0;
    }

    /**
     * Met à jour la position du monstre le long du chemin (utilisé par MainChasse)
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
        double seuilArrivee = Math.max(steering.getSpeed() + 1, 5.0);

        if (estDernierWaypoint && distance < seuilArrivee) {
            arreter();
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

        // Force de séparation des murs
        if (map != null) {
            applyWallSeparation();
        }

        // Sauvegarder la position avant déplacement
        prevX = x;
        prevY = y;

        // Calculer la nouvelle position
        double newX = x + steering.getVelocityX();
        double newY = y + steering.getVelocityY();

        // Gestion des collisions
        if (map != null) {
            if (!collidesWithWall(newX, newY)) {
                x = newX;
                y = newY;
            } else {
                boolean movedX = false;
                boolean movedY = false;

                if (!collidesWithWall(newX, y)) {
                    x = newX;
                    movedX = true;
                }
                if (!collidesWithWall(movedX ? newX : x, newY)) {
                    y = newY;
                    movedY = true;
                }

                if (!movedX) steering.killVelocityX();
                if (!movedY) steering.killVelocityY();
                if (!movedX && !movedY) steering.reset();
            }
        } else {
            x = newX;
            y = newY;
        }

        // Détection de blocage
        double deplacement = Math.sqrt(Math.pow(x - prevX, 2) + Math.pow(y - prevY, 2));
        if (deplacement < 0.3) {
            stuckCounter++;
        } else {
            stuckCounter = 0;
        }

        if (stuckCounter >= STUCK_THRESHOLD && waypointIndex < chemin.size() - 1) {
            waypointIndex++;
            stuckCounter = 0;
            steering.reset();
        }
    }

    // ==================== AUTOMATE À ÉTATS ====================

    /**
     * Met à jour le monstre selon l'automate à états.
     * Gère les transitions entre Attente, Patrouille et Poursuite.
     */
    public void updateAutomate() {
        long tempsActuel = System.currentTimeMillis();
        long deltaTemps = tempsActuel - dernierTemps;
        dernierTemps = tempsActuel;

        switch (etatActuel) {
            case ATTENTE:
                updateAttente(deltaTemps);
                break;
            case PATROUILLE:
                updatePatrouille();
                break;
            case POURSUITE:
                updatePoursuite();
                break;
        }
    }

    /**
     * État ATTENTE : Attend un certain temps avant de passer en patrouille.
     */
    private void updateAttente(long deltaTemps) {
        tempsAttente += deltaTemps;

        if (cibleDetectee()) {
            transitionVers(Etat.POURSUITE);
            return;
        }

        if (tempsAttente >= delaiAttente) {
            transitionVers(Etat.PATROUILLE);
        }
    }

    /**
     * État PATROUILLE : Patrouille entre les points A et B.
     */
    private void updatePatrouille() {
        if (cibleDetectee()) {
            transitionVers(Etat.POURSUITE);
            return;
        }

        if (arrived) {
            versPointB = !versPointB;
            Noeud destination = versPointB ? pointB : pointA;
            if (destination != null) {
                setChemin(destination);
            } else {
                transitionVers(Etat.ATTENTE);
                return;
            }
        }

        updateMouvement();
    }

    /**
     * État POURSUITE : Poursuit la cible.
     */
    private void updatePoursuite() {
        if (!cibleDetectee()) {
            transitionVers(Etat.ATTENTE);
            return;
        }

        if (cible != null && arrived) {
            setChemin(cible);
        }

        updateMouvement();
    }

    /**
     * Effectue une transition vers un nouvel état.
     */
    private void transitionVers(Etat nouvelEtat) {
        etatActuel = nouvelEtat;
        tempsAttente = 0;

        switch (nouvelEtat) {
            case ATTENTE:
                steering.reset();
                break;
            case PATROUILLE:
                Noeud destination = versPointB ? pointB : pointA;
                if (destination != null) {
                    setChemin(destination);
                }
                break;
            case POURSUITE:
                if (cible != null) {
                    setChemin(cible);
                }
                break;
        }
    }

    /**
     * Vérifie si une cible est détectée dans le rayon de détection.
     */
    private boolean cibleDetectee() {
        if (cible == null) return false;
        double distance = Math.sqrt(Math.pow(cible.getX() - x, 2) + Math.pow(cible.getY() - y, 2));
        return distance <= distanceDetection;
    }

    /**
     * Met à jour le mouvement du monstre le long du chemin (pour l'automate).
     */
    private void updateMouvement() {
        if (!hasValidPath()) {
            arrived = true;
            return;
        }

        Noeud target = chemin.get(waypointIndex);
        double distance = distanceTo(target);

        if (distance < waypointTolerance && waypointIndex < chemin.size() - 1) {
            waypointIndex++;
            target = chemin.get(waypointIndex);
        }

        steering.seek(x, y, target.getX(), target.getY());

        if (map != null) {
            applyWallSeparation();
        }

        double newX = x + steering.getVelocityX();
        double newY = y + steering.getVelocityY();

        appliquerMouvement(newX, newY);

        if (waypointIndex == chemin.size() - 1 && distance < 5) {
            arrived = true;
        }
    }

    private boolean hasValidPath() {
        return !chemin.isEmpty() && waypointIndex < chemin.size();
    }

    private double distanceTo(Noeud target) {
        return Math.sqrt(Math.pow(target.getX() - x, 2) + Math.pow(target.getY() - y, 2));
    }

    private void appliquerMouvement(double newX, double newY) {
        if (map == null) {
            x = newX;
            y = newY;
            return;
        }

        if (!collidesWithWall(newX, newY)) {
            x = newX;
            y = newY;
        } else {
            gererCollision(newX, newY);
        }
    }

    private void gererCollision(double newX, double newY) {
        if (!collidesWithWall(newX, y)) {
            x = newX;
            steering.killVelocityY();
            return;
        }

        if (!collidesWithWall(x, newY)) {
            y = newY;
            steering.killVelocityX();
            return;
        }

        steering.reset();
    }

    // ==================== GESTION DU CHEMIN ====================

    /**
     * Définit le chemin à suivre depuis le noeud final RRT*
     */
    public void setChemin(Noeud noeudFinal) {
        chemin.clear();
        waypointIndex = 0;
        arrived = false;
        stuckCounter = 0;
        steering.reset();

        if (noeudFinal == null) return;

        Noeud current = noeudFinal;
        while (current != null) {
            chemin.add(0, current);
            current = current.getParent();
        }

        optimiserPointEntree();
    }

    /**
     * Trouve le waypoint optimal pour rejoindre le chemin.
     */
    private void optimiserPointEntree() {
        if (chemin.isEmpty() || map == null) return;

        int meilleurIndex = 0;
        double meilleureDistance = Double.MAX_VALUE;

        for (int i = 0; i < chemin.size(); i++) {
            Noeud waypoint = chemin.get(i);
            double distance = Math.sqrt(Math.pow(waypoint.getX() - x, 2) + Math.pow(waypoint.getY() - y, 2));

            boolean accessible = !map.traverseMurAvecRayon((int) x, (int) y, waypoint.getX(), waypoint.getY(), RAYON);

            if (accessible) {
                double score = distance - (i * 10);
                if (score < meilleureDistance) {
                    meilleureDistance = score;
                    meilleurIndex = i;
                }
            }
        }

        waypointIndex = meilleurIndex;
    }

    private void arreter() {
        arrived = true;
        steering.reset();
    }

    // ==================== COLLISION & SÉPARATION ====================

    private void applyWallSeparation() {
        double separationX = 0;
        double separationY = 0;
        double probeDistance = RAYON + 4;
        int numProbes = 8;

        for (int i = 0; i < numProbes; i++) {
            double angle = 2 * Math.PI * i / numProbes;
            int probeX = (int) (x + probeDistance * Math.cos(angle));
            int probeY = (int) (y + probeDistance * Math.sin(angle));

            if (map.estDansMur(probeX, probeY)) {
                separationX -= Math.cos(angle);
                separationY -= Math.sin(angle);
            }
        }

        double mag = Math.sqrt(separationX * separationX + separationY * separationY);
        if (mag > 0) {
            double separationForce = 0.5;
            separationX = (separationX / mag) * separationForce;
            separationY = (separationY / mag) * separationForce;
            steering.applySeparation(separationX, separationY);
        }
    }

    private boolean collidesWithWall(double posX, double posY) {
        if (map == null) return false;

        int numPoints = 8;
        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            int checkX = (int) (posX + RAYON * Math.cos(angle));
            int checkY = (int) (posY + RAYON * Math.sin(angle));
            if (map.estDansMur(checkX, checkY)) {
                return true;
            }
        }
        return map.estDansMur((int) posX, (int) posY);
    }

    // ==================== GETTERS ====================

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRotation() { return steering.getRotation(); }
    public double getSpeed() { return steering.getSpeed(); }
    public boolean isArrived() { return arrived; }
    public List<Noeud> getChemin() { return chemin; }
    public int getWaypointIndex() { return waypointIndex; }
    public Etat getEtatActuel() { return etatActuel; }
    public SteeringBehavior getSteering() { return steering; }

    public Noeud getCurrentWaypoint() {
        if (chemin.isEmpty() || waypointIndex >= chemin.size()) {
            return null;
        }
        return chemin.get(waypointIndex);
    }

    // ==================== SETTERS ====================

    public void setWaypointTolerance(double tolerance) {
        this.waypointTolerance = tolerance;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public void setPointsPatrouille(Noeud pointA, Noeud pointB) {
        this.pointA = pointA;
        this.pointB = pointB;
    }

    public void setCible(Noeud cible) {
        this.cible = cible;
    }

    public void setDistanceDetection(double distance) {
        this.distanceDetection = distance;
    }

    public void setDelaiAttente(long delai) {
        this.delaiAttente = delai;
    }
}

