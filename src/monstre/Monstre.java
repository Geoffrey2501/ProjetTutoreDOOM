package monstre;

import java.util.ArrayList;
import java.util.List;
import moteur_graphique.CollisionStrategy;

public class Monstre {

    public static final double RAYON = 0.25;

    // Liste globale pour accéder aux autres monstres (séparation et alerte)
    public static List<Monstre> tousLesMonstres = new ArrayList<>();

    private double x, y;
    private final SteeringBehavior steering;
    private final CollisionStrategy collision;
    private final RRT rrt;

    private List<Noeud> chemin = new ArrayList<>();
    private int waypointIndex = 0;
    private boolean arrived = false;

    private Noeud pointA;
    private Noeud pointB;
    private Noeud destination;

    private Target target;
    private double distanceDetection = 5.0;

    private final Automate automate;

    private long dernierRecalcul = 0;
    private double lastTargetX = -1;
    private double lastTargetY = -1;
    private final double SEUIL_MOUVEMENT = 1.0; // Distance minimum pour justifier un recalcul

    // Variables pour l'alerte
    private double alerteX = -1;
    private double alerteY = -1;

    // ===================== CONSTRUCTEUR =====================

    public Monstre(double x, double y, CollisionStrategy collision) {
        this.x = x;
        this.y = y;
        this.collision = collision;
        this.rrt = new RRT(this.collision);
        this.steering = new SteeringBehavior();
        this.automate = new Automate(this);
        tousLesMonstres.add(this);
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
        if (distToTarget < 0.5 && waypointIndex < chemin.size() - 1) {
            waypointIndex++;
            return;
        }

        // Arrêt final au dernier point
        if (waypointIndex == chemin.size() - 1 && distToTarget < 0.2) {
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

        // Gestion des collisions améliorée pour les coins
        boolean collisionX = collision.isColliding(nextX, y, RAYON);
        boolean collisionY = collision.isColliding(x, nextY, RAYON);

        if (!collisionX && !collisionY && collision.isColliding(nextX, nextY, RAYON)) {
            // On touche un coin ! Au lieu de juste bloquer, on favorise l'axe
            // qui nous éloigne le plus de l'obstacle.
            if (Math.abs(steering.getVelocityX()) > Math.abs(steering.getVelocityY())) {
                collisionY = true; // On bloque Y pour forcer le glissement en X
            } else {
                collisionX = true; // On bloque X pour forcer le glissement en Y
            }
        }

        // Application du mouvement avec "Sliding"
        if (!collisionX) {
            x = nextX;
        } else {
            // Si bloqué en X, on donne un petit bonus de vitesse en Y
            // pour aider à "glisser" le long du mur
            steering.killVelocityX();
        }

        if (!collisionY) {
            y = nextY;
        } else {
            // Si bloqué en Y, on donne un petit bonus de vitesse en X
            steering.killVelocityY();
        }
    }

    public void recalculerChemin() {
        if (destination == null) return;

        // Le RRT va d'abord chercher dans ses souvenirs (graphe existant)
        Noeud finChemin = rrt.trouverChemin(x, y, destination.getX(), destination.getY());

        if (finChemin == null) {
            arrived = false; // Empecher la boucle infinie d'alternance si le chemin echoue
            return;
        }

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
    public void recalculerCheminVers(double targetX, double targetY) {
        Noeud fin = rrt.trouverChemin(x, y, targetX, targetY);

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

    /**
     * Configure les bornes de la carte pour l'algorithme RRT*.
     * Nécessaire pour les cartes BSP avec des coordonnées négatives.
     */
    public void setMapBounds(double minX, double minY, double maxX, double maxY) {
        this.rrt.setMapBounds(minX, minY, maxX, maxY);
    }

    public Automate.Etat getEtat(){
        return automate.getEtat();
    }

    public void forcerPoursuite() {
        this.automate.forcerEtatPoursuite();
    }

    public void recevoirAlerte(double xPosition, double yPosition, Target t) {
        this.alerteX = xPosition;
        this.alerteY = yPosition;
        this.target = t;
        this.automate.forcerEtatAlerte();
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
        
        applySocialSteering(tousLesMonstres);
        alerterAutresMonstres(); // Alerte en continu les autres monstres autour pendant la poursuite

        long currentTime = System.currentTimeMillis();

        // Calcul de la distance entre la position actuelle de la cible et la dernière position connue
        double distanceCibleBougee = Math.sqrt(Math.pow(target.getX() - lastTargetX, 2)
                + Math.pow(target.getY() - lastTargetY, 2));

        // On ne recalcule que si le chemin est vide OU si la cible a bougé significativement,
        // ET uniquement si 500ms se sont écoulées depuis le dernier recalcul pour éviter de lagguer.
        if ((chemin.isEmpty() || arrived || distanceCibleBougee > SEUIL_MOUVEMENT) && (currentTime - dernierRecalcul > 500)) {
            recalculerCheminVers(target.getX(), target.getY());

            // Mise à jour de la dernière position connue
            lastTargetX = target.getX();
            lastTargetY = target.getY();
            dernierRecalcul = currentTime;
        }

        // On appelle le mouvement physique
        updateMouvement();
    }

    public void updateAlerte() {
        applySocialSteering(tousLesMonstres);
        
        long currentTime = System.currentTimeMillis();

        // recalcule le chemin si le point d'alerte bouge ou si on a pas de chemin
        double distanceAlerteBougee = Math.sqrt(Math.pow(alerteX - lastTargetX, 2)
                + Math.pow(alerteY - lastTargetY, 2));

        // Limitation du recalcul à 1 fois par 500ms maximum
        if ((chemin.isEmpty() || arrived || distanceAlerteBougee > SEUIL_MOUVEMENT) && (currentTime - dernierRecalcul > 500)) {
            recalculerCheminVers(alerteX, alerteY);
            lastTargetX = alerteX;
            lastTargetY = alerteY;
            dernierRecalcul = currentTime;
        }

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
        // On augmente la distance de détection (RAYON + 0.4 au lieu de 0.2)
        double probeDistance = RAYON + 0.4;
        int numProbes = 12; // Plus de sondes pour une détection plus fine des coins

        for (int i = 0; i < numProbes; i++) {
            double angle = 2 * Math.PI * i / numProbes;
            double probeX = x + probeDistance * Math.cos(angle);
            double probeY = y + probeDistance * Math.sin(angle);

            if (collision.isColliding(probeX, probeY, RAYON)) {
                // Force inversement proportionnelle à la distance (plus on est près, plus on pousse)
                separationX -= Math.cos(angle);
                separationY -= Math.sin(angle);
            }
        }

        double mag = Math.sqrt(separationX * separationX + separationY * separationY);
        if (mag > 0) {
            // Force de répulsion plus importante (1.2 au lieu de 0.5)
            double separationForce = 1.2;
            // L'ajout d'un petit mouvement aléatoire évite les équilibres parfaits dans les coins
            separationX = (separationX / mag) * separationForce + (Math.random() - 0.5) * 0.05;
            separationY = (separationY / mag) * separationForce + (Math.random() - 0.5) * 0.05;

            // On applique directement sur la position pour un dégagement immédiat
            // plutôt que de passer uniquement par le steering qui est parfois trop lent
            if (!collision.isColliding(x + separationX * 0.1, y, RAYON)) x += separationX * 0.02;
            if (!collision.isColliding(x, y + separationY * 0.1, RAYON)) y += separationY * 0.02;
        }
    }

    /**
     * Applique une force de séparation par rapport aux autres monstres
     * pour éviter qu'ils ne se chevauchent (Social Steering).
     */
    public void applySocialSteering(List<Monstre> autresMonstres) {
        double separationX = 0;
        double separationY = 0;
        double minDistance = RAYON * 4; // Distance minimale souhaitée entre les monstres
        
        for (Monstre autre : autresMonstres) {
            if (autre != this) {
                double dx = x - autre.getX();
                double dy = y - autre.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 0 && dist < minDistance) {
                    separationX += dx / dist; // Force inverse proportionnelle
                    separationY += dy / dist;
                }
            }
        }
        
        double mag = Math.sqrt(separationX * separationX + separationY * separationY);
        if (mag > 0) {
            double separationForce = 0.8; // Modulateur de la force
            separationX = (separationX / mag) * separationForce;
            separationY = (separationY / mag) * separationForce;
            steering.applySeparation(separationX, separationY);
        }
    }

    /**
     * Propage l'alerte aux monstres environnants lorsqu'une cible est détectée.
     */
    public void alerterAutresMonstres() {
        if (target == null) return;
        double rayonAlerte = 15.0;

        // On crie sa propre position ou celle de la cible pour alerter
        for (Monstre autre : tousLesMonstres) {
            if (autre != this && autre.getEtat() != Automate.Etat.POURSUITE) {
                double dist = Math.sqrt(Math.pow(autre.getX() - x, 2) + Math.pow(autre.getY() - y, 2));
                if (dist <= rayonAlerte) {
                    // Partage la cible, et lui dit d'aller vers nous (ou un peu avant la cible)
                    autre.recevoirAlerte(target.getX(), target.getY(), target);
                }
            }
        }
    }
}
