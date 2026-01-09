package moteur_graphique.BSP;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class MapMur {
    private Mur[] murs;

    public MapMur(Mur[] murs) {
        this.murs = murs;
    }

    public MapMur(String file) {
        List<Mur> listTemp = chargerMap(file);
        this.murs = listTemp.toArray(new Mur[0]);
    }

    //TODO: faire un constructeur qui charge une map à partir d'un fichier texte
    //pour cyprian ? oui

    /**
     * Charger la map à partir d'un fichier texte
     * Contenu d'un fichier :
     * Ligne par mur : x0 y0 x1 y1 texture
     * <p>
     * Exemple :
     * 0 0 100 0 brique.png
     * 100 0 100 100 brique.png
     * 100 100 0 100 bois.png
     * ... etc
     *
     * @param file
     */
    private List<Mur> chargerMap(String file) {
        List<Mur> listeMurs = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String ligne;
            int numLigne = 0;

            while ((ligne = br.readLine()) != null) {
                numLigne++;
                ligne = ligne.trim();

                // Ignorer les lignes vides ou les commentaires commencant par "#"
                if (ligne.isEmpty() || ligne.startsWith("#")) {
                    continue;
                }

                // On découpe la ligne par les espaces
                // Format attendu : x0 y0 x1 y1 texture
                String[] parts = ligne.split("\\s+");

                if (parts.length >= 5) {
                    try {
                        double x0 = Double.parseDouble(parts[0]);
                        double y0 = Double.parseDouble(parts[1]);
                        double x1 = Double.parseDouble(parts[2]);
                        double y1 = Double.parseDouble(parts[3]);
                        String texture = parts[4];

                        listeMurs.add(new Mur(x0, y0, x1, y1, texture));
                    } catch (NumberFormatException e) {
                        System.err.println("Erreur de format numérique à la ligne " + numLigne + ": " + e.getMessage());
                    }
                } else {
                    System.err.println("Ligne mal formée (manque d'infos) ligne " + numLigne);
                }
            }
            System.out.println("Chargement map terminé : " + listeMurs.size() + " murs chargés.");

        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du fichier de map: " + e.getMessage());
        }

        return listeMurs;
    }
    
    public Mur[] getMurs() {
        return murs;
    }
}
