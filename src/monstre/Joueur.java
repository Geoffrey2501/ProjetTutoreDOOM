package monstre;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe représentant le joueur contrôlé au clavier (flèches directionnelles ou ZQSD).
 * Le joueur se déplace dans le labyrinthe 2D avec gestion des collisions.
 * Utilise les Key Bindings Swing (InputMap/ActionMap) pour une réception fiable des touches.
 */
public class Joueur {
    public static final int RAYON = 5;
    private double x;
    private double y;
    private double vitesse = 2.5;
    private Map map;

    // Touches actuellement enfoncées (pour mouvement continu et diagonal)
    private final Set<String> touchesEnfoncees = new HashSet<>();

    public Joueur(double x, double y, Map map) {
        this.x = x;
        this.y = y;
        this.map = map;
    }

    /**
     * Installe les key bindings sur un JComponent pour capter les touches.
     * Utilise WHEN_IN_FOCUSED_WINDOW pour fonctionner même sans le focus direct.
     */
    public void setupKeyBindings(JComponent component) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();

        // Flèches + ZQSD/WASD
        String[][] bindings = {
            {"UP",    "pressed UP",    "released UP"},
            {"DOWN",  "pressed DOWN",  "released DOWN"},
            {"LEFT",  "pressed LEFT",  "released LEFT"},
            {"RIGHT", "pressed RIGHT", "released RIGHT"},
            {"Z",     "pressed Z",     "released Z"},
            {"S",     "pressed S",     "released S"},
            {"Q",     "pressed Q",     "released Q"},
            {"D",     "pressed D",     "released D"},
            {"W",     "pressed W",     "released W"},
            {"A",     "pressed A",     "released A"},
        };

        for (String[] b : bindings) {
            String direction = b[0];
            String pressedKey = b[1];
            String releasedKey = b[2];

            inputMap.put(KeyStroke.getKeyStroke(pressedKey), pressedKey);
            inputMap.put(KeyStroke.getKeyStroke(releasedKey), releasedKey);

            actionMap.put(pressedKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    touchesEnfoncees.add(direction);
                }
            });
            actionMap.put(releasedKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    touchesEnfoncees.remove(direction);
                }
            });
        }
    }

    /**
     * Met à jour la position du joueur en fonction des touches enfoncées.
     * Appelé à chaque frame par la boucle de jeu.
     */
    public void update() {
        double dx = 0;
        double dy = 0;

        if (touchesEnfoncees.contains("UP")    || touchesEnfoncees.contains("Z") || touchesEnfoncees.contains("W")) dy -= 1;
        if (touchesEnfoncees.contains("DOWN")  || touchesEnfoncees.contains("S"))                                   dy += 1;
        if (touchesEnfoncees.contains("LEFT")  || touchesEnfoncees.contains("Q") || touchesEnfoncees.contains("A")) dx -= 1;
        if (touchesEnfoncees.contains("RIGHT") || touchesEnfoncees.contains("D"))                                   dx += 1;

        // Pas de mouvement
        if (dx == 0 && dy == 0) return;

        // Normaliser pour que le mouvement diagonal ne soit pas plus rapide
        double magnitude = Math.sqrt(dx * dx + dy * dy);
        dx = (dx / magnitude) * vitesse;
        dy = (dy / magnitude) * vitesse;

        double newX = x + dx;
        double newY = y + dy;

        // Glissade indépendante sur chaque axe
        if (!collidesWithWall(newX, newY)) {
            x = newX;
            y = newY;
        } else {
            if (!collidesWithWall(newX, y)) {
                x = newX;
            }
            if (!collidesWithWall(x, newY)) {
                y = newY;
            }
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

    // --- Getters ---

    public double getX() { return x; }
    public double getY() { return y; }

    public void setVitesse(double vitesse) {
        this.vitesse = vitesse;
    }
}
