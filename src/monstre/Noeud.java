package monstre;

import java.util.ArrayList;
import java.util.List;

public class Noeud {
    private int x;
    private int y;
    private Noeud parent;  // Pour RRT*
    private double cout;   // Coût depuis le départ

    private List<Noeud> voisins;


    public Noeud(int x, int y) {
        this.x = x;
        this.y = y;
        this.voisins = new ArrayList<Noeud>();
        this.parent = null;
        this.cout = Double.MAX_VALUE;
    }

    public int[] getCoordonnees() {
        return new int[] {x, y};
    }

    public int getX() {
        return x;
    }

    public int getY() {
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
