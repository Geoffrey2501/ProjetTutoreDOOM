package prototype_raycasting;

public class Joueur {
    private double x;
    private double y;
    private double angle;

    public Joueur(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
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