# Diagramme CU02 - Créer une Partie Multijoueur P2P

---

## Scénario nominal

```
┌─────────┐                          ┌──────────────┐
│ Joueur  │                          │   Système    │
│ (Host)  │                          │              │
└────┬────┘                          └──────┬───────┘
     │                                      │
     │ creerPartie()                        │
     ├─────────────────────────────────────►│
     │                                      │
     │                                      ├──┐ 
     │                                      │  │ • P2P
     │                                      │◄─┘
     │                                      │
     │ selectionnerMode(mode)               │
     ├─────────────────────────────────────►│
     │                                      │
     │                                      ├──┐ afficherParametres()
     │                                      │  │ (nom, max joueurs,
     │                                      │  │  taille carte)
     │                                      │◄─┘
     │      demandeConfig()                 │
     │◄─────────────────────────────────────┤
     │                                      │
     │ valider(config)                      │
     ├─────────────────────────────────────►│
     │                                      │
     │                                      ├──┐ ouvrirSocket()
     │                                      │  │ (port 25565)
     │                                      │◄─┘
     │                                      │
     │                                      ├──┐ 
     │                                      │  │ getLabyrinthe()
     │                                      │◄─┘
     │                                      │
     │      afficherAttente()               │
     │      IP: 192.168.1.10                │
     │◄─────────────────────────────────────┤
     │                                      │
     │                                      ├──┐ attendreConnexions()
     │                                      │◄─┘
     │                                      │
     ▼                                      ▼
```
Dans ce scénario nominal, le joueur (host) crée une partie multijoueur en P2P, configure les paramètres de la partie, et le système récupère un labyrinthe (déjà près fait ou peut etre on affichera une liste des maps?) puis ouvre un socket pour attendre les connexions des autres joueurs.

---

## Scénario alternatif A1 - Port occupé

```
┌─────────┐                    ┌──────────────┐
│ Joueur  │                    │   Système    │
└────┬────┘                    └──────┬───────┘
     │                                │
     │ valider(config)                │
     ├───────────────────────────────►│
     │                                │
     │                                ├──┐ ouvrirSocket(25565)
     │                                │  │  ÉCHEC (occupé)
     │                                │◄─┘
     │                                │
     │      afficherErreur()          │
     │      "Port occupé"             │
     │◄────────────────────────────────┤
     │                                │
     │                                ├──┐ proposerAutrePort()
     │                                │◄─┘
     │      demandePort()             │
     │◄───────────────────────────────┤
     │                                │
     │ saisirPort(25566)              │
     ├───────────────────────────────►│
     │                                │
     │                                ├──┐ ouvrirSocket(25566)
     │                                │  │ 
     │                                │◄─┘
     │                                │
     │      socketOuvert()            │
     │◄───────────────────────────────┤
     │                                │
     ▼                                ▼
```
Ce scénario alternatif gère le cas où le port par défaut (25565) est déjà utilisé. Le système propose alors au joueur de saisir un autre port et réessaie d'ouvrir le socket.

---

## Scénario alternatif A2 - Aucun joueur ne rejoint

```
┌─────────┐                    ┌──────────────┐
│ Joueur  │                    │   Système    │
└────┬────┘                    └──────┬───────┘
     │                                │
     │ [En attente...]                │
     │                                │
     │                                ├──┐ attendreConnexions()
     │                                │  │ Timeout 5 min
     │                                │◄─┘
     │                                │
     │      afficherTimeout()         │
     │      "Aucun joueur connecté"   │
     │      "Continuer à attendre ?"  │
     │◄───────────────────────────────┤
     │                                │
     │ choisirAction(choix)           │
     ├───────────────────────────────►│
     │                                │
     │                                ├──┐ traiterChoix()
     │                                │◄─┘
     │                                │
     │                                ├──┐ fermerSocket()
     │                                │  │ libererRessources()
     │                                │◄─┘
     │                                │
     │      retourMenuMulti()         │
     │◄───────────────────────────────┤
     │                                │
     ▼                                ▼
     
     Si "Oui" → Prolonger 5 min
     Si "Non" → Retour menu
```
Ce scénario alternatif traite le timeout lorsqu'aucun joueur ne se connecte après 5 minutes d'attente. Le système demande au joueur s'il souhaite continuer à attendre ou annuler la partie.

