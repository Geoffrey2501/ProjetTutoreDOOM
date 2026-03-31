package monstre;

import java.util.ArrayList;
import java.util.List;

public class Monstre {

    public static final int RAYON = 5;

    private double x, y;
    private final SteeringBehavior steering;
    private final Map map;

    private List<Noeud> chemin = new ArrayList<>();
    private int waypointIndex = 0;
    private boolean arrived = false;

    private Noeud pointA;
    private Noeud pointB;
    private Noeud destination;

    private Target target;
    private double distanceDetection = 150;

    private final Automate automate;

    private long dernierRecalcul = 0;
    private double lastTargetX = -1;
    private double lastTargetY = -1;
    private final double SEUIL_MOUVEMENT = 10.0; // Distance minimum pour justifier un recalcul

    // ===================== CONSTRUCTEUR =====================

    public Monstre(double x, double y, Map map) {
        this.x = x;
        this.y = y;
        this.map = map;
        this.steering = new SteeringBehavior();
        this.automate = new Automate(this);

    }

    // ===================== UPDATE =====================

    public void update() {
        automate.update();
    }

    // ===================== AUTOMATE HELPERS =====================

    public boolean cibleDetectee() {
        if (target == null || !target.isVisible()) return false;
        return target.distanceFrom(x, y) <= distanceDetection;
    }

    public void resetSteering() {
        steering.reset();
    }

    public boolean isArrived() {
        return arrived;
    }
    public void reprendrePatrouille() {

        if (pointA == null || pointB == null) return;

        double distA = distance(pointA);
        double distB = distance(pointB);

        destination = (distA < distB) ? pointA : pointB;

        recalculerChemin();
    }

    // ===================== MOUVEMENT =====================

    public void updateMouvement() {
        if (chemin.isEmpty() || waypointIndex >= chemin.size()) {
            arrived = true;
            return;
        }

        Noeud cible = chemin.get(waypointIndex);
        double distToTarget = distance(cible);
         repousserMur();

        // Changement de waypoint si on est assez proche
        if (distToTarget < 10 && waypointIndex < chemin.size() - 1) {
            waypointIndex++;
            return;
        }

        // Arrêt final au dernier point
        if (waypointIndex == chemin.size() - 1 && distToTarget < 5) {
            x = cible.getX();
            y = cible.getY();
            arrived = true;
            steering.reset();
            return;
        }

        // Calcul de la direction souhaitée via le steering
        steering.seek(x, y, cible.getX(), cible.getY());

        // Calcul des positions futures potentielles
        double nextX = x + steering.getVelocityX();
        double nextY = y + steering.getVelocityY();

        // Gestion des collisions par itération sur getMurs()
        boolean collisionX = false;
        boolean collisionY = false;

        // On définit une boîte de collision simple pour le monstre
        // On utilise RAYON pour donner de l'épaisseur au monstre
        for (Mur mur : map.getMurs()) {
            // Test collision sur l'axe X
            if (mur.esDansMur((int) nextX, (int) y)) {
                collisionX = true;
            }
            // Test collision sur l'axe Y
            if (mur.esDansMur((int) x, (int) nextY)) {
                collisionY = true;
            }
        }

        // Application du mouvement si aucune collision n'est détectée
        if (!collisionX) {
            x = nextX;
        } else {
            steering.reset(); // On stoppe la force si on tape un mur
        }

        if (!collisionY) {
            y = nextY;
        } else {
            steering.reset();
        }
    }

    private void recalculerChemin() {
        if (destination == null) return;

        // Le RRT va d'abord chercher dans ses souvenirs (graphe existant)
        Noeud finChemin = RRT.trouverChemin((int) x, (int) y, destination.getX(), destination.getY());

        if (finChemin == null) return;

        chemin.clear();
        waypointIndex = 0;
        arrived = false;

        while (finChemin != null) {
            chemin.add(0, finChemin);
            finChemin = finChemin.getParent();
        }
    }


    /**
     * Méthode générique pour calculer un chemin vers n'importe quelle coordonnée
     */
    public void recalculerCheminVers(int targetX, int targetY) {
        Noeud fin = RRT.trouverChemin((int) x, (int) y, targetX, targetY);

        if (fin == null) return;

        chemin.clear();
        waypointIndex = 0;
        arrived = false;

        // Reconstituer le chemin à partir des parents
        while (fin != null) {
            chemin.add(0, fin);
            fin = fin.getParent();
        }
    }

    // ===================== UTILS =====================

    private double distance(Noeud n) {
        return Math.sqrt(Math.pow(n.getX() - x, 2)
                + Math.pow(n.getY() - y, 2));
    }

    public Target getTarget() {
        return target;
    }

    // ===================== SETTERS =====================

    public void setTarget(Target target) {
        this.target = target;
    }

    public void setPointsPatrouille(Noeud a, Noeud b) {
        this.pointA = a;
        this.pointB = b;
    }

    public Automate.Etat getEtat(){
        return automate.getEtat();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }


    public void updatePatrouille() {

        // Avancer le long du chemin courant
        updateMouvement();

        // Si réellement arrivé au point
        if (arrived && destination != null) {

            // Alterner vers l'autre point
            destination = (destination == pointA) ? pointB : pointA;

            System.out.println("[Monstre] Arrivé au point "
                    + (destination == pointA ? "B" : "A")
                    + " → Nouvelle destination : "
                    + (destination == pointA ? "A" : "B"));

            recalculerChemin();
        }
    }

    public void updatePoursuite() {
        if (target == null) return;

        // Calcul de la distance entre la position actuelle de la cible et la dernière position connue
        double distanceCibleBougee = Math.sqrt(Math.pow(target.getX() - lastTargetX, 2)
                + Math.pow(target.getY() - lastTargetY, 2));

        // On ne recalcule que si le chemin est vide OU si la cible a bougé significativement
        if (chemin.isEmpty() || arrived || distanceCibleBougee > SEUIL_MOUVEMENT) {
            recalculerCheminVers((int)target.getX(), (int)target.getY());

            // Mise à jour de la dernière position connue
            lastTargetX = target.getX();
            lastTargetY = target.getY();
        }

        // On appelle le mouvement physique
        updateMouvement();
    }

    public ArrayList<Noeud> getChemin() {
        for (Noeud n : chemin) {
            System.out.print("(" + n.getX() + ", " + n.getY() + ")->");
        }
        if(chemin == null) return new ArrayList<>();
        return new ArrayList<>(chemin);

    }

    public double getDistanceDetection() {
        return distanceDetection;
    }

    public double getRotation(){
        if (target == null) return 0;
        double angle = Math.atan2(target.getY() - y, target.getX() - x);
        return Math.toDegrees(angle);
    }

    private void repousserMur() {
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

}
