package game;

/**
 * Classe de configuration contenant les constantes du jeu.
 * Centralise tous les paramètres configurables.
 */
public class GameConfig {

    // Configuration de la boucle de jeu
    public static final int FPS = 60;
    public static final long OPTIMAL_TIME = 1_000_000_000 / FPS;

    // Configuration du joueur
    public static final double MOVE_SPEED = 1.5;
    public static final double ROTATION_SPEED = 2.0;
    public static final double MOUSE_SENSITIVITY = 0.001;

    // Position de départ du joueur
    public static final double PLAYER_START_X = 2.0;
    public static final double PLAYER_START_Y = 2.0;
    public static final double PLAYER_START_ANGLE = 0.0;

    // Chemins des ressources
    public static final String MAP_PATH = "assets/maps/map.txt";
    public static final String PLAYER_SPRITE_PATH = "assets/sprites/jonesy.png";

    // Configuration de la fenêtre
    public static final int WINDOW_WIDTH = 1920;
    public static final int WINDOW_HEIGHT = 1080;

}

