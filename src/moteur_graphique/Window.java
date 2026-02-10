package moteur_graphique;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Window extends JFrame {

    // Composant de dessin
    private final CanvasPanel panelDessin;
    private GameRenderer renderer;

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
        panelDessin.setFocusable(true); // Important pour les KeyListeners
        panelDessin.requestFocus();

        add(panelDessin);

        // Gestion fermeture propre
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e){
                System.exit(0);
            }
        });

        setVisible(true);
    }

    public void setRenderer(GameRenderer renderer) {
        this.renderer = renderer;
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

            // 2. Dessiner l'UI par dessus
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
        // (Copier ici tout le code de dessinerScoreboard de l'ancien Raycasting.java)
        // Je le simplifie pour la réponse, mais tu remets ton code exact ici
        Graphics2D g2d = (Graphics2D) g;
        int tableWidth = 400;
        int tableHeight = 50 + (playerList.size() * 40) + 20;
        int tableX = (screenWidth - tableWidth) / 2;
        int tableY = (screenHeight - tableHeight) / 2;

        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRoundRect(tableX, tableY, tableWidth, tableHeight, 20, 20);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(FONT_ARIAL, Font.BOLD, 24));
        g2d.drawString("JOUEURS EN LIGNE", tableX + 80, tableY + 35);

        g2d.setFont(new Font(FONT_ARIAL, Font.PLAIN, 18));
        int y = tableY + 80;
        int i = 1;
        for(String p : playerList) {
            g2d.setColor(getPlayerColor(p));
            g2d.fillOval(tableX + 60, y - 12, 15, 15);
            g2d.setColor(Color.WHITE);
            g2d.drawString(p, tableX + 85, y);
            y += 40;
        }
    }

    private Color getPlayerColor(String playerName) {
        if (playerName.equals(localPlayerName)) return new Color(0, 200, 0);
        int index = (playerName.hashCode() & 0x7FFFFFFF) % PLAYER_COLORS.length;
        return PLAYER_COLORS[index];
    }
}