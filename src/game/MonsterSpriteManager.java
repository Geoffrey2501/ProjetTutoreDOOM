package game;

import entite.Sprite;
import moteur_graphique.GameRenderer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les sprites des monstres dans le jeu multijoueur.
 */
public class MonsterSpriteManager {

    private final Map<String, Sprite> monsterSprites;
    private final GameRenderer renderer;

    public MonsterSpriteManager(GameRenderer renderer) {
        this.monsterSprites = new ConcurrentHashMap<>();
        this.renderer = renderer;
    }

    /**
     * Met à jour la position d'un monstre.
     */
    public void onMonsterMove(String monsterId, double x, double y) {
        synchronized (monsterSprites) {
            Sprite sprite = monsterSprites.get(monsterId);
            if (sprite == null) {
                // Création du sprite de monstre (en supposant qu'il utilise jonesy.png comme texture temporaire ou autre chose)
                sprite = new Sprite(x, y, "assets/sprites/jonesy.png", "Monstre " + monsterId);
                monsterSprites.put(monsterId, sprite);
                renderer.addSprite(sprite);
            } else {
                sprite.setX(x);
                sprite.setY(y);
            }
        }
    }

    /**
     * Supprime tous les montres.
     */
    public void clear() {
        synchronized (monsterSprites) {
            for (Sprite sprite : monsterSprites.values()) {
                renderer.removeSprite(sprite);
            }
            monsterSprites.clear();
        }
    }
}
