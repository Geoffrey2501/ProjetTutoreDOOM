package monstre;

public class Map {
    private int largeur;
    private int hauteur;

    private MurTabBoolean[] murs = {
        // Bordures extérieures
        new MurTabBoolean(0, 0, 600, 10),       // Haut
        new MurTabBoolean(0, 590, 600, 600),    // Bas
        new MurTabBoolean(0, 0, 10, 600),       // Gauche
        new MurTabBoolean(590, 0, 600, 600),    // Droite

        // Labyrinthe - Rangée 1 (haut)
        new MurTabBoolean(80, 10, 100, 120),
        new MurTabBoolean(80, 120, 200, 140),
        new MurTabBoolean(180, 60, 200, 140),
        new MurTabBoolean(260, 10, 280, 180),
        new MurTabBoolean(340, 60, 440, 80),
        new MurTabBoolean(420, 80, 440, 160),
        new MurTabBoolean(500, 10, 520, 120),

        // Labyrinthe - Rangée 2
        new MurTabBoolean(10, 180, 120, 200),
        new MurTabBoolean(100, 200, 120, 280),
        new MurTabBoolean(180, 180, 200, 280),
        new MurTabBoolean(180, 260, 280, 280),
        new MurTabBoolean(260, 200, 280, 260),
        new MurTabBoolean(340, 140, 360, 260),
        new MurTabBoolean(360, 180, 440, 200),
        new MurTabBoolean(420, 200, 440, 280),
        new MurTabBoolean(500, 160, 520, 280),
        new MurTabBoolean(500, 260, 590, 280),

        // Labyrinthe - Rangée 3 (milieu)
        new MurTabBoolean(60, 260, 80, 380),
        new MurTabBoolean(80, 340, 180, 360),
        new MurTabBoolean(160, 320, 180, 340),
        new MurTabBoolean(160, 320, 260, 340),
        new MurTabBoolean(240, 340, 260, 420),
        new MurTabBoolean(320, 300, 340, 400),
        new MurTabBoolean(340, 340, 440, 360),
        new MurTabBoolean(420, 300, 440, 340),
        new MurTabBoolean(500, 320, 520, 420),
        new MurTabBoolean(520, 340, 590, 360),

        // Labyrinthe - Rangée 4
        new MurTabBoolean(10, 420, 100, 440),
        new MurTabBoolean(80, 440, 100, 520),
        new MurTabBoolean(160, 400, 180, 500),
        new MurTabBoolean(160, 480, 260, 500),
        new MurTabBoolean(240, 420, 260, 480),
        new MurTabBoolean(320, 420, 420, 440),
        new MurTabBoolean(400, 400, 420, 420),
        new MurTabBoolean(400, 440, 420, 520),
        new MurTabBoolean(480, 400, 500, 500),
        new MurTabBoolean(500, 480, 590, 500),

        // Labyrinthe - Rangée 5 (bas)
        new MurTabBoolean(60, 540, 80, 590),
        new MurTabBoolean(80, 540, 160, 560),
        new MurTabBoolean(220, 540, 320, 560),
        new MurTabBoolean(300, 560, 320, 590),
        new MurTabBoolean(380, 540, 400, 590),
        new MurTabBoolean(400, 540, 500, 560),
        new MurTabBoolean(540, 540, 560, 590),
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
        for (MurTabBoolean mur : murs) {
            if (mur.esDansMur(x, y)) {
                return true;
            }
        }
        return false;
    }

    public MurTabBoolean[] getMurs() {
        return murs;
    }

    public boolean traverseMur(int x1, int y1, int x2, int y2) {
        for (MurTabBoolean mur : murs) {
            if (mur.esDansMurSegment(x1, y1, x2, y2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si un point avec un rayon donné est dans un mur (collision cercle-rectangle)
     * @param x coordonnée X du centre
     * @param y coordonnée Y du centre
     * @param rayon rayon du cercle
     * @return true si le cercle touche un mur
     */
    public boolean estDansMurAvecRayon(int x, int y, int rayon) {
        for (MurTabBoolean mur : murs) {
            if (mur.cercleIntersecte(x, y, rayon)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si un segment "épais" (avec un rayon) traverse un mur
     * On vérifie le segment principal + les segments décalés perpendiculairement
     * @param x1 début X
     * @param y1 début Y
     * @param x2 fin X
     * @param y2 fin Y
     * @param rayon épaisseur du chemin (rayon du monstre)
     * @return true si le chemin épais traverse un mur
     */
    public boolean traverseMurAvecRayon(int x1, int y1, int x2, int y2, int rayon) {
        // Vérifier le segment central
        if (traverseMur(x1, y1, x2, y2)) {
            return true;
        }

        // Vérifier les points de départ et d'arrivée avec le rayon
        if (estDansMurAvecRayon(x1, y1, rayon) || estDansMurAvecRayon(x2, y2, rayon)) {
            return true;
        }

        // Calculer le vecteur perpendiculaire normalisé
        double dx = x2 - x1;
        double dy = y2 - y1;
        double longueur = Math.sqrt(dx * dx + dy * dy);

        if (longueur == 0) {
            return false;
        }

        // Vecteur perpendiculaire normalisé * rayon
        double perpX = (-dy / longueur) * rayon;
        double perpY = (dx / longueur) * rayon;

        // Vérifier les deux segments décalés (gauche et droite du chemin)
        if (traverseMur((int)(x1 + perpX), (int)(y1 + perpY), (int)(x2 + perpX), (int)(y2 + perpY))) {
            return true;
        }
        if (traverseMur((int)(x1 - perpX), (int)(y1 - perpY), (int)(x2 - perpX), (int)(y2 - perpY))) {
            return true;
        }

        return false;
    }
}
