# Diagramme de Cas d'Utilisation - Vue Globale

---

## Diagramme global du système

```
                    ┌───────────────────────────────────────┐
                    │     MOTEUR DOOM-LIKE (Système)        │
                    │                                       │
                    │                                       │
┌──────────┐        │    ┌──────────────────────────┐       │
│          │        │    │   GESTION DE PARTIE      │       │
│          │        │    │                          │       │
│  JOUEUR  │────────┼───►│  • Jouer en solo         │       │
│          │        │    │  • Créer partie P2P      │       │
│          │        │    │  • Rejoindre partie P2P  │       │
└────┬─────┘        │    │                          │       │
     │              │    └──────────────────────────┘       │
     │              │                                       │
     │              │    ┌──────────────────────────┐       │
     │              │    │      GAMEPLAY            │       │
     │              │    │                          │       │
     └──────────────┼───►│  • Se déplacer           │       │
     │              │    │  • Combattre             │       │
     │              │    │  • Interagir avec PNJ    │       │
     └──────────────┼───►│  • Chat (P2P)            │       │
                    │    │                          │       │
                    │    └──────────────────────────┘       │
                    │                                       │
                    │    ┌──────────────────────────┐       │
                    │    │    CONFIGURATION         │       │
                    │    │                          │       │
                    │    │  • Paramètres            │       │
                    │    │                          │       │
                    │    └──────────────────────────┘       │
                    │                                       │
                    └───────────────────────────────────────┘
                     ▲              ▲
                     │              │
       ┌─────────────┘              └───────────────┐
       │                                            │
┌──────┴──────┐                               ┌─────┴───────┐
│ Système IA  │                               │ Autres      │
│ (Monstres + │                               │ joueurs     │
│   PNJ)      │                               │   (P2P)     │
└─────────────┘                               └─────────────┘

                             
```

---

## Organisation par modules

### Module 1 : Gestion de Partie

```
                        ┌─────────────────────────────────┐
                        │   GESTION DE PARTIE             │
                        │                                 │
┌──────────┐            │  ┌───────────────────────┐      │
│          │            │  │ CU01: Jouer en solo   │      │
│  JOUEUR  │────────────┼─►│ (charger une carte,   │      │
│          │            │  │ lancer la partie)     │      │
└────┬─────┘            │  └───────────────────────┘      │
     │                  │                                 │
     │                  │  ┌───────────────────────┐      │
     │                  │  │ CU02: Créer partie    │      │
     └──────────────────┼─►│       P2P (hôte)      │      │
     │                  │  └───────────────────────┘      │
     │                  │                                 │
     │                  │  ┌───────────────────────┐      │
     │                  │  │ CU03: Rejoindre       │      │
     └──────────────────┼─►│       partie P2P      │      │
                        │  │       (client)        │      │
                        │  └───────────────────────┘      │
                        │                                 │
                        └─────────────────────────────────┘

```

---

### Module 2 : Gameplay

```
                        ┌─────────────────────────────────┐
                        │       GAMEPLAY                  │
                        │                                 │
┌──────────┐            │  ┌───────────────────────┐      │
│          │            │  │ CU04: Se déplacer     │      │
│  JOUEUR  │────────────┼─►│       dans le niveau  │      │
│          │            │  └───────────────────────┘      │
└────┬─────┘            │                                 │
     │                  │  ┌───────────────────────┐      │
     │                  │  │ CU05: Combattre       │      │
     └──────────────────┼─►│       un ennemi       │      │
     │                  │  └───────────────────────┘      │
     │                  │                                 │
     │                  │  ┌───────────────────────┐      │
     │                  │  │ CU06: Interagir       │      │
     └──────────────────┼─►│       avec un PNJ     │      │
     │                  │  └───────────────────────┘      │
     │                  │                                 │
     │                  │  ┌───────────────────────┐      │
     │                  │  │ CU07: Chat textuel    │      │
     └──────────────────┼─►│    (P2P, multijoueur) │      │
                        │  └───────────────────────┘      │
                        │                                 │
                        └─────────────────────────────────┘

```

---

### Module 3 : Systèmes internes

```
    ┌─────────────────────────────────┐
    │   MODULE 3 : SYSTÈMES INTERNES  │
    │                                 │
    │  ┌───────────────────────┐      │
    │  │ CU09: Gérer IA        │      │
    │  │  (monstres + PNJ)     │      │
    │  └───────────────────────┘      │
    │             │                   │
    │             │ utilise           │
    │             ▼                   │
    │  ┌───────────────────────┐      │
    │  │ CU10: Synchroniser    │      │
    │  │       état du jeu     │      │
    │  │       (P2P)           │      │
    │  └───────────────────────┘      │
    │                                 │
    └─────────────────────────────────┘

```

---

## Relations entre cas d'utilisation

### Dépendances principales

```
    CU01: Jouer en solo
         │
         │ Base pour la logique de jeu
         ▼
    CU04: Se déplacer
         │
         │ Nécessaire pour
         ▼
    CU05: Combattre
         │
         │
         ▼
    CU06: Interagir avec PNJ
         │
         │ Inclut
         ▼
    CU09: Gérer IA (monstres + PNJ)


    CU02: Créer partie P2P ──┐
         │                   │
         │                   ├──► CU10: Synchroniser
         ▼                   │        état du jeu (P2P)
    CU03: Rejoindre P2P ─────┘
         │
         │
         ▼
    CU07: Chat textuel (P2P)

```

---


**Date**    : 6 novembre 2025
**Version** : 1.1 (suppression du générateur de niveaux, passage complet en P2P)

