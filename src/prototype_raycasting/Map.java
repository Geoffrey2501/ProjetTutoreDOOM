package prototype_raycasting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Map {
    private int WIDTH;
    private int HEIGHT;

    private boolean[][] map; // 1 = mure | 0 = vide

    public Map () {
        this.WIDTH = 0;
        this.HEIGHT = 0;
        this.map = new boolean[0][0];
    }

    public Map(String fichier) {
        loadMap(fichier);
    }

    private void loadMap(String fichier) {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                line = line.replaceAll("\\s+", "");
                lines.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le fichier: " + fichier, e);
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide: " + fichier);
        }

        this.HEIGHT = lines.size();
        this.WIDTH = lines.get(0).length();

        if (this.WIDTH == 0) {
            throw new IllegalArgumentException("Largeur invalide (ligne 1 vide) dans: " + fichier);
        }

        for (int y = 0; y < this.HEIGHT; y++) {
            String row = lines.get(y);

            if (row.length() != this.WIDTH) {
                throw new IllegalArgumentException("Map non rectangulaire : ligne " + (y + 1) + " fait " + row.length()
                        + " mais attendu " + WIDTH);
            }

            for(int x = 0; x < this.WIDTH; x++) {
                char c = row.charAt(x);
                if (c != '0' && c != '1') {
                    throw new IllegalArgumentException(
                            "Caractère invalide '" + c + "' à (x=" + x + ", y=" + y + "). Attendu 0 ou 1."
                    );
                }
            }
        }

        this.map = new boolean[this.HEIGHT][this.WIDTH];
        for(int y = 0; y < this.HEIGHT; y++) {
            String row = lines.get(y);
            for(int x = 0; x < this.WIDTH; x++) {
                this.map[y][x] = (row.charAt(x) == '1');
            }
        }
    }

    public boolean[][] getMap() {
        return map;
    }

    public int getWIDTH() {
        return WIDTH;
    }

    public int getHEIGHT() {
        return HEIGHT;
    }

    // Méthode pour faire les tests
    public boolean isWall(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return true;
        return map[y][x];
    }
}
