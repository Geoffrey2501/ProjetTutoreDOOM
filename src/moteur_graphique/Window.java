package moteur_graphique;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class Window extends JFrame {

    // Composant de dessin
    private final CanvasPanel panelDessin;
    private GameRenderer renderer;
    private GameRenderer minimapRenderer;

    private int currentFps = 0;

    public void setFPS(int fps) {
        this.currentFps = fps;
    }

    private void dessinerFPS(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font(FONT_ARIAL, Font.BOLD, 14));

        // Petit fond sombre pour la lisibilité
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(5, 5, 70, 22);

        // Texte en jaune
        g2d.setColor(Color.YELLOW);
        g2d.drawString("FPS: " + currentFps, 12, 21);
    }

    // --- GESTION UI (Logs & Scoreboard) ---
    private final List<LogMessage> logMessages = new CopyOnWriteArrayList<>();
    private static final int MAX_LOGS = 5;
    private static final long LOG_DURATION_MS = 5000;

    private boolean showScoreboard = false;
    private final List<String> playerList = new CopyOnWriteArrayList<>();
    private String localPlayerName = "";

    // Couleurs & Fontes
    private static final String FONT_ARIAL = "Arial";
    private static final Color[] PLAYER_COLORS = {
            new Color(0, 150, 255), new Color(255, 100, 100), new Color(255, 200, 0),
            new Color(200, 100, 255), new Color(255, 150, 50), new Color(100, 255, 255),
            new Color(255, 100, 200), new Color(150, 255, 150)
    };

    /**
     * Tronquer un nom s'il dépasse la longueur maximale
     */
    private static final int MAX_NAME_LENGTH = 20;

    private String truncateName(String name) {
        if (name == null) return "";
        if (name.length() <= MAX_NAME_LENGTH) {
            return name;
        }
        return name.substring(0, MAX_NAME_LENGTH - 3) + "...";
    }

    // Classe interne pour les logs (déplacée ici)
    private static class LogMessage {
        String text;
        long timestamp;
        Color color;
        LogMessage(String text, Color color) {
            this.text = text;
            this.color = color;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() { return System.currentTimeMillis() - timestamp > LOG_DURATION_MS; }
    }

    public Window(int width, int height) {
        super("Doom-like Java Engine");
        setSize(width, height);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Panel personnalisé pour le dessin
        panelDessin = new CanvasPanel();
        panelDessin.setBackground(Color.BLACK);
        panelDessin.setFocusable(true);
        disableFocusTraversal(panelDessin);

        add(panelDessin);

        // Gestion fermeture propre
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e){
                System.exit(0);
            }
        });

        setVisible(true);
        panelDessin.requestFocusInWindow();
    }

    private static void disableFocusTraversal(Component component) {
        component.setFocusTraversalKeysEnabled(false);
        Set<AWTKeyStroke> emptySet = new HashSet<>();
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptySet);
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptySet);
    }

    public void setRenderer(GameRenderer renderer) {
        this.renderer = renderer;
    }

    public void setMinimapRenderer(GameRenderer minimapRenderer) {
        this.minimapRenderer = minimapRenderer;
    }

    /**
     * Appelé par la boucle de jeu pour rafraîchir l'écran
     */
    public void draw() {
        panelDessin.repaint();
    }

    // --- API pour MainGame (Délégation vers le Panel) ---
    // Pour attacher les listeners (Input) sans exposer le panel directement
    public void addInputListener(java.util.EventListener listener) {
        if (listener instanceof java.awt.event.KeyListener) {
            panelDessin.addKeyListener((java.awt.event.KeyListener) listener);
        }
        if (listener instanceof java.awt.event.MouseListener) {
            panelDessin.addMouseListener((java.awt.event.MouseListener) listener);
        }
        if (listener instanceof java.awt.event.MouseMotionListener) {
            panelDessin.addMouseMotionListener((java.awt.event.MouseMotionListener) listener);
        }
    }

    // Pour la gestion de la souris (Robot)
    public int getWidth() { return panelDessin.getWidth(); }
    public int getHeight() { return panelDessin.getHeight(); }
    public Point getLocationOnScreen() { return panelDessin.getLocationOnScreen(); }

    public void setCursor(Cursor cursor) {
        panelDessin.setCursor(cursor); // Appliquer au panel, pas à la frame
    }

    // --- LOGIQUE UI ---

    public void addLogMessage(String message, Color color) {
        logMessages.add(new LogMessage(message, color));
        while (logMessages.size() > MAX_LOGS) logMessages.remove(0);
    }

    public void setShowScoreboard(boolean show) { this.showScoreboard = show; }

    public void updatePlayerList(String localPlayer, List<String> remotePlayers) {
        this.localPlayerName = localPlayer;
        this.playerList.clear();
        this.playerList.add(localPlayer);
        this.playerList.addAll(remotePlayers);
    }

    // --- PARTIE DESSIN (Interne) ---

    private class CanvasPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 1. Dessiner le jeu (Raycasting ou autre)
            if (renderer != null) {
                renderer.render(g, getWidth(), getHeight());
            }

            // 2. Dessiner la minimap par-dessus (si disponible)
            if (minimapRenderer != null) {
                minimapRenderer.render(g, getWidth(), getHeight());
            }

            // 3. Dessiner l'UI par dessus
            dessinerFPS(g);
            dessinerLogs(g);
            if (showScoreboard) {
                dessinerScoreboard(g, getWidth(), getHeight());
            }
        }
    }

    private void dessinerLogs(Graphics g) {
        logMessages.removeIf(LogMessage::isExpired);
        if (logMessages.isEmpty()) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font(FONT_ARIAL, Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();

        int y = 30;
        for (LogMessage log : logMessages) {
            long age = System.currentTimeMillis() - log.timestamp;
            float alpha = 1.0f;
            if (age > LOG_DURATION_MS - 1000) alpha = (LOG_DURATION_MS - age) / 1000.0f;

            g2d.setColor(new Color(0, 0, 0, (int)(150 * alpha)));
            g2d.fillRect(10, y - fm.getAscent(), fm.stringWidth(log.text) + 10, fm.getHeight() + 4);

            g2d.setColor(new Color(log.color.getRed(), log.color.getGreen(), log.color.getBlue(), (int)(255 * alpha)));
            g2d.drawString(log.text, 15, y);
            y += fm.getHeight() + 8;
        }
    }

    private void dessinerScoreboard(Graphics g, int screenWidth, int screenHeight) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dimensions du tableau
        int tableWidth = 400;
        int rowHeight = 40;
        int headerHeight = 50;
        int tableHeight = headerHeight + (playerList.size() * rowHeight) + 20;

        // Position centrée
        int tableX = (screenWidth - tableWidth) / 2;
        int tableY = (screenHeight - tableHeight) / 2;

        // Fond semi-transparent
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(tableX, tableY, tableWidth, tableHeight, 20, 20);

        // Bordure
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(tableX, tableY, tableWidth, tableHeight, 20, 20);

        // Titre
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fmTitle = g2d.getFontMetrics();
        String title = "JOUEURS EN LIGNE";
        int titleWidth = fmTitle.stringWidth(title);
        g2d.setColor(Color.WHITE);
        g2d.drawString(title, tableX + (tableWidth - titleWidth) / 2, tableY + 35);

        // Ligne de séparation sous le titre
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawLine(tableX + 20, tableY + headerHeight, tableX + tableWidth - 20, tableY + headerHeight);

        // Liste des joueurs
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        FontMetrics fm = g2d.getFontMetrics();

        int y = tableY + headerHeight + 30;
        int index = 1;

        for (String playerName : playerList) {
            // Indicateur pour le joueur local
            boolean isLocal = playerName.equals(localPlayerName);
            Color playerColor = getPlayerColor(playerName);

            // Numéro du joueur
            g2d.setColor(new Color(150, 150, 150));
            g2d.drawString(String.valueOf(index) + ".", tableX + 30, y);

            // Icône joueur (petit cercle coloré)
            g2d.setColor(playerColor);
            g2d.fillOval(tableX + 60, y - 12, 15, 15);

            // Nom du joueur
            // Version plus claire de la couleur pour le texte
            Color textColor = new Color(
                    Math.min(255, playerColor.getRed() + 55),
                    Math.min(255, playerColor.getGreen() + 55),
                    Math.min(255, playerColor.getBlue() + 55)
            );
            g2d.setColor(textColor);

            if (isLocal) {
                g2d.drawString(truncateName(playerName) + " (vous)", tableX + 85, y);
            } else {
                g2d.drawString(truncateName(playerName), tableX + 85, y);
            }

            y += rowHeight;
            index++;
        }
    }

    private Color getPlayerColor(String playerName) {
        if (playerName.equals(localPlayerName)) return new Color(0, 200, 0);
        int index = (playerName.hashCode() & 0x7FFFFFFF) % PLAYER_COLORS.length;
        return PLAYER_COLORS[index];
    }
}