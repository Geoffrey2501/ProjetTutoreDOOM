package monstre;

/**
 * Classe implémentant les comportements de Steering Behavior
 * pour des déplacements naturels et fluides.
 */
public class SteeringBehavior {
    private double maxVelocity = 3.0;
    private double maxForce = 0.3;
    private double arrivalRadius = 5.0;
    private double slowingRadius = 30.0;

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
     * Comportement Seek agressif : se dirige directement vers la cible
     * avec moins de lissage, idéal pour la poursuite à courte distance.
     */
    public void seekAggressive(double currentX, double currentY, double targetX, double targetY) {
        double desiredX = targetX - currentX;
        double desiredY = targetY - currentY;

        double magnitude = Math.sqrt(desiredX * desiredX + desiredY * desiredY);

        // Si très proche, aller directement vers la cible
        if (magnitude < 5) {
            velocityX = desiredX;
            velocityY = desiredY;
            return;
        }

        if (magnitude > 0) {
            desiredX = (desiredX / magnitude) * maxVelocity;
            desiredY = (desiredY / magnitude) * maxVelocity;
        }

        // Force de steering plus élevée pour réagir plus vite
        double aggressiveForce = maxForce * 3;
        double steeringX = clamp(desiredX - velocityX, -aggressiveForce, aggressiveForce);
        double steeringY = clamp(desiredY - velocityY, -aggressiveForce, aggressiveForce);

        velocityX = clamp(velocityX + steeringX, -maxVelocity, maxVelocity);
        velocityY = clamp(velocityY + steeringY, -maxVelocity, maxVelocity);

        updateRotation();
    }

    /**
     * Comportement Arrive : ralentit progressivement à l'approche de la cible
     */
    public void arrive(double currentX, double currentY, double targetX, double targetY) {
        double distance = Math.sqrt(Math.pow(targetX - currentX, 2) + Math.pow(targetY - currentY, 2));

        if (distance < 0.1) {
            velocityX = 0;
            velocityY = 0;
            return;
        }

        // Vitesse proportionnelle à la distance dans la zone de ralentissement
        double speed = maxVelocity;
        if (distance < slowingRadius) {
            speed = maxVelocity * (distance / slowingRadius);
            speed = Math.max(speed, 0.5); // Vitesse minimale pour toujours avancer vers la cible
        }

        // Toujours se diriger vers la cible (même en freinant)
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

    public void killVelocityX() {
        this.velocityX = 0;
    }

    public void killVelocityY() {
        this.velocityY = 0;
    }

    /**
     * Applique une force de séparation pour repousser le monstre d'un mur.
     * Ignore la limite maxForce pour garantir un dégagement rapide.
     */
    public void applySeparation(double forceX, double forceY) {
        velocityX = clamp(velocityX + forceX, -maxVelocity, maxVelocity);
        velocityY = clamp(velocityY + forceY, -maxVelocity, maxVelocity);
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
