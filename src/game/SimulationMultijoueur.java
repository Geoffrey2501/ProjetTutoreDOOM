package game;

import javax.swing.*;
import java.awt.*;

/**
 * Simulation d'un jeu multijoueur avec 3 joueurs.
 * Lance 3 instances du jeu sur des ports différents avec des fenêtres séparées.
 *
 * Joueur 1 (Serveur) : Port 5001 - Démarre en premier
 * Joueur 2 : Port 5002 - Se connecte à Joueur 1
 * Joueur 3 : Port 5003 - Se connecte à Joueur 1 (le maillage P2P s'occupera du reste)
 *
 * @author Groupe DOOM
 * @version 1.0
 */
public class SimulationMultijoueur {

    // Configuration des joueurs
    private static final String[] PLAYER_NAMES = {"Alice", "Bob", "Charlie"};
    private static final int[] PLAYER_PORTS = {5001, 5002, 5003};
    private static final Color[] PLAYER_COLORS = {Color.RED, Color.GREEN, Color.BLUE};

    // Positions de départ différentes pour chaque joueur
    private static final double[][] START_POSITIONS = {
        {150, 150, 0},      // Alice: en haut à gauche
        {350, 150, 180},    // Bob: en haut à droite, regarde à gauche
        {250, 350, 90}      // Charlie: en bas au centre, regarde en haut
    };

    private MainGameMultiplayer[] games;
    private JFrame controlPanel;

    public SimulationMultijoueur() {
        games = new MainGameMultiplayer[3];
    }

    /**
     * Lance la simulation avec 3 joueurs.
     */
    public void start() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     SIMULATION MULTIJOUEUR DOOM-LIKE - 3 JOUEURS           ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║  Joueur 1: Alice  (Port 5001) - Serveur initial            ║");
        System.out.println("║  Joueur 2: Bob    (Port 5002) - Se connecte à Alice        ║");
        System.out.println("║  Joueur 3: Charlie(Port 5003) - Se connecte à Alice        ║");
        System.out.println("║                                                            ║");
        System.out.println("║  Le maillage P2P complet se formera automatiquement !      ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Créer le panneau de contrôle
        createControlPanel();

        // Lancer les joueurs avec un délai entre chacun
        launchPlayersSequentially();
    }

    /**
     * Lance les 3 joueurs de manière séquentielle avec des délais.
     */
    private void launchPlayersSequentially() {
        // Thread pour lancer les joueurs avec délais
        new Thread(() -> {
            try {
                // === JOUEUR 1 : Alice (Serveur) ===
                System.out.println("\n[SIMULATION] Démarrage de " + PLAYER_NAMES[0] + " sur le port " + PLAYER_PORTS[0] + "...");
                games[0] = createPlayer(0, null, 0);
                games[0].start();
                System.out.println("[SIMULATION] ✓ " + PLAYER_NAMES[0] + " démarré !");

                // Attendre que le serveur soit prêt
                Thread.sleep(2000);

                // === JOUEUR 2 : Bob ===
                System.out.println("\n[SIMULATION] Démarrage de " + PLAYER_NAMES[1] + " sur le port " + PLAYER_PORTS[1] + "...");
                System.out.println("[SIMULATION] " + PLAYER_NAMES[1] + " se connecte à " + PLAYER_NAMES[0] + "...");
                games[1] = createPlayer(1, "localhost", PLAYER_PORTS[0]);
                games[1].start();
                System.out.println("[SIMULATION] ✓ " + PLAYER_NAMES[1] + " démarré et connecté !");

                // Attendre la connexion
                Thread.sleep(2000);

                // === JOUEUR 3 : Charlie ===
                System.out.println("\n[SIMULATION] Démarrage de " + PLAYER_NAMES[2] + " sur le port " + PLAYER_PORTS[2] + "...");
                System.out.println("[SIMULATION] " + PLAYER_NAMES[2] + " se connecte à " + PLAYER_NAMES[0] + "...");
                games[2] = createPlayer(2, "localhost", PLAYER_PORTS[0]);
                games[2].start();
                System.out.println("[SIMULATION] ✓ " + PLAYER_NAMES[2] + " démarré et connecté !");

                // Résumé
                Thread.sleep(1000);
                System.out.println("\n╔════════════════════════════════════════════════════════════╗");
                System.out.println("║           SIMULATION PRÊTE - 3 JOUEURS ACTIFS              ║");
                System.out.println("╠════════════════════════════════════════════════════════════╣");
                System.out.println("║  Contrôles:                                                ║");
                System.out.println("║    - ZQSD / Flèches : Déplacement                          ║");
                System.out.println("║    - Souris : Rotation                                     ║");
                System.out.println("║    - Tab : Afficher le scoreboard                          ║");
                System.out.println("║    - Échap : Libérer/Capturer la souris                    ║");
                System.out.println("║                                                            ║");
                System.out.println("║  Cliquez sur une fenêtre pour la contrôler !               ║");
                System.out.println("╚════════════════════════════════════════════════════════════╝");

            } catch (InterruptedException e) {
                System.err.println("[SIMULATION] Erreur lors du lancement: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Crée un joueur avec sa configuration.
     *
     * @param index Index du joueur (0, 1, ou 2)
     * @param serverIp IP du serveur à rejoindre (null si serveur initial)
     * @param serverPort Port du serveur à rejoindre
     * @return Instance du jeu
     */
    private MainGameMultiplayer createPlayer(int index, String serverIp, int serverPort) {
        String name = PLAYER_NAMES[index];
        int port = PLAYER_PORTS[index];

        MainGameMultiplayer game = new MainGameMultiplayer(name, port, serverIp, serverPort);

        return game;
    }

    /**
     * Crée un panneau de contrôle pour gérer la simulation.
     */
    private void createControlPanel() {
        controlPanel = new JFrame("Contrôle Simulation - 3 Joueurs");
        controlPanel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        controlPanel.setSize(400, 300);
        controlPanel.setLayout(new BorderLayout());

        // Panel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Titre
        JLabel titleLabel = new JLabel("Simulation Multijoueur DOOM");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Informations sur les joueurs
        for (int i = 0; i < 3; i++) {
            JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel colorLabel = new JLabel("●");
            colorLabel.setForeground(PLAYER_COLORS[i]);
            colorLabel.setFont(new Font("Arial", Font.BOLD, 20));

            JLabel infoLabel = new JLabel(PLAYER_NAMES[i] + " - Port " + PLAYER_PORTS[i]);
            infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));

            playerPanel.add(colorLabel);
            playerPanel.add(infoLabel);
            mainPanel.add(playerPanel);
        }

        mainPanel.add(Box.createVerticalStrut(20));

        // Bouton pour arrêter la simulation
        JButton stopButton = new JButton("Arrêter la simulation");
        stopButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopButton.addActionListener(e -> stopSimulation());
        mainPanel.add(stopButton);

        // Instructions
        mainPanel.add(Box.createVerticalStrut(20));
        JLabel instructionLabel = new JLabel("<html><center>Cliquez sur une fenêtre de jeu<br>pour la contrôler</center></html>");
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setForeground(Color.GRAY);
        mainPanel.add(instructionLabel);

        controlPanel.add(mainPanel, BorderLayout.CENTER);

        // Positionner le panneau de contrôle
        controlPanel.setLocation(50, 50);
        controlPanel.setVisible(true);
    }

    /**
     * Arrête la simulation et ferme toutes les fenêtres.
     */
    private void stopSimulation() {
        System.out.println("\n[SIMULATION] Arrêt de la simulation...");

        for (int i = 0; i < games.length; i++) {
            if (games[i] != null) {
                System.out.println("[SIMULATION] Arrêt de " + PLAYER_NAMES[i] + "...");
                games[i].stop();
            }
        }

        System.out.println("[SIMULATION] Simulation terminée.");
        System.exit(0);
    }

    /**
     * Point d'entrée principal.
     */
    public static void main(String[] args) {

        // Utiliser le look and feel du système
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignorer
        }

        // Lancer la simulation
        SwingUtilities.invokeLater(() -> {
            SimulationMultijoueur simulation = new SimulationMultijoueur();
            simulation.start();
        });
    }
}

