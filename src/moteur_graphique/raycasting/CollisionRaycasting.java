package moteur_graphique.raycasting;

import moteur_graphique.CollisionStrategy;

public class CollisionRaycasting implements CollisionStrategy {
    private final MapBool map;

    public CollisionRaycasting(MapBool map){
        this.map = map;
    }

    @Override
    public boolean isColliding(double x, double y, double radius) {
        return map.isWall((int) x, (int) y);
    }
}
