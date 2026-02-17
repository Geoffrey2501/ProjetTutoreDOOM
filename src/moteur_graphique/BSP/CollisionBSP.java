package moteur_graphique.BSP;

import moteur_graphique.CollisionStrategy;

import java.awt.geom.Line2D;

public class CollisionBSP implements CollisionStrategy {
    private final MapMur map;
    BSPParcours bspParcours;

    public CollisionBSP(MapMur map){
        this.map = map;
        this.bspParcours = new BSPParcours(null, map);
    }

    @Override
    public boolean isColliding(double x, double y, double radius) {
        //for (Mur mur : map.getMurs()) {
        for (Mur mur : this.bspParcours.getMursVisibles(x, y)) {
            double distance = Line2D.ptSegDist(mur.x0, mur.y0, mur.x1, mur.y1, x, y);

            if (distance < radius) {
                return true;
            }
        }
        return false;
    }
}
