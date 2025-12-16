package prototype_raycasting;

public class MainTest {
    public static void main(String[] args) throws InterruptedException {
        Map map = new Map("src/prototype_raycasting/map/map.txt");

        Joueur joueur = new Joueur(0.0, 1.0, 0.0);

        Raycasting raycasting = new Raycasting(map, joueur);

        raycasting.render();
        for(int i = 0; i < 100; i++) {
            Thread.sleep(100);
            joueur.setY(joueur.getY() + 0.01);
            raycasting.render();
        }
    }
}
