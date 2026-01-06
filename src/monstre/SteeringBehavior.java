package monstre;

/**
 * Classe implémentant les comportements de Steering Behavior
 * pour des déplacements naturels et fluides.
 */
public class SteeringBehavior {
    private double maxVelocity = 5.0;
    private double maxForce = 0.2;
    private double arrivalRadius = 15.0;
    private double slowingRadius = 60.0;

    private double velocityX = 0;
    private double velocityY = 0;
    private double rotation = 0;

    /**
     * Comportement Seek : se dirige vers la cible à vitesse maximale
     */
    public void seek(double currentX, double currentY, double targetX, double targetY) {
        double desiredX = targetX - currentX;
        double desiredY = targetY - currentY;

        double magnitude = Math.sqrt(desiredX * desiredX + desiredY * desiredY);
        if (magnitude > 0) {
            desiredX = (desiredX / magnitude) * maxVelocity;
            desiredY = (desiredY / magnitude) * maxVelocity;
        }

        double steeringX = clamp(desiredX - velocityX, -maxForce, maxForce);
        double steeringY = clamp(desiredY - velocityY, -maxForce, maxForce);

        velocityX = clamp(velocityX + steeringX, -maxVelocity, maxVelocity);
        velocityY = clamp(velocityY + steeringY, -maxVelocity, maxVelocity);

        updateRotation();
    }

    /**
     * Comportement Arrive : ralentit progressivement à l'approche de la cible
     */
    public void arrive(double currentX, double currentY, double targetX, double targetY) {
        double distance = Math.sqrt(Math.pow(targetX - currentX, 2) + Math.pow(targetY - currentY, 2));

        if (distance < arrivalRadius) {
            velocityX *= 0.85;
            velocityY *= 0.85;
            return;
        }

        double speed = maxVelocity;
        if (distance < slowingRadius) {
            speed = maxVelocity * (distance / slowingRadius);
        }

        double desiredX = (targetX - currentX) / distance * speed;
        double desiredY = (targetY - currentY) / distance * speed;

        double steeringX = clamp(desiredX - velocityX, -maxForce, maxForce);
        double steeringY = clamp(desiredY - velocityY, -maxForce, maxForce);

        velocityX = clamp(velocityX + steeringX, -maxVelocity, maxVelocity);
        velocityY = clamp(velocityY + steeringY, -maxVelocity, maxVelocity);

        updateRotation();
    }

    /**
     * Met à jour la rotation de façon lissée vers la direction du mouvement
     */
    private void updateRotation() {
        if (velocityX != 0 || velocityY != 0) {
            double targetRotation = Math.atan2(velocityY, velocityX);
            double rotationDiff = targetRotation - rotation;

            // Normaliser entre -PI et PI
            while (rotationDiff > Math.PI) rotationDiff -= 2 * Math.PI;
            while (rotationDiff < -Math.PI) rotationDiff += 2 * Math.PI;

            rotation += rotationDiff * 0.15; // Rotation lissée
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // Getters
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getRotation() { return rotation; }
    public double getSpeed() { return Math.sqrt(velocityX * velocityX + velocityY * velocityY); }

    // Setters pour configuration
    public void setMaxVelocity(double maxVelocity) { this.maxVelocity = maxVelocity; }
    public void setMaxForce(double maxForce) { this.maxForce = maxForce; }
    public void setArrivalRadius(double arrivalRadius) { this.arrivalRadius = arrivalRadius; }
    public void setSlowingRadius(double slowingRadius) { this.slowingRadius = slowingRadius; }

    /**
     * Réinitialise la vélocité (arrêt complet)
     */
    public void reset() {
        velocityX = 0;
        velocityY = 0;
    }
}
