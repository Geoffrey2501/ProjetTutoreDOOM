package prototype_raycasting;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;

public class MainGame implements Runnable {

    private Map map;
    private Joueur joueur;
    private Raycasting raycasting;
    private Input input;

    private Robot robot;

    private boolean running;
    private final int FPS = 60;
    private final long OPTIMAL_TIME = 1_000_000_000 / FPS;

    public MainGame(){
        map = new Map("src/prototype_raycasting/map/map.txt");
        joueur = new Joueur(2.0, 2.0, 0.0);
        input = new Input();

        raycasting = new Raycasting(map, joueur);

        // Exemple d'ajout d'autres joueurs (décommenter pour tester)
        // Joueur autreJoueur1 = new Joueur(4.0, 4.0, 0.0);
        // Joueur autreJoueur2 = new Joueur(3.0, 3.0, 0.0);
        // raycasting.ajouterAutreJoueur(autreJoueur1);
        // raycasting.ajouterAutreJoueur(autreJoueur2);

        raycasting.addKeyListener(input);
        raycasting.addMouseListener(input);
        raycasting.addMouseMotionListener(input);
        raycasting.setFocusable(true);
        raycasting.requestFocus();
        raycasting.setVisible(true);

        try{
            robot = new Robot();
        }catch (AWTException e){
            e.printStackTrace();
        }

        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        raycasting.getContentPane().setCursor(blankCursor);
    }

    @Override
    public void run() {
        running = true;
        long lastLoopTime = System.nanoTime();

        while (running){
            long now = System.nanoTime();
            long updateLength = now - lastLoopTime;
            lastLoopTime = now;
            double delta = updateLength / 1000000000.0;

            update(delta);
            raycasting.render();

            try {
                Thread.sleep(Math.max(0, (lastLoopTime - System.nanoTime() + OPTIMAL_TIME) / 1000000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update(double delta){
        double moveSpeed = 1.5 * delta; // units per second
        double rotSpeed = 2.0 * delta; // radians per second

        double dx = 0;
        double dy = 0;

        // Movement
        if(input.forward) {
            dx += Math.cos(joueur.getAngle()) * moveSpeed;
            dy += Math.sin(joueur.getAngle()) * moveSpeed;
        }
        if(input.backward) {
            dx -= Math.cos(joueur.getAngle()) * moveSpeed;
            dy -= Math.sin(joueur.getAngle()) * moveSpeed;
        }
        if(input.strafeLeft) {
            dx += Math.sin(joueur.getAngle()) * moveSpeed;
            dy -= Math.cos(joueur.getAngle()) * moveSpeed;
        }
        if(input.strafeRight) {
            dx -= Math.sin(joueur.getAngle()) * moveSpeed;
            dy += Math.cos(joueur.getAngle()) * moveSpeed;
        }

        // Collision detection
        if (!map.isWall((int)(joueur.getX() + dx), (int)joueur.getY())) {
            joueur.setX(joueur.getX() + dx);
        }
        if (!map.isWall((int)joueur.getX(), (int)(joueur.getY() + dy))) {
            joueur.setY(joueur.getY() + dy);
        }

        // Rotation
        if(input.turnLeft) joueur.setAngle(joueur.getAngle() - rotSpeed);
        if(input.turnRight) joueur.setAngle(joueur.getAngle() + rotSpeed);

        if (raycasting.isShowing()) {

            // Trouver le centre de la fenêtre
            int centerX = raycasting.getWidth() / 2;
            int centerY = raycasting.getHeight() / 2;

            // La position actuelle de la souris (via Input listener)
            int currentMouseX = input.mouseX;

            // Calcul de la différence (Delta) par rapport au centre
            int deltaX = currentMouseX - centerX;

            // Si la souris a bougé du centre
            if (deltaX != 0) {
                // On applique la rotation
                // 0.002 est la sensibilité de la souris
                joueur.setAngle(joueur.getAngle() + deltaX * 0.001);

                // --- RECENTRAGE ---
                // On convertit les coordonnées locales (fenêtre) en coordonnées écran global
                // Car Robot utilise les coordonnées de l'écran entier
                Point centerScreen = new Point(centerX, centerY);
                SwingUtilities.convertPointToScreen(centerScreen, raycasting);

                // On remet physiquement la souris au centre
                robot.mouseMove(centerScreen.x, centerScreen.y);

                // IMPORTANT : On doit dire à notre classe Input que la souris est revenue au centre
                // Sinon au prochain tour, elle croira qu'on a encore bougé
                input.mouseX = centerX;
                input.mouseY = centerY;
            }
        }
    }

    static void main(String[] args) {
        MainGame game = new MainGame();
        new Thread(game).start();

    }
}
