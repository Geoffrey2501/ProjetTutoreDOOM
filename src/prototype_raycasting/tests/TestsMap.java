package prototype_raycasting.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import prototype_raycasting.Map;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestsMap {

    private static final String TEST_FILE = "test_map.txt";

    @AfterEach
    void tearDown() {
        File f = new File(TEST_FILE);
        if (f.exists()) {
            f.delete();
        }
    }

    @Test
    void testMapChargementValide() {
        String content =
            "11111\n" +
            "10001\n" +
            "10101\n" +
            "11111";
        createTempFile(TEST_FILE, content);

        Map map = new Map(TEST_FILE);

        assertEquals(5, map.getWIDTH(), "La largeur de la carte devrait être 5");
        assertEquals(4, map.getHEIGHT(), "La hauteur de la carte devrait être 4");

        boolean[][] grid = map.getMap();
        assertTrue(grid[0][0], "Le coin (0,0) devrait être un mur");
        assertFalse(grid[1][1], "La case (1,1) devrait être vide");
        assertTrue(grid[2][2], "La case (2,2) devrait être un mur");
    }

    @Test
    void testIsWall() {
        String content =
            "111\n" +
            "101\n" +
            "111";
        createTempFile(TEST_FILE, content);

        Map map = new Map(TEST_FILE);

        // Test des murs internes
        assertTrue(map.isWall(0, 0), "isWall(0,0) devrait retourner true");
        assertFalse(map.isWall(1, 1), "isWall(1,1) devrait retourner false");

        // Test des limites (Out of bounds) - Doit retourner true (mur)
        assertTrue(map.isWall(-1, 0), "isWall(-1,0) hors limites devrait retourner true");
        assertTrue(map.isWall(0, -1), "isWall(0,-1) hors limites devrait retourner true");
        assertTrue(map.isWall(3, 0), "isWall(3,0) hors limites devrait retourner true");
        assertTrue(map.isWall(0, 3), "isWall(0,3) hors limites devrait retourner true");
    }

    @Test
    void testMapInvalide() {
        // Création d'une map non rectangulaire
        String content =
            "111\n" +
            "10\n" +
            "111";
        createTempFile(TEST_FILE, content);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Map(TEST_FILE);
        });

        // On vérifie que le message d'erreur contient bien ce qu'on attend
        // Note: Le message exact dépend de l'implémentation dans Map.java
        assertTrue(exception.getMessage().contains("Map non rectangulaire") ||
                   exception.getMessage().contains("Largeur invalide"),
                   "Le message d'erreur devrait indiquer le problème de dimensions");
    }

    private void createTempFile(String filename, String content) {
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(content);
        } catch (IOException e) {
            fail("Impossible de créer le fichier de test " + filename);
        }
    }
}
