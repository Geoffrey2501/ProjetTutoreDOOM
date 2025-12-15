package prototype_raycasting;

public class MainTest {
    public static void main(String[] args) throws InterruptedException {
        Map map = new Map("src/prototype_raycasting/map/map.txt");

        Joueur joueur = new Joueur(2.0, 2.0, 0.0);

        Raycasting raycasting = new Raycasting(map, joueur);

        // Ajouter quelques autres joueurs à différentes positions
        Joueur autreJoueur1 = new Joueur(5.0, 5.0, 0.0);
        Joueur autreJoueur2 = new Joueur(3.0, 8.0, 0.0);
        Joueur autreJoueur3 = new Joueur(7.0, 3.0, 0.0);
        
        raycasting.ajouterAutreJoueur(autreJoueur1);
        raycasting.ajouterAutreJoueur(autreJoueur2);
        raycasting.ajouterAutreJoueur(autreJoueur3);

        raycasting.render();
        
        // Faire tourner le joueur pour voir les autres joueurs
        for(int i = 0; i < 100; i++) {
            Thread.sleep(50);
            joueur.setAngle(joueur.getAngle() + 0.05);
            raycasting.render();
        }
    }
}
