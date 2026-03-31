package moteur_graphique.BSP;

import moteur_graphique.CollisionStrategy;

import java.awt.geom.Line2D;

public class CollisionBSP implements CollisionStrategy {
    private final MapMur map;

    public CollisionBSP(MapMur map){
        this.map = map;
    }

    @Override
    public boolean isColliding(double x, double y, double radius) {
        for (Mur mur : map.getMurs()) {
            double distance = Line2D.ptSegDist(mur.x0, mur.y0, mur.x1, mur.y1, x, y);

            if (distance < radius) {
                return true;
            }
        }
        return false;
    }
}
