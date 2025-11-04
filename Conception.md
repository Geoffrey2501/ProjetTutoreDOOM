# Présentation du Projet - Moteur Doom-like en Java

---

## Qu'est-ce que ce projet ?

Un **prototype de moteur graphique 3D** développé en Java, inspiré des jeux classiques Wolfenstein 3D et Doom. Il s'agit d'un projet pédagogique qui explore les grandes problématiques derrière la réalisation d'un moteur graphique.

---

## Fonctionnalités principales

### 1. **Moteur de rendu 3D**
- Technique de **raycasting** pour simuler la 3D depuis un environnement 2D
- Optimisation avec **BSP** (Binary Space Partitioning)
- Textures sur les murs, sols et plafonds
- Gestion des FPS

### 2. **Génération procédurale de labyrinthes**
- Création de labyrinthe parfait
- Niveaux générés automatiquement et aléatoirement
- Paramétrage de la taille et de la complexité

### 3. **Intelligence artificielle**
- **Ennemis intelligents** avec comportements variés :
    - Patrouille, détection, poursuite, attaque, fuite
    - Pathfinding
- **PNJ conversationnels** utilisant des modèles de langage
    - Dialogues dynamiques et contextuels
    - Génération de quêtes par IA

### 4. **Multijoueur (2 modes)**
- **Client-Serveur** : serveur central gérant l'état du jeu
- **Peer-to-Peer (P2P)** : connexion directe entre joueurs
- Synchronisation en temps réel des positions et actions par la gestion d'états

### 5. **Interface utilisateur**
- Menus (principal, pause, options)
- HUD avec informations vitales (vie, munitions, mini-carte)
- Configuration personnalisable

---

## Technologies utilisées

| Composant | Technologie |
|-----------|-------------|
| **Langage** | Java |
| **Interface graphique** | Swing |
| **Réseau** | Java Sockets |
| **IA** | Algorithmes de graphes (BFS, Dijkstra) |
| **Génération procédurale** | Prim & Kruskal |
| **Architecture** | MVC + Threads |
