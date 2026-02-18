# ProjetTutoreDOOM

## 0. Informations Générales
- **Nom du projet** : ProjetTutoreDOOM
- **Auteurs** : Cyprian, Ilyès, Marcelin, Geoffrey

## 1. Installation
Le projet est une application Java standard.

### Prérequis
- **Java JDK** (version 25).
- Un IDE (IntelliJ IDEA, Eclipse, VS Code) ou un terminal.

### Procédure
1. Récupérez le dossier du projet (via github).
```bash
git clone https://github.com/Geoffrey2501/ProjetTutoreDOOM.git
```

## 2. Bibliothèques Requises
Aucune bibliothèque externe n'est nécessaire.
Le projet utilise uniquement les bibliothèques standard de Java.

## 3. Lancement de l'Application

### Via le Terminal (Ligne de commande)
Placez-vous à la racine du projet, puis compilez et exécutez :

**Compilation :**
*(Créez un dossier `bin` à la racine du projet s'il n'existe pas)*
```bash
mkdir bin
javac -d bin -sourcepath src src/game/MainGameMultiplayer.java
```

**Exécution :**
```bash
java -cp bin game.MainGameMultiplayer
```

---

## 4. Séquence de Lancement (Guide Pratique)

Lors du lancement, l'application vous guidera dans la console. Voici la séquence type pour démarrer une partie :

1. **Identification** : Entrez votre pseudo.
2. **Configuration Réseau** : Entrez un port local (ex: `5001`).
3. **Mode de Connexion** : On vous demande si vous voulez rejoindre un joueur existant.
   - **Pour être Hôte (Premier joueur)** : Répondez `n` (non).
   - **Pour Rejoindre (Deuxième joueur)** : Répondez `o` (oui), puis entrez l'adresse IP et le port de l'hôte.

### Exemple de session dans la console :

```text
=== DOOM-LIKE MULTIJOUEUR P2P ===

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

## 5. Détails Techniques

Ce projet implémente plusieurs concepts avancés "from scratch" :

- **Moteur Graphique (Raycasting)** : Rendu 3D simulé à partir d'une carte 2D, similaire à Wolfenstein 3D ou DOOM.
- **Optimisation Spatial (BSP - Binary Space Partitioning)** : Utilisation d'un arbre BSP pour gérer l'affichage et les collisions des murs de manière performante.
- **Réseau P2P (Peer-to-Peer)** : Architecture décentralisée (maillage complet) où chaque joueur envoie sa position et ses actions directement aux autres pairs, sans serveur central dédié.
- **Intelligence Artificielle (RRT)** : Algorithme *Rapidly-exploring Random Tree* pour la navigation des monstres (démos disponibles dans `src/monstre/MainRRT.java`).
