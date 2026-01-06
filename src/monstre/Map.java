package monstre;

public class Map {
    private int largeur;
    private int hauteur;

    private Mur[] murs = {
        new Mur(100, 100, 150, 150),   // Carré en haut à gauche
        new Mur(250, 80, 350, 130),    // Rectangle horizontal
        new Mur(450, 100, 500, 200),   // Rectangle vertical
        new Mur(150, 250, 250, 350),   // Carré au milieu gauche
        new Mur(350, 300, 450, 350),   // Rectangle au milieu droit
        new Mur(200, 450, 300, 550),   // Rectangle en bas
        new Mur(400, 400, 500, 500),   // Carré en bas à droite
        new Mur(50, 400, 100, 500),    // Carré en bas à gauche
    };


    public Map(int largeur, int hauteur) {
        this.largeur = largeur;
        this.hauteur = hauteur;
    }

    public int getLargeur() {
        return largeur;
    }

    public int getHauteur() {
        return hauteur;
    }

    public boolean estDansMur(int x, int y) {
        for (Mur mur : murs) {
            if (mur.esDansMur(x, y)) {
                return true;
            }
        }
        return false;
    }

    public Mur[] getMurs() {
        return murs;
    }

    public boolean traverseMur(int x1, int y1, int x2, int y2) {
        for (Mur mur : murs) {
            if (mur.esDansMurSegment(x1, y1, x2, y2)) {
                return true;
            }
        }
        return false;
    }
}
