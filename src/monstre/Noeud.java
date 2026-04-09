package monstre;

import java.util.ArrayList;
import java.util.List;

public class Noeud {
    private double x;
    private double y;
    private Noeud parent;  // Pour RRT*
    private double cout;   // Coût depuis le départ

    private List<Noeud> voisins;


    public Noeud(double x, double y) {
        this.x = x;
        this.y = y;
        this.voisins = new ArrayList<Noeud>();
        this.parent = null;
        this.cout = Double.MAX_VALUE;
    }

    public double[] getCoordonnees() {
        return new double[] {x, y};
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Noeud getParent() {
        return parent;
    }

    public void setParent(Noeud parent) {
        this.parent = parent;
    }

    public double getCout() {
        return cout;
    }

    public void setCout(double cout) {
        this.cout = cout;
    }

    public List<Noeud> getVoisins() {
        return voisins;
    }

    public void ajouterVoisin(Noeud voisin) {
        voisins.add(voisin);
    }

    public void resetVoisins() {
        voisins.clear();
    }
}
