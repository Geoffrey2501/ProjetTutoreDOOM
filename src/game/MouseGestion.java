package game;

import moteur_graphique.Window;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gère la capture et le recentrage de la souris.
 * Permet de basculer entre le mode "jeu" (souris capturée) et le mode "menu" (souris libre).
 */
public class MouseGestion {

    private static final Logger LOGGER = Logger.getLogger(MouseGestion.class.getName());

    private final Window window;
    private final Input input;
    private Robot robot;

    private final Cursor blankCursor;
    private final Cursor defaultCursor;
    private final Point centerPoint;

    private boolean mouseCaptured = true;
    private boolean escapePressed = false;

    /**
     * Constructeur de MouseCaptureHandler.
     * Initialise les curseurs, le Robot et applique le curseur invisible par défaut<;
     * @param window
     * @param input
     */
    public MouseGestion(Window window, Input input) {
        this.window = window;
        this.input = input;
        this.centerPoint = new Point();

        // Création du curseur invisible
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
        defaultCursor = Cursor.getDefaultCursor();

        // Initialisation du Robot pour contrôler la souris
        try {
            robot = new Robot();
        } catch (AWTException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du Robot", e);
        }

        // Application du curseur invisible par défaut
        window.setCursor(blankCursor);
    }

    /**
     * Met à jour l'état de la capture de la souris.
     * Appelé à chaque frame pour gérer les entrées Escape et clic gauche.
     */
    public void update() {
        // Gestion de la touche Escape pour basculer l'état
        if (input.isEscape()) {
            if (!escapePressed) {
                escapePressed = true;
                toggleMouseCapture(!mouseCaptured);
            }
        } else {
            escapePressed = false;
        }

        // Clic gauche pour recapturer la souris si elle est libre
        if (!mouseCaptured && input.isMouseLeftClicked()) {
            toggleMouseCapture(true);
            input.resetMouseLeftClicked();
        }
    }

    /**
     * Gère la rotation de la souris et retourne le delta X si la souris est capturée.
     * @return le déplacement horizontal de la souris, ou 0 si la souris n'est pas capturée
     */
    public int handleMouseRotation() {
        if (!mouseCaptured || !window.isVisible()) {
            return 0;
        }

        int width = window.getWidth();
        int height = window.getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        int deltaX = input.getMouseX() - centerX;

        if (deltaX != 0) {
            // Recentre la souris au milieu de l'écran
            centerPoint.setLocation(centerX, centerY);
            SwingUtilities.convertPointToScreen(centerPoint, window);
            robot.mouseMove(centerPoint.x, centerPoint.y);

            input.setMouseX(centerX);
            input.setMouseY(centerY);
        }

        return deltaX;
    }

    /**
     * Bascule l'état de la capture de la souris.
     * @param capture true pour capturer, false pour libérer
     */
    private void toggleMouseCapture(boolean capture) {
        mouseCaptured = capture;
        if (mouseCaptured) {
            window.setCursor(blankCursor);
            recenterMouse();
        } else {
            window.setCursor(defaultCursor);
        }
    }

    /**
     * Recentre la souris au milieu de la fenêtre.
     */
    private void recenterMouse() {
        if (window.isVisible()) {
            centerPoint.setLocation(window.getWidth() / 2, window.getHeight() / 2);
            SwingUtilities.convertPointToScreen(centerPoint, window);
            robot.mouseMove(centerPoint.x, centerPoint.y);
            input.setMouseX(window.getWidth() / 2);
            input.setMouseY(window.getHeight() / 2);
        }
    }

    public boolean isMouseCaptured() {
        return mouseCaptured;
    }
}

