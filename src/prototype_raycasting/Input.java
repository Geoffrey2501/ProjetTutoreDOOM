package prototype_raycasting;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Set;

public class Input extends KeyAdapter implements MouseListener, MouseMotionListener {

    public boolean forward, backward, strafeLeft, strafeRight, turnLeft, turnRight;
    public boolean showScoreboard; // Touche Tab pour afficher le tableau des joueurs
    public int mouseX, mouseY;
    public boolean escape;
    public boolean mouseLeftClicked;

    /**
     * Désactive le focus traversal pour que Tab soit capturé
     * Appeler cette méthode avec le composant principal (JFrame)
     */
    public static void disableFocusTraversal(Component component) {
        component.setFocusTraversalKeysEnabled(false);
        // Désactiver les touches de traversal
        Set<AWTKeyStroke> emptySet = new HashSet<>();
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptySet);
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptySet);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        set(e.getKeyCode(), true);
    }
    @Override
    public void keyReleased(KeyEvent e) {
        set(e.getKeyCode(), false);
    }

    private void set(int code, boolean down) {
        switch (code) {
            case KeyEvent.VK_Z, KeyEvent.VK_UP      -> forward = down;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN    -> backward = down;
            case KeyEvent.VK_Q                      -> strafeLeft = down;
            case KeyEvent.VK_D                      -> strafeRight = down;
            case KeyEvent.VK_LEFT                   -> turnLeft = down;
            case KeyEvent.VK_RIGHT                  -> turnRight = down;
            case KeyEvent.VK_TAB                    -> showScoreboard = down;
            case KeyEvent.VK_ESCAPE                 -> escape = down;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseLeftClicked = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
