import prototype_raycasting.Map;

public class Lancer {
    public static void main(String[] args) {
        Map m = new Map("src/prototype_raycasting/map/map.txt");
        System.out.println("W=" + m.getWIDTH() + " H=" + m.getHEIGHT());
        for (int y = 0; y < m.getHEIGHT(); y++) {
            for (int x = 0; x < m.getWIDTH(); x++) {
                System.out.print(m.isWall(x, y) ? "#" : ".");
            }
            System.out.println();
        }
    }
}