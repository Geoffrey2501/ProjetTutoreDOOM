package moteur_graphique.BSP;

public class Mur {
    public int x0, y0, x1, y1;
    public String texture;

    public Mur(int x0, int y0, int x1, int y1, String texture) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.texture = texture;
    }

    public Mur(int x0, int y0, int x1, int y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.texture = "";
    }

    public boolean estDansMur(int x, int y) {
        return x>= Math.min(x0, x1) && x <= Math.max(x0, x1) &&
                y >= Math.min(y0, y1) && y <= Math.max(y0, y1);
    }
}
