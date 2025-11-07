# Diagramme CU05 - Combattre un Ennemi

---

## Scénario nominal (Local Solo)

```
┌─────────┐         ┌──────────────┐         ┌──────────────┐
│ Joueur  │         │   Système    │         │   Ennemi IA  │
│         │         │  (Moteur)    │         │              │
└────┬────┘         └──────┬───────┘         └──────┬───────┘
     │                     │                        │
     │ 1. Vise ennemi      │                        │
     │    (souris)         │                        │
     │                     │                        │
     │ 2. Clique (tir)     │                        │
     ├────────────────────►│                        │
     │                     │                        │
     │                     │ 3. Vérifie munitions   │
     │                     │    > 0                 │
     │                     │                        │
     │                     │ 4. Vérifie cooldown OK │
     │                     │                        │
     │                     │ 5. Vérifie reload?     │
     │                     │    (état arme)         │
     │                     │                        │
     │                     │ 6. Vérifie line of     │
     │                     │    sight               │
     │                     │                        │
     │                     │ 7. Raycast direction   │
     │                     │    visée               │
     │                     │         │              │
     │                     │         └─────────────►│
     │                     │                        │
     │                     ├──────────[ alt: HIT ]──┤
     │                     │                        │
     │                     │ 8. HIT détecté         │
     │                     │                        │
     │                     │ 9. Calcul dégâts       │
     │                     │    Damage = 20 HP      │
     │                     │                        │
     │                     │ 10. Munition -1        │
     │                     │                        │
     │                     │ 11. ApplyDamage(20)    │
     │                     ├───────────────────────►│
     │                     │                        │
     │                     │                        │ HP: 100→80
     │                     │                        │ Clamp[0,100]
     │                     │                        │
     │ 12. Animation tir   │                        │ 13. Animation hit
     │     + muzzle flash  │                        │     + son impact
     │     + son tir       │                        │    
     ◄─────────────────────┤                        │
     │                     │                        │
     │                     │                        │ 14. Contre-
     │                     │                        │     attaque
     │                     ├────────[ else: MISS ]──┤
     │                     │                        │
     │                     │ (Pas de message        │
     │                     │  vers Ennemi IA)       │
     │                     │                        │
     ▼                     ▼                        ▼

```

---

## Scénario alternatif A1 - Plus de munitions

```
┌─────────┐                    ┌──────────────┐
│ Joueur  │                    │   Système    │
│         │                    │  (Moteur)    │
└────┬────┘                    └──────┬───────┘
     │                                │
     │ 1. Clique (tir)                │
     ├───────────────────────────────►│
     │                                │
     │                                │ 2. Vérifie munitions
     │                                │    = 0 [ÉCHEC]
     │                                │
     │                                │ 3. PAS de raycast
     │                                │    PAS de décrément
     │                                │
     │ 4. Son "clic" vide             │
     │    (dry fire)                  │
     ◄────────────────────────────────┤
     │                                │
     │ 5. Affiche UI:                 │
     │    "Rechargez!"                │
     ◄────────────────────────────────┤
     │                                │
     ▼                                ▼
     
```

---

## Scénario alternatif A2 - Tir manqué (MISS)

```
┌─────────┐                    ┌──────────────┐
│ Joueur  │                    │   Système    │
│         │                    │  (Moteur)    │
└────┬────┘                    └──────┬───────┘
     │                                │
     │ 1. Clique (tir)                │
     ├───────────────────────────────►│
     │                                │
     │                                │ 2. Munitions > 0 [OK]
     │                                │
     │                                │ 3. Cooldown OK
     │                                │
     │                                │ 4. Reload OK
     │                                │
     │                                │ 5. Raycast direction
     │                                │    visée
     │                                │         │
     │                                │         └──► Mur/Décor
     │                                │
     │                                ├─[ alt: MISS ]────┤
     │                                │                  │
     │                                │ 6. Pas de HIT    │
     │                                │    sur Ennemi IA │
     │                                │                  │
     │                                │ 7. Munition -1   │
     │                                │                  │
     │                                │ (Aucun message   │
     │                                │  vers Ennemi IA) │
     │                                │                  │
     │ 8. Animation tir               │                  │
     ◄────────────────────────────────┤                  │
     │    (pas de dégât)              │                  │
     │                                ├──────────────────┤
     ▼                                ▼

```

---

## Scénario alternatif A6 - Combat P2P (multijoueur)

```
┌──────────┐                                            ┌──────────┐       ┌──────────┐
│ Joueur1  │                                            │ Joueur2  │       │ Joueur3  │
│(Tireur)  │                                            │ (Cible)  │       │(Témoin)  │
└────┬─────┘                                            └─────┬────┘       └────┬─────┘
     │                                                        │                 │
     │ 1. Vise Joueur2                                        │                 │
     │    (souris)                                            │                 │
     │                                                        │                 │
     │ 2. Clique (tir)                                        │                 │
     │                                                        │                 │
     │ 3. Validation locale                                   │                 │
     │ • Munitions > 0?                                       │                 │
     │ • Cooldown OK?                                         │                 │
     │ • Reload OK?                                           │                 │
     │                                                        │                 │
     │ 4. Raycast local                                       │                 │
     │    (line of sight)                                     │                 │
     │         │                                              │                 │
     │         └──────────[ alt: HIT ]────────────────────────┤                 │
     │                                                        │                 │
     │ 5. Munition -1 (local)                                 │                 │
     │                                                        │                 │
     │ 6. Animation tir locale                                │                 │
     │    + muzzle flash + son                                │                 │
     │                                                        │                 │
     │ 7. Envoi P2P: PLAYER_SHOT {                            │                 │
     │      shooter_id: J1,                                   │                 │
     │      target_id: J2,                                    │                 │
     │      damage: 20,                                       │                 │
     │      shot_id: 42,                                      │                 │
     │      position, direction,                              │                 │
     │      timestamp                                         │                 │
     │    }                                                   │                 │
     ├───────────────────────────────────────────────────────►│                 │
     │                                                        │                 │
     │                                                        │ 8. Validation   │
     │                                                        │    réception    │
     │                                                        │ • shot_id dédup │
     │                                                        │ • Timestamp OK  │
     │                                                        │                 │
     │                                                        │ 9. ApplyDamage  │
     │                                                        │    (20)         │
     │                                                        │                 │
     │                                                        │ HP: 100→80      │
     │                                                        │ Clamp[0,100]    │
     │                                                        │                 │
     │                                                        │ 10. Animation   │
     │                                                        │     hit + son   │
     │                                                        │                 │
     │ 11. ACK P2P: {                                         │                 │
     │       shot_id: 42,                                     │                 │
     │       hit: true,                                       │                 │
     │       hp: 80                                           │                 │
     │     }                                                  │                 │
     ◄────────────────────────────────────────────────────────┤                 │
     │                                                        │                 │
     │ 12. Confirmation hit                                   │                 │
     │     (feedback visuel)                                  │                 │
     │                                                        │                 │
     │                                                        │ 13. Broadcast   │
     │                                                        │     P2P état:   │
     │                                                        │     PLAYER_HIT {│
     │                                                        │       target_id │
     │                                                        │       hp: 80,   │
     │                                                        │       shot_id   │
     │                                                        │     }           │
     │                                                        ├────────────────►│
     │                                                        │                 │
     │                                                        │                 │ 14. Affiche
     │                                                        │                 │     animation
     │                                                        │                 │     hit J2
     │                                                        ├─────[ else: MISS ]────┤
     │                                                        │                 │
     │                                                        │ (Raycast local  │
     │                                                        │  pas de HIT)    │
     │                                                        │                 │
     │                                                        │ (Pas d'ACK      │
     │                                                        │  envoyé)        │
     │                                                        │                 │
     ▼                                                        ▼                 ▼

```

---

## Mort de l'ennemi

```
┌─────────┐         ┌──────────────┐         ┌──────────────┐
│ Joueur  │         │   Système    │         │   Ennemi IA  │
│         │         │  (Moteur)    │         │              │
└────┬────┘         └──────┬───────┘         └──────┬───────┘
     │                     │                        │
     │ 1. Tir final        │                        │
     ├────────────────────►│                        │
     │                     │                        │
     │                     │ 2. Raycast + HIT       │
     │                     │                        │
     │                     │ 3. ApplyDamage(20)     │
     │                     ├───────────────────────►│
     │                     │                        │
     │                     │                        │ HP: 20 → 0
     │                     │                        │ Clamp[0, max]
     │                     │                        │
     │                     │                        │ 4. If HP <= 0:
     │                     │                        │    Die()
     │                     │                        │
     │                     │                        │ 5. DisableCollider()
     │                     │                        │    (plus de hit)
     │                     │                        │
     │                     │                        │ 6. StartDeathAnim()
     │                     │                        │    (5 secondes)
     │                     │                        │
     │                     │ 7. Broadcast DEATH {   │
     │                     │      enemy_id,         │
     │                     │      killer_id         │
     │                     │    }                   │
     │                     ◄────────────────────────┤
     │                     │                        │
     │ 8. Animation mort   │                        │
     │    + son mort       │                        │
     ◄─────────────────────┤                        │
     │                     │                        │
     │ 9. +1 Frag (score)  │                        │
     │    Affiche kill feed│                        │
     ◄─────────────────────┤                        │
     │                     │                        │
     │                     │                        │ 10. Corps reste
     │                     │                        │     visible 5s
     │                     │                        │
     │                     │                        │ 11. Despawn(T+5s)
     │                     │                        │     Destroy()
     │                     │                        │
     ▼                     ▼                        X

```

---

## Système de combat (Flowchart)

```
    Cycle de combat
    
    ┌──────────────┐
    │   JOUEUR     │
    │   Clique     │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │ Munitions    │
    │    > 0?      │
    └──────┬───────┘
           │
       ┌───┴───┐
       │       │
      Oui     Non
       │       │
       │       ▼
       │   ┌─────────┐
       │   │ Son vide│
       │   │ (A1)    │
       │   └─────────┘
       │       │
       │      [FIN]
       │
       ▼
    ┌──────────────┐
    │  Cooldown    │
    │    OK?       │
    └──────┬───────┘
           │
       ┌───┴───┐
       │       │
      Oui     Non
       │       │
       │       ▼
       │    [Attente]
       │
       ▼
    ┌──────────────┐
    │   Reload?    │
    │ (en cours)   │
    └──────┬───────┘
           │
       ┌───┴───┐
       │       │
      Non     Oui
       │       │
       │       ▼
       │    [Bloqué]
       │
       ▼
    ┌──────────────┐
    │   Raycast    │
    │ + Line of    │
    │   Sight      │
    └──────┬───────┘
           │
      ┌────┴─────┐
      │          │
    HIT?       MISS?
      │          │
      ▼          ▼
  ┌───────┐  ┌──────────┐
  │Dégâts │  │ Anim tir │
  │  -20  │  │ (pas de  │
  │       │  │  dégâts) │
  └───┬───┘  └──────────┘
      │
      ▼
  ┌────────┐
  │HP > 0? │
  └───┬────┘
      │
   ┌──┴──┐
   │     │
  Oui   Non
   │     │
   ▼     ▼
┌──────┐┌────────────┐
│ Vit  ││    Mort    │
│      ││ Die()      │
│      ││ Despawn(5s)│
└──────┘└────────────┘

```

---

**Date** : 4 novembre 2025  
**CU** : CU05 - Combattre un ennemi

