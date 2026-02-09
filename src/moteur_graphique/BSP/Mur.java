package moteur_graphique.BSP;

public class Mur {
    public double x0, y0, x1, y1;
    public String texture;

    public Mur(double x0, double y0, double x1, double y1, String texture) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.texture = texture;
    }

    public Mur(double x0, double y0, double x1, double y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.texture = "";
    }

    public boolean estDansMur(double x, double y) {
        return x>= Math.min(x0, x1) && x <= Math.max(x0, x1) &&
                y >= Math.min(y0, y1) && y <= Math.max(y0, y1);
    }

    @Override
    public String toString() {
        return "Mur (" + x0 + "," + y0 + " -> " + x1 + "," + y1 + ") [" + texture + "]";
    }
}
