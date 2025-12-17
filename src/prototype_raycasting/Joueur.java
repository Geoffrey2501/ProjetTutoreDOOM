package prototype_raycasting;

import java.util.logging.Logger;

/**
 * Classe représentant un joueur dans le jeu Doom-like
 * Supporte la synchronisation réseau pour le multijoueur
 */
public class Joueur {
    private static final Logger LOGGER = Logger.getLogger(Joueur.class.getName());
    private String id;  // Identifiant unique du joueur pour le réseau
    private double x;
    private double y;
    private double angle;
    private boolean positionInitialized = false; // Flag pour savoir si la position est valide

    // Interpolation variables
    private double targetX;
    private double targetY;
    private double targetAngle;
    private static final double INTERPOLATION_SPEED = 25.0; // Augmenté pour être plus réactif (was 10.0)
    private static final double TELEPORT_THRESHOLD = 50.0; // Distance au-delà de laquelle on téléporte directement

    /**
     * Constructeur avec identifiant (pour multijoueur)
     */
    public Joueur(String id, double x, double y, double angle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.angle = normalizeAngle(angle);
        
        // Initialiser les cibles à la position actuelle
        this.targetX = x;
        this.targetY = y;
        this.targetAngle = this.angle;
        
        this.positionInitialized = true; // Position explicitement définie
    }

    /**
     * Constructeur sans identifiant (joueur local par défaut)
     */
    public Joueur(double x, double y, double angle) {
        this("local", x, y, angle);
    }

    /**
     * Constructeur pour joueur distant non initialisé
     */
    public Joueur(String id) {
        this.id = id;
        this.x = 0;
        this.y = 0;
        this.angle = 0;
        this.targetX = 0;
        this.targetY = 0;
        this.targetAngle = 0;
        this.positionInitialized = false; // Position pas encore reçue
    }

    public boolean isPositionInitialized() {
        return positionInitialized;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setAngle(double angle) {
        this.angle = normalizeAngle(angle);
    }

    /**
     * Met à jour toutes les coordonnées en une seule opération (utile pour le réseau)
     */
    public void setPosition(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = normalizeAngle(angle);
        
        // Reset targets to avoid interpolation glitch when teleporting
        this.targetX = x;
        this.targetY = y;
        this.targetAngle = this.angle;
        
        this.positionInitialized = true;
    }

    /**
     * Met à jour la position cible pour l'interpolation
     */
    public void setTargetPosition(double x, double y, double angle) {
        this.targetX = x;
        this.targetY = y;
        this.targetAngle = normalizeAngle(angle);
        this.positionInitialized = true;
    }

    /**
     * Interpole la position actuelle vers la position cible
     * @param delta Temps écoulé depuis la dernière frame (en secondes)
     */
    public void interpolate(double delta) {
        if (!positionInitialized) return;
        
        // Si la distance est trop grande, on téléporte directement (éviter le "sliding" sur longue distance)
        double distSq = (targetX - x) * (targetX - x) + (targetY - y) * (targetY - y);
        if (distSq > TELEPORT_THRESHOLD * TELEPORT_THRESHOLD) {
            x = targetX;
            y = targetY;
            angle = targetAngle;
            return;
        }
        
        // Interpolation linéaire simple (LERP)
        double t = Math.min(1.0, delta * INTERPOLATION_SPEED);
        
        this.x += (targetX - this.x) * t;
        this.y += (targetY - this.y) * t;
        
        // Interpolation d'angle (plus complexe à cause du modulo 2PI)
        double diff = targetAngle - this.angle;
        // Ajuster la différence pour prendre le chemin le plus court
        while (diff < -Math.PI) diff += 2 * Math.PI;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        
        this.angle = normalizeAngle(this.angle + diff * t);
    }

    /**
     * Sérialise la position du joueur pour l'envoi réseau
     * Format: "x;y;angle" (utilise ; comme séparateur pour éviter les conflits avec le format décimal)
     */
    public String toNetworkString() {
        return String.format(java.util.Locale.US, "%.4f;%.4f;%.4f", x, y, angle);
    }

    /**
     * Parse une chaîne réseau et met à jour la position
     * Format attendu: "x;y;angle" ou ancien format "x,y,angle"
     * @return true si le parsing a réussi
     */
    public boolean fromNetworkString(String data) {
        try {
            // Supporter les deux séparateurs
            String[] parts;
            if (data.contains(";")) {
                parts = data.split(";");
            } else {
                parts = data.split(",");
            }

            if (parts.length >= 3) {
                double newX = Double.parseDouble(parts[0].trim());
                double newY = Double.parseDouble(parts[1].trim());
                double newAngle = normalizeAngle(Double.parseDouble(parts[2].trim()));
                
                if (!positionInitialized) {
                    // Première fois : téléportation
                    this.x = newX;
                    this.y = newY;
                    this.angle = newAngle;
                    this.targetX = newX;
                    this.targetY = newY;
                    this.targetAngle = newAngle;
                    this.positionInitialized = true;
                } else {
                    // Mises à jour suivantes : interpolation
                    setTargetPosition(newX, newY, newAngle);
                }
                return true;
            } else if (parts.length == 2) {
                // Compatibilité avec l'ancien format x,y sans angle
                double newX = Double.parseDouble(parts[0].trim());
                double newY = Double.parseDouble(parts[1].trim());
                
                if (!positionInitialized) {
                    this.x = newX;
                    this.y = newY;
                    this.targetX = newX;
                    this.targetY = newY;
                    this.positionInitialized = true;
                } else {
                    this.targetX = newX;
                    this.targetY = newY;
                }
                return true;
            }
        } catch (NumberFormatException e) {
            LOGGER.warning("Erreur parsing position joueur: " + data + " - " + e.getMessage());
        }
        return false;
    }

    /**
     * Crée un nouveau joueur à partir d'une chaîne réseau
     */
    public static Joueur fromNetwork(String id, String data) {
        Joueur j = new Joueur(id); // Utiliser le constructeur non initialisé
        j.fromNetworkString(data);
        return j;
    }

    /**
     * Rotation du joueur en degrés
     * @param degrees nombre de degrés à ajouter (positif = sens horaire, négatif = sens antihoraire)
     */
    public void rotateDegrees(double degrees) {
        angle = normalizeAngle(angle + Math.toRadians(degrees));
    }

    /**
     * Rotation du joueur en radians
     * @param radians nombre de radians à ajouter
     */
    public void rotateRadians(double radians) {
        angle = normalizeAngle(angle + radians);
    }

    /**
     * Normalise un angle entre 0 et 2π radians
     * @param a l'angle à normaliser
     * @return l'angle normalisé dans [0, 2π)
     */
    private double normalizeAngle(double a) {
        a = a % (2 * Math.PI);
        if (a < 0) {
            a += 2 * Math.PI;
        }
        return a;
    }

    /**
     * Obtenir l'angle en degrés
     * @return l'angle en degrés
     */
    public double getAngleDegrees() {
        return Math.toDegrees(angle);
    }

    @Override
    public String toString() {
        return String.format("Joueur[x=%.2f, y=%.2f, angle=%.2f° (%.2f rad)]",
                x, y, getAngleDegrees(), angle);
    }
}
