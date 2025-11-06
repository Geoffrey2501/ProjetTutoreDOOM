# Diagramme CU01 - Jouer en Solo

---

## Scénario nominal
```
┌─────────┐                                    ┌──────────────┐
│ Joueur  │                                    │   Système    │
└────┬────┘                                    └──────┬───────┘
     │                                                │
     │ nouvellePartie()                               │
     ├───────────────────────────────────────────────►│
     │                                                │
     │                                                ├──┐ afficherParametres() 
     │                                                │◄─┘
     │      demandeConfirmation()                     │
     │◄───────────────────────────────────────────────┤
     │                                                │
     │ true                                           │
     ├─ ─── ── ── ─── ─── ─── ─── ─── ─── ─── ─── ───►│
     │                                                │
     │                                                ├──┐ getLabyrinthe()
     │                                                │  │ 
     │                                                │  │
     │                                                │◄─┘
     │                                                │
     │      afficherJeu(carte, position)              │
     │───────────────────────────────────────────────►│
     │                                                │
     │◄─── ─── ── ─── ─── ─── ─── ─── ─── ─── ─── ────┤
     │                                                │
     │                                                │
     ▼                                                ▼
```
Dans ce scénario nominal, le joueur démarre une nouvelle partie en solo. Le système affiche les paramètres de la partie, récupère un labyrinthe et affiche le jeu avec la carte et la position initiale du joueur.

---

## Scénario alternatif A1 - Annulation

```
┌─────────┐                    ┌──────────────┐
│ Joueur  │                    │   Système    │
└────┬────┘                    └──────┬───────┘
     │                                │
     │                                ├──┐ afficherParametres()
     │                                │◄─┘
     │      demandeConfirmation()     │
     │◄───────────────────────────────┤
     │ true                           │
     ├─ ─── ── ── ─── ─── ─── ─── ───►│
     │                                │
     │                                ├──┐ annulerCreation()
     │                                │◄─┘
     │                                │
     │                                ├──┐ afficherMenuPrincipal()
     │                                │◄─┘
     │      menuPrincipal()           │
     │◄───────────────────────────────┤
     │                                │
     ▼                                ▼
     
     [FIN]
```
Ce scénario alternatif décrit le cas où le joueur décide d'annuler la création de la partie après avoir vu les paramètres. Le système annule la création et retourne au menu principal.

---

## Scénario alternatif A3 - Pause

```
┌─────────┐                    ┌──────────────┐
│ Joueur  │                    │   Système    │
└────┬────┘                    └──────┬───────┘
     │                                │
     │ [En jeu]                       │
     │                                │
     │ appuyerEchap()                 │
     ├───────────────────────────────►│
     │                                │
     │                                ├──┐ pauserJeu()
     │                                │  │
     │                                │◄─┘
     │                                │
     │                                ├──┐ afficherMenuPause()
     │                                │  │ - Reprendre
     │                                │  │ - Options
     │                                │  │ - Quitter
     │                                │◄─┘
     │                                │
     │ selectionnerOption(option)     │
     ├───────────────────────────────►│
     │                                │
     │                                ├──┐ traiterOption()
     │                                │◄─┘
     │◄── ── ── ── ── ── ── ── ── ── ─┤
     │                                │
     ▼                                ▼
     
     Si "Reprendre" → reprendrJeu()
     Si "Options" → afficherMenuConfig()
     Si "Quitter" → afficherMenuPrincipal()
```
Ce scénario alternatif décrit le cas où le joueur met le jeu en pause en appuyant sur la touche Échap. Le système affiche le menu de pause avec plusieurs options que le joueur peut sélectionner.

---