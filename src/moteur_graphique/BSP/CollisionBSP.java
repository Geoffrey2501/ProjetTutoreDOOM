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
        //for (Mur mur : this.bspParcours.getMursVisiblesAngle(x, y)) {
            //apres plusieurs tests, getMursVisibles est bien plus lourd et long...
            //meme sur des milliers de murs, la difference est enorme, et getMursVisibles ne fait pas du tout gagner du temps

            double distance = Line2D.ptSegDist(mur.x0, mur.y0, mur.x1, mur.y1, x, y);

            if (distance < radius) {
                return true;
            }
        }
        return false;
    }
}
