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

    // Attributs de l'automate
    private Etat etatActuel = Etat.ATTENTE;
    private long tempsAttente = 0;
    private long delaiAttente = 2000; // 2 secondes d'attente
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

    // ==================== MÉTHODE PRINCIPALE UPDATE (AUTOMATE) ====================

    /**
     * Met à jour le monstre selon l'automate à états.
     * Gère les transitions entre Attente, Patrouille et Poursuite.
     */
    public void update() {
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

    // ==================== MÉTHODES D'ÉTAT ====================

    /**
     * État ATTENTE : Attend un certain temps avant de passer en patrouille.
     */
    private void updateAttente(long deltaTemps) {
        tempsAttente += deltaTemps;

        // Transition : Attente -> Poursuite (si cible détectée)
        if (cibleDetectee()) {
            transitionVers(Etat.POURSUITE);
            return;
        }

        // Transition : Attente -> Patrouille (après délai)
        if (tempsAttente >= delaiAttente) {
            transitionVers(Etat.PATROUILLE);
        }
    }

    /**
     * État PATROUILLE : Patrouille entre les points A et B.
     */
    private void updatePatrouille() {
        // Transition : Patrouille -> Poursuite (si cible détectée)
        if (cibleDetectee()) {
            transitionVers(Etat.POURSUITE);
            return;
        }

        // Vérifier si arrivé au point de destination
        if (arrived) {
            // Alterner entre point A et B
            versPointB = !versPointB;
            Noeud destination = versPointB ? pointB : pointA;
            if (destination != null) {
                setChemin(destination);
            } else {
                // Pas de points de patrouille définis -> retour en attente
                transitionVers(Etat.ATTENTE);
                return;
            }
        }

        // Continuer le mouvement
        updateMouvement();
    }

    /**
     * État POURSUITE : Poursuit la cible.
     */
    private void updatePoursuite() {
        // Transition : Poursuite -> Attente (si cible perdue)
        if (!cibleDetectee()) {
            transitionVers(Etat.ATTENTE);
            return;
        }

        // Mettre à jour le chemin vers la cible
        if (cible != null && arrived) {
            setChemin(cible);
        }

        // Continuer le mouvement
        updateMouvement();
    }

    // ==================== GESTION DES TRANSITIONS ====================

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

    // ==================== MOUVEMENT ====================

    /**
     * Met à jour le mouvement du monstre le long du chemin.
     */
    private void updateMouvement() {
        if (!hasValidPath()) {
            arrived = true;
            return;
        }

        Noeud target = getCurrentTarget();
        double distance = distanceTo(target);

        // Passer au waypoint suivant si assez proche
        if (distance < waypointTolerance && waypointIndex < chemin.size() - 1) {
            waypointIndex++;
            target = chemin.get(waypointIndex);
        }

        // Appliquer le steering behavior
        steering.seek(x, y, target.getX(), target.getY());

        // Calculer et appliquer le mouvement avec gestion des collisions
        double newX = x + steering.getVelocityX();
        double newY = y + steering.getVelocityY();

        appliquerMouvement(newX, newY);

        // Vérifier si arrivé à destination
        if (waypointIndex == chemin.size() - 1 && distance < 5) {
            arrived = true;
        }
    }

    /**
     * Vérifie si le chemin est valide.
     */
    private boolean hasValidPath() {
        return !chemin.isEmpty() && waypointIndex < chemin.size();
    }

    /**
     * Retourne le waypoint cible actuel.
     */
    private Noeud getCurrentTarget() {
        return chemin.get(waypointIndex);
    }

    /**
     * Calcule la distance vers un noeud.
     */
    private double distanceTo(Noeud target) {
        return Math.sqrt(Math.pow(target.getX() - x, 2) + Math.pow(target.getY() - y, 2));
    }

    /**
     * Applique le mouvement avec gestion des collisions.
     */
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
            // Collision : essayer de glisser le long des murs
            gererCollision(newX, newY);
        }
    }

    /**
     * Gère la collision en essayant de glisser le long des murs.
     */
    private void gererCollision(double newX, double newY) {
        // Essai glissade horizontale
        if (!collidesWithWall(newX, y)) {
            x = newX;
            steering.killVelocityY();
            return;
        }

        // Essai glissade verticale
        if (!collidesWithWall(x, newY)) {
            y = newY;
            steering.killVelocityX();
            return;
        }

        // Bloqué dans un coin
        steering.reset();
    }

    // ==================== GESTION DU CHEMIN ====================

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

    // ==================== COLLISION ====================

    /**
     * Vérifie si la position donnée entre en collision avec un mur
     * en tenant compte du rayon du monstre.
     */
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

    /**
     * Retourne le waypoint actuel ciblé
     */
    public Noeud getCurrentWaypoint() {
        if (chemin.isEmpty() || waypointIndex >= chemin.size()) {
            return null;
        }
        return chemin.get(waypointIndex);
    }

    // ==================== SETTERS / CONFIGURATION ====================

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

