package game;

import entite.Joueur;
import moteur_graphique.raycasting.MapBool;

/**
 * Contrôleur du joueur gérant les mouvements et rotations.
 * Sépare la logique de mouvement de la boucle de jeu principale.
 */
public class PlayerController {

    private final Joueur joueur;
    private final MapBool map;
    private final Input input;

    /**
     * Constructeur du PlayerController.
     * @param joueur
     * @param map
     * @param input
     */
    public PlayerController(Joueur joueur, MapBool map, Input input) {
        this.joueur = joueur;
        this.map = map;
        this.input = input;
    }

    /**
     * Met à jour les mouvements et rotations du joueur.
     * @param delta temps écoulé depuis la dernière mise à jour
     * @return true si le joueur a bougé ou tourné
     */
    public boolean update(double delta) {
        double moveSpeed = GameConfig.MOVE_SPEED * delta * (input.isSprint() ? 2.0 : 1.0);
        double rotSpeed = GameConfig.ROTATION_SPEED * delta;

        boolean moved = handleMovement(moveSpeed);
        moved |= handleKeyboardRotation(rotSpeed);

        return moved;
    }

    /**
     * Gère les mouvements du joueur (ZQSD).
     * @param moveSpeed vitesse de déplacement
     * @return true si le joueur a bougé
     */
    private boolean handleMovement(double moveSpeed) {
        double angle = joueur.getAngle();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double dx = 0;
        double dy = 0;

        // Avant/Arrière
        if (input.isForward()) {
            dx += cos;
            dy += sin;
        }
        if (input.isBackward()) {
            dx -= cos;
            dy -= sin;
        }

        // Strafe gauche/droite
        if (input.isStrafeLeft()) {
            dx += sin;
            dy -= cos;
        }
        if (input.isStrafeRight()) {
            dx -= sin;
            dy += cos;
        }

        if (dx != 0 || dy != 0) {
            dx *= moveSpeed;
            dy *= moveSpeed;
            return applyMovement(dx, dy);
        }

        return false;
    }

    /**
     * Applique le mouvement en vérifiant les collisions avec les murs.
     * @param dx déplacement en X
     * @param dy déplacement en Y
     * @return true si le mouvement a été appliqué
     */
    private boolean applyMovement(double dx, double dy) {
        boolean moved = false;
        double nextX = joueur.getX() + dx;
        double nextY = joueur.getY() + dy;

        // Vérification des collisions séparément pour X et Y (slide le long des murs)
        if (!map.isWall((int) nextX, (int) joueur.getY())) {
            joueur.setX(nextX);
            moved = true;
        }

        if (!map.isWall((int) joueur.getX(), (int) nextY)) {
            joueur.setY(nextY);
            moved = true;
        }

        return moved;
    }

    /**
     * Gère la rotation du joueur via le clavier (flèches gauche/droite).
     * @param rotSpeed vitesse de rotation
     * @return true si le joueur a tourné
     */
    private boolean handleKeyboardRotation(double rotSpeed) {
        double angle = joueur.getAngle();

        if (input.isTurnLeft()) {
            joueur.setAngle(angle - rotSpeed);
            return true;
        } else if (input.isTurnRight()) {
            joueur.setAngle(angle + rotSpeed);
            return true;
        }

        return false;
    }

    /**
     * Applique une rotation de la souris.
     * @param deltaX déplacement horizontal de la souris
     */
    public void applyMouseRotation(int deltaX) {
        if (deltaX != 0) {
            joueur.setAngle(joueur.getAngle() + deltaX * GameConfig.MOUSE_SENSITIVITY);
        }
    }

    public Joueur getJoueur() {
        return joueur;
    }
}

