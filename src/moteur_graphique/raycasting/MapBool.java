package moteur_graphique.raycasting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapBool {
    private int width;
    private int height;

    private boolean[][] grid; // 1 = mur | 0 = vide

    public MapBool() {
        width = 10;
        height = 10;
        grid = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y == 0 || y == height - 1 || x == 0 || x == width - 1) {
                    grid[y][x] = true; // mur
                } else {
                    grid[y][x] = false; // vide
                }
            }
        }
    }

    public MapBool(String fichier) {
        loadMap(fichier);
    }

    private void loadMap(String fichier) {
        List<String> lines = readMapFile(fichier);
        validateMapDimensions(lines, fichier);
        validateMapContent(lines, fichier);
        buildMap(lines);
    }

    private List<String> readMapFile(String fichier) {
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
            throw new java.io.UncheckedIOException("Impossible de lire le fichier: " + fichier, e);
        }

        return lines;
    }

    private void validateMapDimensions(List<String> lines, String fichier) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide: " + fichier);
        }

        this.height = lines.size();
        this.width = lines.getFirst().length();

        if (this.width == 0) {
            throw new IllegalArgumentException("Largeur invalide (ligne 1 vide) dans: " + fichier);
        }
    }

    private void validateMapContent(List<String> lines, String fichier) {
        for (int y = 0; y < this.height; y++) {
            String row = lines.get(y);
            if (row.length() != this.width) {
                throw new IllegalArgumentException("Map non rectangulaire dans " + fichier + " : ligne " + (y + 1) + " fait " + row.length()
                        + " mais attendu " + width);
            }
            validateRowCharacters(row, y);
        }
    }

    private void validateRowCharacters(String row, int y) {
        for (int x = 0; x < row.length(); x++) {
            char c = row.charAt(x);
            if (c != '0' && c != '1') {
                throw new IllegalArgumentException(
                        "Caractère invalide '" + c + "' à (x=" + x + ", y=" + y + "). Attendu 0 ou 1."
                );
            }
        }
    }

    private void buildMap(List<String> lines) {
        this.grid = new boolean[this.height][this.width];
        for (int y = 0; y < this.height; y++) {
            String row = lines.get(y);
            for (int x = 0; x < this.width; x++) {
                this.grid[y][x] = (row.charAt(x) == '1');
            }
        }
    }

    public boolean[][] getGrid() {
        return grid;
    }

    public int getWIDTH() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // Méthode pour faire les tests
    public boolean isWall(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return true;
        return grid[y][x];
    }
}
