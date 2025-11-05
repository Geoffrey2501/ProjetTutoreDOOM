# Diagramme CU04 - Se Déplacer dans le Labyrinthe

---

## Scénario nominal

```
# Diagramme de séquence CU04 - Déplacement (Scénario nominal)

┌─────────┐   ┌──────────────┐   ┌──────────────┐   ┌─────────┐   ┌──────────────┐   ┌──────────────┐
│ Joueur  │   │ InputManager │   │   Player     │   │ Physics │   │  GameLoop    │   │  Renderer    │
└────┬────┘   └──────┬───────┘   └──────┬───────┘   └────┬────┘   └──────┬───────┘   └──────┬───────┘
     │               │                   │                │                │                   │
     │ 1. Appuie     │                   │                │                │                   │
     │    sur 'Z'    │                   │                │                │                   │
     ├──────────────►│                   │                │                │                   │
     │               │                   │                │                │                   │
     │               │ 2. detectInput()  │                │                │                   │
     │               │   → MOVE_FORWARD  │                │                │                   │
     │               │                   │                │                │                   │
     │               │ 3. requestMove(FORWARD)            │                │                   │
     │               ├──────────────────►│                │                │                   │
     │               │                   │                │                │                   │
     │               │                   │ 4. getPosition()                │                   │
     │               │                   │   → (x, y)     │                │                   │
     │               │                   │                │                │                   │
     │               │                   │ 5. calculateNewPos(direction)   │                   │
     │               │                   │   → (x', y')   │                │                   │
     │               │                   │                │                │                   │
     │               │                   │ 6. checkCollision(x', y')       │                   │
     │               │                   ├───────────────►│                │                   │
     │               │                   │                │                │                   │
     │               │                   │                │ 7. raycast()   │                   │
     │               │                   │                │    → isFree?   │                   │
     │               │                   │                │                │                   │
     │               │                   │ 8. result: true (pas de mur)    │                   │
     │               │                   ◄────────────────┤                │                   │
     │               │                   │                │                │                   │
     │               │                   │ 9. setPosition(x', y')          │                   │
     │               │                   │                │                │                   │
     │               │ 10. moveComplete()│                │                │                   │
     │               ◄───────────────────┤                │                │                   │
     │               │                   │                │                │                   │
     │               │                   │                │ 11. update()   │                   │
     │               │                   │                │   (boucle jeu) │                   │
     │               │                   │                ◄────────────────┤                   │
     │               │                   │                │                │                   │
     │               │                   │                │ 12. render()   │                   │
     │               │                   │                │                ├──────────────────►│
     │               │                   │                │                │                   │
     │               │                   │                │                │ 13. getPlayerPos()│
     │               │                   │                │                │   → (x', y')      │
     │               │                   ◄────────────────┼────────────────┤                   │
     │               │                   │                │                │                   │
     │               │                   │                │                │ 14. performRaycasting()
     │               │                   │                │                │    depuis (x',y') │
     │               │                   │                │                │                   │
     │               │                   │                │ 15. displayFrame()                 │
     │ 16. Affichage │                   │                │                ◄────────────────────
     │     nouvelle  │                   │                │                │                   │
     │     vue       │                   │                │                │                   │
     ◄──────────────────────────────────────────────────────────────────────────────────────────
     │               │                   │                │                │                   │
     │  ⏱️ < 50ms   │                   │                │                │                   │
     │               │                   │                │                │                   │
     ▼               ▼                   ▼                ▼                ▼                   ▼

```

---

## Scénario alternatif A1 - Collision avec mur

```
┌─────────┐                    ┌──────────────┐              ┌──────────────┐
│ Joueur  │                    │   Système    │              │   Rendu      │
└────┬────┘                    └──────┬───────┘              └──────┬───────┘
     │                                │                             │
     │ 1. Appuie sur 'Z'              │                             │
     ├───────────────────────────────►│                             │
     │                                │                             │
     │                                │ 2. Détecte input            │
     │                                │ 3. Calcule position (x',y') │
     │                                │ 4. Vérifie collision        │
     │                                │    Mur détecté ❌           │
     │                                │ 5. Position inchangée       │
     │                                │                             │
     │                                │ 6. Demande nouveau rendu    │
     │                                ├────────────────────────────►│
     │                                │                             │
     │                                │                             │ 7. Raycasting
     │                                │                             │    depuis (x,y)
     │                                │                             │
     │                                │ 8. Image calculée           │
     │                                ◄─────────────────────────────┤
     │                                │                             │
     │ 9. Vue inchangée affichée      │                             │
     ◄────────────────────────────────┤                             │
     │                                │                             │

```

---

## Scénario alternatif A2 - Mouvement diagonal

```
┌─────────┐                    ┌──────────────┐              ┌──────────────┐
│ Joueur  │                    │   Système    │              │   Rendu      │
└────┬────┘                    └──────┬───────┘              └──────┬───────┘
     │                                │                             │
     │ 1. Appuie sur 'Z' + 'D'        │                             │
     ├───────────────────────────────►│                             │
     │                                │                             │
     │                                │ 2. Détecte 2 inputs         │
     │                                │ 3. Calcule vecteur diag ↗   │
     │                                │ 4. Normalise vitesse        │
     │                                │ 5. Vérifie collision OK ✅   │
     │                                │ 6. Met à jour position      │
     │                                │                             │
     │                                │ 7. Demande nouveau rendu    │
     │                                ├────────────────────────────►│
     │                                │                             │
     │                                │                             │ 8. Raycasting
     │                                │                             │    depuis (x',y')
     │                                │                             │
     │                                │ 9. Image calculée           │
     │                                ◄─────────────────────────────┤
     │                                │                             │
     │ 10. Affichage déplacement diag │                             │
     ◄────────────────────────────────┤                             │

```

---

## Scénario alternatif A3 - Rotation caméra

```
┌─────────┐                    ┌──────────────┐              ┌──────────────┐
│ Joueur  │                    │   Système    │              │   Rendu      │
└────┬────┘                    └──────┬───────┘              └──────┬───────┘
     │                                │                             │
     │ 1. Bouge la souris →           │                             │
     ├───────────────────────────────►│                             │
     │                                │                             │
     │                                │ 2. Détecte deltaX souris    │
     │                                │ 3. Met à jour angle yaw     │
     │                                │    angle += deltaX          │
     │                                │ 4. Position inchangée       │
     │                                │                             │
     │                                │ 5. Demande nouveau rendu    │
     │                                ├────────────────────────────►│
     │                                │                             │
     │                                │                             │ 6. Raycasting
     │                                │                             │    depuis (x,y)
     │                                │                             │    avec nouvel angle
     │                                │ 7. Image calculée           │
     │                                ◄─────────────────────────────┤
     │                                │                             │
     │ 8. Affichage vue tournée       │                             │
     ◄────────────────────────────────┤                             │

```

```

---

## Système de contrôles

```
    Contrôles clavier
    
    ┌─────────────────────────────┐
    │    Z : Avancer              │
    │    S : Reculer              │
    │    Q : Gauche (strafe)      │
    │    D : Droite (strafe)      │
    │                             │
    │    ← → : Rotation caméra    │
    │    Souris : Rotation        │
    │                             │
    │    E : Interagir            │
    │    Espace : Action          │
    │    Échap : Pause            │
    └─────────────────────────────┘
    
    
    Système de collision
    
    ┌───────────────────────┐
    │   Position actuelle   │
    │       (x, y)          │
    └──────────┬────────────┘
               │
               │ Input détecté
               ▼
    ┌───────────────────────┐
    │ Calcul nouvelle pos   │
    │      (x', y')         │
    └──────────┬────────────┘
               │
               │ Vérification
               ▼
         ┌─────┴──────┐
         │            │
    Mur? │            │ Libre?
         │            │
         ▼            ▼
    ┌────────┐   ┌────────┐
    │ REFUSE │   │ ACCEPTE│
    └────────┘   └────┬───┘
                      │
                      ▼
              ┌────────────────┐
              │ Applique (x',y')│
              └────────────────┘
```

---

**Date** : 4 novembre 2025  
**CU** : CU04 - Se déplacer dans le labyrinthe

