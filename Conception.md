# Présentation & Vision du Projet - Moteur Doom-like en Java

---

## Énoncé de vision

Créer un prototype de moteur “Doom-like” simple, performant et pédagogique en Java/Swing, qui démontre le rendu pseudo-3D (raycasting + BSP), la génération procédurale, une IA d’ennemis crédible et un multijoueur (client-serveur & P2P). C'est un sorte de laboratoire d’apprentissage clair et mesurable.

---

## Qu'est-ce que ce projet ?

Un **prototype de moteur graphique 3D** développé en Java, inspiré des jeux classiques Wolfenstein 3D et Doom. Il s'agit d'un projet pédagogique qui explore les grandes problématiques derrière la réalisation d'un moteur graphique.

---

## Pour qui (clients) ?

Équipe projet & tuteur : valider des acquis via un démonstrateur technique.

Étudiants/développeurs : base lisible pour expérimenter rendu/IA/réseau.

Communauté pédagogique : exemple minimal reproductible et commenté.

---

## Besoins clés & Proposition de valeur

Comprendre/manipuler un moteur de rendu : raycasting + BSP (optimise ce qui est visible/chargé) + textures.

Générer vite des niveaux : labyrinthes parfaits via Prim et Kruskal.

Tester une IA crédible : parcours de graphe (BFS, Dijkstra) + états (patrouille, poursuite, fuite).

Valider le réseau : deux modes jouables (client-serveur & P2P) via sockets Java.

Rester accessible : Java (threads/sockets) + Swing pour le rendu et les entrées.

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
## Principes directeurs

Pédagogie d’abord : privilégier la clarté aux micro-optimisations.

Architecture propre : MVC + Threads, modules séparés, code commenté.

Mesurabilité : chaque brique avec critères de succès observables.

Itératif : rendu → génération → IA → réseau, en incréments testables.

Portabilité : Java/Swing pour réduire les frictions d’installation.

---
## Indicateurs de succès (S.M.A.R.T.)

Rendu : scène “corridor + salle” à ≥30 FPS sur machine standard.

Génération : labyrinthe parfait < 1 s pour une taille cible définie.

IA : enchaînement d’états reproductible (patrouille → poursuite → perte de cible).

Réseau : 2–4 joueurs connectés, positions/actions synchronisées sans désync visible.

Dev : build démarrable + README clair (install/test), démo vidéo courte.

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
