# ProjetTutoreDOOM

## Informations Générales
- **Nom du projet** : ProjetTutoreDOOM
- **Auteurs** : Cyprian, Ilyès, Marcelin, Geoffrey

## Installation

Le projet est une application Java standard.

### Prérequis
- **Java JDK 25** ou supérieur.
- **GNU Make** (optionnel, pour utiliser le Makefile).
- Un IDE (IntelliJ IDEA, Eclipse, VS Code) ou un terminal.

### Procédure
1. Clonez le dépôt :
```bash
git clone https://github.com/Geoffrey2501/ProjetTutoreDOOM.git
cd ProjetTutoreDOOM
```

## Bibliothèques Requises

Le projet utilise uniquement les bibliothèques standard de Java (AWT, Swing, `java.net`, etc.).
Les tests unitaires nécessitent **JUnit 5** (fourni automatiquement par IntelliJ IDEA).

## Structure du Projet

```
ProjetTutoreDOOM/
├── src/
│   ├── game/                  # Boucle de jeu, multijoueur, entrées clavier/souris
│   ├── moteur_graphique/      # Fenêtre, rendu, stratégies de collision
│   │   ├── BSP/               # Arbre BSP, murs, rendu BSP
│   │   └── raycasting/        # Raycasting, carte booléenne, collisions
│   ├── entite/                # Joueur, sprites
│   ├── Reseau/                # Serveur P2P, client, gestion des connexions
│   └── monstre/               # Monstres, algorithme RRT, steering
├── test/
│   ├── entite/                # Tests sur la carte et les murs
│   └── Reseau/                # Tests sur les connexions réseau
├── assets/
│   └── maps/                  # Fichiers de carte (map.txt, mapBSP.txt)
├── Documentation/             # Documents d'itération, diagrammes PlantUML
├── Makefile                   # Compilation et exécution via make
└── README.md
```

## Lancement de l'Application

### Via le Makefile (recommandé)

| Commande | Description |
|---|---|
| `make` | Compile toutes les sources dans `bin/` |
| `make run` | Compile et lance le jeu multijoueur |
| `make run-rrt` | Compile et lance la démo RRT (navigation monstres) |
| `make run-rrt-prog` | Compile et lance la démo RRT progressive |
| `make clean` | Supprime le dossier `bin/` (fichiers compilés) |
| `make help` | Affiche la liste des commandes disponibles |

Exemple rapide :
```bash
make run    # Compile puis lance le jeu
```

### Via le Terminal (manuellement)

Placez-vous à la racine du projet, puis compilez et exécutez :

**Compilation :**
```bash
mkdir -p bin
javac -d bin -sourcepath src src/game/MainGameMultiplayer.java
```

**Exécution :**
```bash
java -cp bin game.MainGameMultiplayer
```

### Démo RRT (navigation des monstres)

```bash
make run-rrt
```

Ou manuellement :
```bash
javac -d bin -sourcepath src src/monstre/MainRRT.java
java -cp bin monstre.MainRRT
```

---

## Séquence de Lancement (Guide Pratique)

Lors du lancement, l'application vous guidera dans la console. Voici la séquence type pour démarrer une partie :

1. **Mode de rendu** : Choisissez entre **BSP** (Binary Space Partitioning) ou **Raycasting**.
2. **Identification** : Entrez votre pseudo.
3. **Configuration Réseau** : Entrez un port local (ex: `5001`).
4. **Mode de Connexion** : On vous demande si vous voulez rejoindre un joueur existant.
   - **Pour être Hôte (Premier joueur)** : Répondez `n` (non).
   - **Pour Rejoindre (Deuxième joueur)** : Répondez `o` (oui), puis entrez l'adresse IP et le port de l'hôte.

### Exemple de session dans la console :

```text
=== DOOM-LIKE MULTIJOUEUR P2P ===

Mode de rendu:
  1. BSP (Binary Space Partitioning)
  2. Raycasting
Votre choix (1/2): 1
Mode sélectionné: BSP

Votre IP locale: 192.168.1.15
(utilisez cette adresse pour que d'autres se connectent à vous)

Votre nom de joueur: Slayer
Votre port (ex: 5001): 5001

=== Mode Peer-to-Peer (Maillage complet) ===
Vous pouvez vous connecter à un ou plusieurs joueurs.
Le réseau se synchronisera automatiquement (tous connectés à tous).

Voulez-vous rejoindre un joueur existant? (o/n): n

En attente de connexions sur le port 5001
Les autres joueurs peuvent se connecter à votre IP:port
```

*(Une fois lancé, une fenêtre graphique s'ouvrira avec le jeu.)*

---

## Tests

Les tests unitaires utilisent **JUnit 5** et se trouvent dans le dossier `test/`.

Pour les exécuter, utilisez votre IDE (IntelliJ IDEA, Eclipse) qui intègre nativement JUnit 5 :
clic droit sur le dossier `test/` > *Run Tests*.

---

## Détails Techniques

Ce projet implémente plusieurs concepts avancés "from scratch" :

- **Moteur Graphique (Raycasting)** : Rendu 3D simulé à partir d'une carte 2D, similaire à Wolfenstein 3D ou DOOM.
- **Optimisation Spatiale (BSP - Binary Space Partitioning)** : Utilisation d'un arbre BSP pour gérer l'affichage et les collisions des murs de manière performante.
- **Réseau P2P (Peer-to-Peer)** : Architecture décentralisée (maillage complet) où chaque joueur envoie sa position et ses actions directement aux autres pairs, sans serveur central dédié.
- **Intelligence Artificielle (RRT)** : Algorithme *Rapidly-exploring Random Tree* pour la navigation des monstres (démos disponibles via `make run-rrt` ou `src/monstre/MainRRT.java`).
- **Cartes** : Fichiers de niveau au format texte dans `assets/maps/`.
