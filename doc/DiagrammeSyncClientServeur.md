# Diagramme Synchronisation Multijoueur - Client-Serveur

---

## Architecture Client-Serveur

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│ Client 1 │         │ SERVEUR  │         │ Client 2 │
│          │         │          │         │          │
│ Joueur A │◄───────►│ AUTORITÉ │◄───────►│ Joueur B │
│          │  TCP    │ CENTRALE │  TCP    │          │
└──────────┘         └──────────┘         └──────────┘
                           │
                           │
                      État du jeu
                      • Positions
                      • HP
                      • Munitions
                      • Ennemis
                      • ...
```

---

## Flux de synchronisation des mouvements

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│ Client 1 │         │ Serveur  │         │ Client 2 │
└─────┬────┘         └─────┬────┘         └─────┬────┘
      │                    │                    │
      │ [Appuie sur Z]     │                    │
      │                    │                    │
      │ 1. Prédiction      │                    │
      │    locale          │                    │
      │    (x,y)→(x',y')   │                    │
      │                    │                    │
      │ 2. Envoi UPDATE    │                    │
      ├───────────────────►│                    │
      │ {id:1, pos:(x',y'),│                    │
      │  action:FORWARD}   │                    │
      │                    │                    │
      │                    │ 3. VALIDATION      │
      │                    │    • Collision?    │
      │                    │    • Vitesse OK?   │
      │                    │    • Anti-triche   │
      │                    │                    │
      │ 4. ACK             │                    │
      ◄────────────────────┤                    │
      │ {valid:true}       │                    │
      │                    │                    │
      │                    │ 5. BROADCAST       │
      │                    ├───────────────────►│
      │                    │ {id:1, pos:(x',y')}│
      │                    │                    │
      │                    │                    │ 6. MAJ affichage
      │                    │                    │    Position J1
      │                    │                    │
      ▼                    ▼                    ▼
      
```

---

## Flux de combat synchronisé

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│ Client 1 │         │ Serveur  │         │ Client 2 │
│(Tireur)  │         │          │         │ (Cible)  │
└─────┬────┘         └─────┬────┘         └─────┬────┘
      │                    │                    │
      │ [Clic souris]      │                    │
      │                    │                    │
      │ 1. Envoi SHOOT     │                    │
      ├───────────────────►│                    │
      │ {action:SHOOT,     │                    │
      │  target:2,         │                    │
      │  angle:45°}        │                    │
      │                    │                    │
      │                    │ 2. Raycast serveur │
      │                    │    (anti-triche)   │
      │                    │        └─► HIT     │
      │                    │                    │
      │                    │ 3. Calcul dégâts   │
      │                    │    Damage = 20     │
      │                    │    HP_J2: 100→80   │
      │                    │                    │
      │ 4. Confirm HIT     │ 5. Notify DAMAGE   │
      ◄────────────────────┤───────────────────►│
      │ {hit:true,         │ {from:1,           │
      │  damage_dealt:20}  │  damage:20,        │
      │                    │  hp:80}            │
      │                    │                    │
      │ 6. Anim tir        │                    │ 7. Anim dégât 
      │                    │                    │
      ▼                    ▼                    ▼
```

---

## Gestion de la latence

```
    Client avec prédiction
    
    ┌──────────────────────────────────┐
    │         CLIENT 1                 │
    │                                  │
    │  Input Z                         │
    │    │                             │
    │    ▼                             │
    │  ┌─────────────────┐             │
    │  │  PRÉDICTION     │             │
    │  │  Applique       │             │
    │  │  immédiatement  │             │
    │  │  mouvement local│             │
    │  └────────┬────────┘             │
    │           │                      │
    │           ▼                      │
    │  ┌─────────────────┐             │
    │  │ Envoie au       │             │
    │  │ serveur         │             │
    │  └────────┬────────┘             │
    │           │                      │
    └───────────┼──────────────────────┘
                │
                ▼
         ┌─────────────┐
         │   SERVEUR   │
         │   Valide    │
         └──────┬──────┘
                │
         ┌──────┴──────┐
         │             │
      Valid?       Invalid?
         │             │
         ▼             ▼
    ┌────────┐    ┌────────┐
    │   OK   │    │ROLLBACK│
    │        │    │ Client │
    └────────┘    └────────┘
        │              │
        ▼              ▼
                  Client corrige
                  sa position
```

---

## Heartbeat et détection de déconnexion

```
┌──────────┐                          ┌──────────┐
│ Client   │                          │ Serveur  │
└─────┬────┘                          └─────┬────┘
      │                                     │
      │ Heartbeat toutes les 3s             │
      │ ────────────────────────────────────►
      │                                     │
      │                                     │ Dernière réception
      │                                     │ enregistrée
      │                                     │
      │ ────────────────────────────────────►
      │                                     │
      │                                     │
      │ ────────────────────────────────────►
      │                                     │
      │                                     │
      │         ╳╳╳╳╳╳╳╳╳ (perte réseau)    │
      │                                     │
      │                                     │ Timeout 10s
      │                                     │ sans heartbeat
      │                                     │
      │                                     │   TIMEOUT
      │                                     │
      │                                     │ Déconnexion
      │                                     │ du joueur
      │                                     │
      ◄─────────────────────────────────────┤
         Tentative reconnexion              │
         ou retour menu                     │
      │                                     │
      ▼                                     ▼
```

---

**Date** : 4 novembre 2025  
**Type** : Synchronisation Client-Serveur

