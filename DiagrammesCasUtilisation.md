# Diagrammes de Cas d'Utilisation (CU) - Scénarios et Conditions de Validation

---

## Table des matières

1. [Vue d'ensemble du système](#1-vue-densemble-du-système)
2. [Acteurs du système](#2-acteurs-du-système)
3. [Diagramme de cas d'utilisation global](#3-diagramme-de-cas-dutilisation-global)
4. [Cas d'utilisation détaillés](#4-cas-dutilisation-détaillés)
   - 4.1 [CU01 - Jouer en solo](#cu01---jouer-en-solo)
   - 4.2 [CU02 - Créer une partie multijoueur](#cu02---créer-une-partie-multijoueur)
   - 4.3 [CU03 - Rejoindre une partie multijoueur](#cu03---rejoindre-une-partie-multijoueur)
   - 4.4 [CU04 - Se déplacer dans le labyrinthe](#cu04---se-déplacer-dans-le-labyrinthe)
   - 4.5 [CU05 - Combattre un ennemi](#cu05---combattre-un-ennemi)
   - 4.6 [CU06 - Interagir avec un PNJ](#cu06---interagir-avec-un-pnj)
   - 4.7 [CU07 - Communiquer via chat](#cu07---communiquer-via-chat)
   - 4.8 [CU08 - Configurer les paramètres](#cu08---configurer-les-paramètres)
   - 4.9 [CU09 - Gérer les ennemis (IA)](#cu09---gérer-les-ennemis-ia)
   - 4.10 [CU10 - Synchroniser l'état du jeu](#cu10---synchroniser-létat-du-jeu)
5. [Matrice de traçabilité](#5-matrice-de-traçabilité)

---

## 1. Vue d'ensemble du système

Le système est un **moteur de jeu Doom-like** développé en Java, permettant :
- Le jeu en mode solo avec ennemis IA
- Le jeu en mode multijoueur (Client-Serveur ou P2P)
- La génération procédurale de labyrinthes
- L'interaction avec des PNJ intelligents
- La configuration personnalisée du jeu

---

## 2. Acteurs du système

| Acteur | Type | Description |
|--------|------|-------------|
| **Joueur** | Principal | Utilisateur humain qui joue au jeu |
| **Serveur** | Secondaire | Système gérant les parties en mode Client-Serveur |
| **Système IA** | Secondaire | Moteur d'intelligence artificielle pour ennemis et PNJ |
| **Générateur de niveaux** | Secondaire | Algorithme de génération procédurale (Prim/Kruskal) |
| **Système réseau** | Secondaire | Gestionnaire des communications P2P ou Client-Serveur |

---

## 3. Diagramme de cas d'utilisation global

```
                        ┌─────────────────────────────────────────────┐
                        │         Moteur Doom-like (Système)          │
                        │                                             │
                        │  ┌────────────────────────────────────┐     │
                        │  │  Gestion de partie                 │     │
┌─────────┐             │  │  ┌──────────────────────────┐      │     │
│         │             │  │  │ CU01: Jouer en solo      │      │     │
│ Joueur  │─────────────┼──┼──│ CU02: Créer partie multi │      │     │
│         │             │  │  │ CU03: Rejoindre partie   │      │     │
└─────────┘             │  │  └──────────────────────────┘      │     │
     │                  │  └────────────────────────────────────┘     │
     │                  │                                             │
     │                  │  ┌────────────────────────────────────┐     │
     │                  │  │  Gameplay                          │     │
     │                  │  │  ┌──────────────────────────┐      │     │
     └──────────────────┼──┼──│ CU04: Se déplacer        │      │     │
     │                  │  │  │ CU05: Combattre ennemi   │      │     │
     │                  │  │  │ CU06: Interagir avec PNJ │      │     │
     └──────────────────┼──┼──│ CU07: Chat multijoueur   │      │     │
                        │  │  └──────────────────────────┘      │     │
                        │  └────────────────────────────────────┘     │
                        │                                             │
                        │  ┌────────────────────────────────────┐     │
                        │  │  Configuration                     │     │
                        │  │  ┌──────────────────────────┐      │     │
                        │  │  │ CU08: Paramètres         │◄─────┼─────│
                        │  │  └──────────────────────────┘      │     │
                        │  └────────────────────────────────────┘     │
                        │                    △                        │
                        │                    │                        │
                        └────────────────────┼────────────────────────┘
                                             │
                        ┌────────────────────┴────────────────────┐
                        │                                          │
                   ┌────┴─────┐                            ┌──────┴──────┐
                   │ Système  │                            │  Générateur │
                   │    IA    │                            │  de niveaux │
                   │          │                            │             │
                   └──────────┘                            └─────────────┘
                        │                                          │
                        │                                          │
             ┌──────────┴──────────┐                              │
             │ CU09: Gérer ennemis │                              │
             └─────────────────────┘                              │
                                                                   │
                                              ┌────────────────────┴──────┐
                                              │ Génération labyrinthe     │
                                              │ (inclus dans CU01/02/03)  │
                                              └───────────────────────────┘

                   ┌─────────────┐
                   │   Serveur   │
                   │   (réseau)  │
                   └──────┬──────┘
                          │
                ┌─────────┴─────────┐
                │ CU10: Synchroniser│
                │   état du jeu     │
                └───────────────────┘
```

---

## 4. Cas d'utilisation détaillés

---

## CU01 - Jouer en solo

### Identification
- **ID** : CU01
- **Nom** : Jouer en solo
- **Acteur principal** : Joueur
- **Acteurs secondaires** : Système IA, Générateur de niveaux
- **Type** : Primaire, essentiel

### Description
Le joueur lance une partie solo dans un labyrinthe généré procéduralement, avec des ennemis contrôlés par IA.

### Préconditions
- Le jeu est lancé et le menu principal est affiché
- Aucune partie n'est en cours

### Déclencheur
Le joueur sélectionne "Nouvelle partie" dans le menu principal

### Scénario nominal

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Joueur | Clique sur "Nouvelle partie" |
| 2 | Système | Affiche les options de configuration (taille labyrinthe, difficulté) |
| 3 | Joueur | Sélectionne les paramètres souhaités |
| 4 | Joueur | Confirme et lance la génération |
| 5 | Générateur | Génère le labyrinthe (Prim ou Kruskal selon configuration) |
| 6 | Système | Place le joueur au point de spawn |
| 7 | Système IA | Place les ennemis aléatoirement |
| 8 | Système | Lance la boucle de jeu (rendu + logique) |
| 9 | Joueur | Joue (se déplace, combat, interagit) |

### Scénarios alternatifs

**A1 : Annulation de la création**
- À l'étape 3, le joueur clique sur "Retour"
- Le système retourne au menu principal
- Fin du cas d'utilisation

**A2 : Génération échouée**
- À l'étape 5, la génération échoue (erreur mémoire, bug)
- Le système affiche un message d'erreur
- Le système propose de réessayer ou retourner au menu
- Retour à l'étape 2

**A3 : Pause du jeu**
- À l'étape 9, le joueur appuie sur "Échap"
- Le système affiche le menu pause
- Le joueur peut reprendre, configurer ou quitter
- Si reprise : retour à l'étape 9
- Si quitter : retour au menu principal

### Postconditions
- **Succès** : Le joueur est dans le labyrinthe et peut jouer
- **Échec** : Retour au menu principal

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V01.1 | Le labyrinthe doit être généré en moins de 3 secondes (taille standard) | Performance | Haute |
| V01.2 | Le labyrinthe doit être parfait (un seul chemin entre deux points) | Fonctionnel | Haute |
| V01.3 | Le framerate doit rester >= 30 FPS sur matériel cible | Performance | Haute |
| V01.4 | Les ennemis doivent être placés à au moins 5 cases du joueur | Fonctionnel | Moyenne |
| V01.5 | Le joueur doit spawner dans une zone libre (pas de mur) | Fonctionnel | Haute |
| V01.6 | Les contrôles doivent répondre en moins de 50ms | Performance | Haute |
| V01.7 | Aucune fuite mémoire après 30 min de jeu | Performance | Haute |

### Exigences non fonctionnelles
- **Utilisabilité** : Interface intuitive, temps d'apprentissage < 5 minutes
- **Performance** : Génération < 3s, rendu >= 30 FPS
- **Fiabilité** : Aucun crash durant une session de jeu normale

---

## CU02 - Créer une partie multijoueur

### Identification
- **ID** : CU02
- **Nom** : Créer une partie multijoueur
- **Acteur principal** : Joueur (Host)
- **Acteurs secondaires** : Serveur (mode Client-Serveur) ou Système réseau (mode P2P), Générateur de niveaux
- **Type** : Primaire, essentiel

### Description
Le joueur crée une partie multijoueur et devient l'hôte, attendant que d'autres joueurs rejoignent.

### Préconditions
- Le jeu est lancé
- Une connexion réseau est disponible
- Le joueur a choisi le mode multijoueur

### Déclencheur
Le joueur sélectionne "Créer une partie" dans le menu multijoueur

### Scénario nominal

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Joueur | Clique sur "Multijoueur" puis "Créer une partie" |
| 2 | Système | Affiche le choix du mode (Client-Serveur / P2P) |
| 3 | Joueur | Sélectionne le mode de jeu |
| 4 | Système | Affiche les paramètres (nom partie, max joueurs, taille carte, etc.) |
| 5 | Joueur | Configure les paramètres et valide |
| 6 | Système réseau | Ouvre le port et crée le socket serveur |
| 7 | Générateur | Génère le labyrinthe avec la seed |
| 8 | Système | Affiche l'écran d'attente avec IP/code de partie |
| 9 | Système | Attend la connexion d'autres joueurs |
| 10 | Joueur | Décide de démarrer la partie manuellement ou attend le nombre max |
| 11 | Système | Lance la partie multijoueur |

### Scénarios alternatifs

**A1 : Port déjà utilisé**
- À l'étape 6, le port est déjà occupé
- Le système tente un autre port ou demande au joueur d'en spécifier un
- Retour à l'étape 6

**A2 : Aucun joueur ne rejoint**
- À l'étape 9, timeout de 5 minutes sans connexion
- Le système demande si le joueur veut continuer à attendre
- Si non : annulation et retour au menu

**A3 : Annulation de la création**
- À n'importe quelle étape avant 11, le joueur annule
- Le système ferme le socket et libère les ressources
- Retour au menu multijoueur

**A4 : Joueur indésirable (kick)**
- À l'étape 9, un joueur se connecte
- L'hôte peut l'exclure avant le démarrage
- Le joueur exclu est déconnecté
- Retour à l'étape 9

### Postconditions
- **Succès** : La partie multijoueur est lancée avec au moins 2 joueurs
- **Échec** : Retour au menu multijoueur, ressources libérées

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V02.1 | Le socket doit s'ouvrir en moins de 2 secondes | Performance | Haute |
| V02.2 | L'IP/code de partie doit être affiché clairement | Ergonomie | Haute |
| V02.3 | La partie doit supporter au minimum 2 joueurs, maximum 8 | Fonctionnel | Haute |
| V02.4 | La seed de génération doit être partagée avec tous les clients | Fonctionnel | Haute |
| V02.5 | Tous les joueurs doivent avoir la même carte | Fonctionnel | Critique |
| V02.6 | Le host doit pouvoir démarrer manuellement avec 1+ autres joueurs | Fonctionnel | Moyenne |
| V02.7 | En mode P2P, l'IP doit être détectée automatiquement | Ergonomie | Moyenne |
| V02.8 | Le firewall ne doit pas bloquer (ou guide d'ouverture port) | Technique | Moyenne |

### Exigences non fonctionnelles
- **Sécurité** : Validation des connexions, protection contre DoS basique
- **Utilisabilité** : Instructions claires pour ouvrir les ports
- **Compatibilité** : Fonctionne sur LAN et Internet (avec port forwarding)

---

## CU03 - Rejoindre une partie multijoueur

### Identification
- **ID** : CU03
- **Nom** : Rejoindre une partie multijoueur
- **Acteur principal** : Joueur (Client)
- **Acteurs secondaires** : Serveur ou Host (P2P)
- **Type** : Primaire, essentiel

### Description
Le joueur rejoint une partie multijoueur existante créée par un autre joueur.

### Préconditions
- Le jeu est lancé
- Une connexion réseau est disponible
- Une partie est disponible (créée par un host)

### Déclencheur
Le joueur sélectionne "Rejoindre une partie" dans le menu multijoueur

### Scénario nominal

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Joueur | Clique sur "Multijoueur" puis "Rejoindre une partie" |
| 2 | Système | Affiche le choix : liste automatique ou saisie manuelle IP |
| 3 | Joueur | Choisit une option |
| 4a | Système | (Si auto) Scanne le réseau local et affiche les parties disponibles |
| 4b | Joueur | (Si manuel) Saisit l'IP et le port |
| 5 | Joueur | Sélectionne/valide la partie à rejoindre |
| 6 | Système réseau | Tente la connexion au serveur/host |
| 7 | Système réseau | Effectue le handshake (version protocole, etc.) |
| 8 | Système | Reçoit les données de la partie (carte, seed, joueurs connectés) |
| 9 | Système | Reconstruit le labyrinthe localement avec la seed |
| 10 | Système | Place le joueur à son spawn point |
| 11 | Système | Affiche "En attente du démarrage..." |
| 12 | Serveur/Host | Lance la partie |
| 13 | Système | Démarre le jeu en mode multijoueur |

### Scénarios alternatifs

**A1 : Connexion échouée**
- À l'étape 6, impossible de se connecter (timeout, refus)
- Le système affiche "Connexion impossible"
- Le joueur peut réessayer ou retourner au menu
- Retour à l'étape 2

**A2 : Version incompatible**
- À l'étape 7, la version du protocole diffère
- Le système affiche "Version incompatible, veuillez mettre à jour"
- Retour au menu

**A3 : Partie pleine**
- À l'étape 7, le serveur refuse (nombre max de joueurs atteint)
- Le système affiche "Partie complète"
- Retour à l'étape 2

**A4 : Exclu par l'hôte**
- À l'étape 11, l'hôte exclut le joueur
- Le système affiche "Vous avez été exclu"
- Retour au menu

**A5 : Pseudo déjà utilisé**
- À l'étape 7, le pseudo du joueur est déjà pris
- Le système demande un autre pseudo
- Retour à l'étape 7 avec nouveau pseudo

### Postconditions
- **Succès** : Le joueur est connecté et attend/joue dans la partie
- **Échec** : Retour au menu multijoueur

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V03.1 | La connexion doit réussir en moins de 5 secondes | Performance | Haute |
| V03.2 | Le scan automatique doit trouver les parties LAN en < 3s | Performance | Moyenne |
| V03.3 | Le handshake doit vérifier la compatibilité de version | Fonctionnel | Haute |
| V03.4 | La reconstruction locale doit être identique à l'original | Fonctionnel | Critique |
| V03.5 | Le joueur doit être notifié clairement de l'état de connexion | Ergonomie | Moyenne |
| V03.6 | Les données de partie doivent être reçues intégralement | Fonctionnel | Critique |
| V03.7 | Support de l'IPv4 et IPv6 | Technique | Basse |

### Exigences non fonctionnelles
- **Fiabilité** : Gestion des déconnexions durant le chargement
- **Ergonomie** : Messages d'erreur clairs et exploitables
- **Performance** : Chargement rapide même pour grandes cartes

---

## CU04 - Se déplacer dans le labyrinthe

### Identification
- **ID** : CU04
- **Nom** : Se déplacer dans le labyrinthe
- **Acteur principal** : Joueur
- **Acteurs secondaires** : Système de rendu (Raycasting), Système de collision
- **Type** : Primaire, essentiel

### Description
Le joueur utilise les contrôles pour se déplacer dans l'environnement 3D du labyrinthe.

### Préconditions
- Une partie est en cours (solo ou multi)
- Le joueur est vivant (HP > 0)
- Le jeu n'est pas en pause

### Déclencheur
Le joueur appuie sur une touche de déplacement (Z, Q, S, D, flèches)

### Scénario nominal

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Joueur | Appuie sur une touche de déplacement (ex: Z pour avancer) |
| 2 | Système | Détecte l'input clavier |
| 3 | Système | Calcule la nouvelle position en fonction de la direction et vitesse |
| 4 | Système collision | Vérifie si la nouvelle position est valide (pas de mur) |
| 5 | Système | Met à jour la position du joueur |
| 6 | Système rendu | Recalcule le raycasting depuis la nouvelle position |
| 7 | Système | Affiche la nouvelle vue à l'écran |
| 8 | Système réseau | (Si multi) Envoie la nouvelle position aux autres joueurs |

### Scénarios alternatifs

**A1 : Collision avec un mur**
- À l'étape 4, la nouvelle position contient un mur
- Le système refuse le déplacement
- La position reste inchangée
- Fin (pas de mouvement)

**A2 : Mouvement diagonal (2 touches simultanées)**
- À l'étape 1, le joueur appuie sur Z + D simultanément
- Le système calcule un vecteur diagonal
- Normalisation du vecteur pour vitesse constante
- Suite du scénario normal

**A3 : Rotation de la caméra**
- À l'étape 1, le joueur bouge la souris ou utilise les flèches
- Le système met à jour l'angle de vue (yaw, pitch)
- Pas de changement de position, seulement direction
- Recalcul du raycasting avec nouvel angle

**A4 : Interaction avec une porte**
- À l'étape 4, la position contient une porte fermée
- Le joueur appuie sur E pour ouvrir
- La porte s'ouvre (animation)
- Le passage devient libre pour les déplacements futurs

### Postconditions
- **Succès** : Le joueur est à la nouvelle position, la vue est mise à jour
- **Échec partiel** : Position inchangée en cas de collision

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V04.1 | La latence input → affichage doit être < 50ms | Performance | Haute |
| V04.2 | Le mouvement doit être fluide (interpolation si FPS < 60) | Ergonomie | Moyenne |
| V04.3 | Les collisions doivent être précises (hitbox cohérente) | Fonctionnel | Haute |
| V04.4 | La vitesse de déplacement doit être configurable | Fonctionnel | Basse |
| V04.5 | Le joueur ne doit pas traverser les murs (bug clipping) | Fonctionnel | Critique |
| V04.6 | Les déplacements doivent être synchronisés en < 100ms (multi) | Performance | Haute |
| V04.7 | Support des manettes (optionnel) | Ergonomie | Basse |

### Exigences non fonctionnelles
- **Performance** : 60 FPS stable pendant les déplacements
- **Ergonomie** : Contrôles réactifs et naturels (ZQSD standard)
- **Accessibilité** : Touches reconfigurables

---

## CU05 - Combattre un ennemi

### Identification
- **ID** : CU05
- **Nom** : Combattre un ennemi
- **Acteur principal** : Joueur
- **Acteurs secondaires** : Système IA (ennemi), Système de combat, Serveur (en multi)
- **Type** : Primaire, essentiel

### Description
Le joueur engage le combat avec un ennemi contrôlé par l'IA ou un autre joueur (en multi).

### Préconditions
- Une partie est en cours
- Le joueur est vivant (HP > 0)
- Un ennemi est à portée de tir
- Le joueur possède des munitions

### Déclencheur
Le joueur vise un ennemi et clique (tir)

### Scénario nominal

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Joueur | Vise un ennemi avec la souris |
| 2 | Joueur | Clique pour tirer |
| 3 | Système | Vérifie les munitions disponibles |
| 4 | Système | Effectue un raycast dans la direction de visée |
| 5 | Système | Détecte la collision avec l'ennemi |
| 6 | Système combat | Calcule les dégâts (distance, type d'arme) |
| 7 | Système | Déduit les HP de l'ennemi |
| 8 | Système | Affiche l'animation de tir et le feedback visuel/sonore |
| 9 | Système IA | L'ennemi réagit (prend des dégâts, cri) |
| 10 | Système IA | L'ennemi contre-attaque ou fuit |
| 11 | Système | (Si HP ennemi <= 0) L'ennemi meurt et disparaît |
| 12 | Système | Mise à jour du score/stats du joueur |

### Scénarios alternatifs

**A1 : Plus de munitions**
- À l'étape 3, munitions = 0
- Le système joue un son de "clic" vide
- Fin (pas de tir)

**A2 : Tir manqué**
- À l'étape 5, le raycast ne touche aucun ennemi (mur ou vide)
- Consommation de munition quand même
- Pas de dégât infligé
- Animation de tir quand même
- Fin

**A3 : Ennemi en armure**
- À l'étape 6, l'ennemi possède une armure
- Les dégâts sont réduits
- L'armure se détériore
- Suite du scénario

**A4 : Attaque au corps à corps**
- À l'étape 2, le joueur utilise une arme de mêlée
- Vérification de la proximité (< 2 mètres)
- Dégâts fixes sans munitions
- Suite du scénario

**A5 : Joueur tué par l'ennemi**
- À l'étape 10, l'ennemi riposte et tue le joueur (HP <= 0)
- Écran de mort
- Option de respawn (multi) ou fin de partie (solo)

**A6 : Multijoueur - Validation serveur**
- À l'étape 5, en mode multi Client-Serveur
- Le client envoie l'action SHOOT au serveur
- Le serveur recalcule le raycast (anti-triche)
- Le serveur valide ou refuse le tir
- Seul le résultat serveur compte

### Postconditions
- **Succès** : L'ennemi est blessé ou mort, le joueur perd une munition
- **Échec** : Munition perdue sans effet (tir manqué)

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V05.1 | Le raycast doit être précis (hitbox de l'ennemi cohérente) | Fonctionnel | Haute |
| V05.2 | Les dégâts doivent être calculés en fonction de la distance | Fonctionnel | Moyenne |
| V05.3 | Le feedback visuel/sonore doit être immédiat (< 100ms) | Ergonomie | Haute |
| V05.4 | En multi, le serveur doit valider tous les tirs (anti-triche) | Sécurité | Critique |
| V05.5 | Les munitions doivent être décomptées correctement | Fonctionnel | Haute |
| V05.6 | L'IA doit réagir dans les 200ms après avoir été touchée | Performance | Moyenne |
| V05.7 | Les points de vie ne doivent jamais être négatifs | Fonctionnel | Moyenne |
| V05.8 | Le corps de l'ennemi mort doit rester visible temporairement | Ergonomie | Basse |

### Exigences non fonctionnelles
- **Jouabilité** : Combat fluide et réactif
- **Équilibrage** : Dégâts et difficulté ajustés
- **Sécurité** : Impossible de tricher en multijoueur

---

## CU06 - Interagir avec un PNJ

### Identification
- **ID** : CU06
- **Nom** : Interagir avec un PNJ (Non-Player Character)
- **Acteur principal** : Joueur
- **Acteurs secondaires** : Système IA (PNJ), Modèle de langage (Mistral)
- **Type** : Secondaire, optionnel (Phase 5)

### Description
Le joueur dialogue avec un PNJ qui répond intelligemment grâce à un modèle de langage.

### Préconditions
- Une partie est en cours
- Le joueur est proche d'un PNJ (< 3 mètres)
- Le système IA est actif
- Une connexion au modèle de langage est disponible

### Déclencheur
Le joueur appuie sur E (interaction) devant un PNJ

### Scénario nominal

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Joueur | Appuie sur E devant un PNJ |
| 2 | Système | Détecte la proximité et affiche l'interface de dialogue |
| 3 | Système | Affiche le message d'accueil du PNJ (contextualisé) |
| 4 | Joueur | Tape une question ou sélectionne une option |
| 5 | Système | Envoie le prompt au modèle IA (avec contexte du jeu) |
| 6 | Modèle IA | Génère une réponse cohérente |
| 7 | Système | Affiche la réponse du PNJ |
| 8 | Système | (Si quête disponible) Propose une quête générée |
| 9 | Joueur | Continue le dialogue, accepte la quête, ou quitte |
| 10 | Système | (Si quête acceptée) Ajoute l'objectif au journal |
| 11 | Joueur | Ferme le dialogue (Échap ou fin de conversation) |

### Scénarios alternatifs

**A1 : Modèle IA indisponible**
- À l'étape 6, le service IA ne répond pas (timeout)
- Le système affiche un dialogue pré-écrit générique
- Le joueur peut continuer avec des options limitées

**A2 : Réponse inappropriée de l'IA**
- À l'étape 6, l'IA génère du contenu hors contexte ou inapproprié
- Le système filtre et remplace par une réponse par défaut
- Log de l'incident pour amélioration

**A3 : Attaque du PNJ**
- À l'étape 1, le joueur attaque le PNJ au lieu d'interagir
- Le PNJ réagit (fuit, se défend, ou meurt selon le type)
- Fin du dialogue possible avec ce PNJ

**A4 : Plusieurs joueurs interagissent**
- À l'étape 1, en multijoueur, un autre joueur dialogue déjà
- Le système met en file d'attente ou permet une conversation de groupe
- Chaque joueur peut voir les réponses

### Postconditions
- **Succès** : Le joueur a obtenu des informations ou une quête
- **Neutre** : Dialogue terminé sans effet particulier
- **Échec** : PNJ hostile ou dialogue impossible

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V06.1 | La réponse de l'IA doit arriver en < 2 secondes | Performance | Haute |
| V06.2 | Le contexte du jeu doit être intégré au prompt (position, quêtes) | Fonctionnel | Haute |
| V06.3 | Les réponses doivent être cohérentes avec le rôle du PNJ | Qualité | Haute |
| V06.4 | Un filtre de contenu doit bloquer les propos inappropriés | Sécurité | Haute |
| V06.5 | Le dialogue doit être sauvegardé dans l'historique | Fonctionnel | Basse |
| V06.6 | Le joueur doit pouvoir interrompre à tout moment | Ergonomie | Moyenne |
| V06.7 | Les quêtes générées doivent être réalisables dans le jeu | Fonctionnel | Haute |

### Exigences non fonctionnelles
- **Immersion** : Dialogues naturels et contextualisés
- **Performance** : Temps de réponse acceptable
- **Coût** : Limitation des appels API si service payant

---

## CU07 - Communiquer via chat

### Identification
- **ID** : CU07
- **Nom** : Communiquer via chat textuel
- **Acteur principal** : Joueur
- **Acteurs secondaires** : Serveur (mode CS) ou autres clients (mode P2P)
- **Type** : Secondaire, important (multijoueur uniquement)

### Description
Les joueurs peuvent s'envoyer des messages textuels pendant une partie multijoueur.

### Préconditions
- Une partie multijoueur est en cours
- Le joueur est connecté
- Au moins un autre joueur est présent

### Déclencheur
Le joueur appuie sur T (touche chat)

### Scénario nominal

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Joueur | Appuie sur T |
| 2 | Système | Ouvre la zone de saisie de texte (focus) |
| 3 | Joueur | Tape un message (max 200 caractères) |
| 4 | Joueur | Appuie sur Entrée pour envoyer |
| 5 | Système | Vérifie la validité du message (longueur, contenu) |
| 6 | Système réseau | Envoie le message au serveur ou broadcast (P2P) |
| 7 | Serveur/P2P | Transmet le message à tous les joueurs |
| 8 | Système | Affiche le message dans le chat de tous les joueurs |
| 9 | Système | Ferme la zone de saisie et redonne le contrôle au jeu |

### Scénarios alternatifs

**A1 : Message vide**
- À l'étape 4, le joueur envoie un message vide
- Le système ignore l'envoi
- Retour à l'étape 2

**A2 : Message trop long**
- À l'étape 5, le message dépasse 200 caractères
- Le système tronque automatiquement ou affiche une erreur
- Retour à l'étape 3

**A3 : Contenu inapproprié détecté**
- À l'étape 5, le filtre détecte des insultes/spam
- Le message est bloqué
- Avertissement au joueur
- Retour à l'étape 2

**A4 : Annulation**
- À l'étape 3, le joueur appuie sur Échap
- La zone de saisie se ferme sans envoi
- Retour au jeu

**A5 : Déconnexion pendant l'envoi**
- À l'étape 6, le réseau est coupé
- Le message n'est pas envoyé
- Notification "Échec d'envoi"
- Le joueur est déconnecté

**A6 : Spam détecté**
- Le joueur envoie 5+ messages en moins de 3 secondes
- Le système applique un cooldown de 10 secondes
- Messages bloqués temporairement

### Postconditions
- **Succès** : Le message est affiché chez tous les joueurs
- **Échec** : Message bloqué ou non envoyé

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V07.1 | Le message doit arriver en < 500ms (LAN) ou < 2s (Internet) | Performance | Haute |
| V07.2 | Le chat doit afficher les 20 derniers messages | Fonctionnel | Moyenne |
| V07.3 | Le filtre anti-spam doit bloquer 5+ msg en 3s | Sécurité | Moyenne |
| V07.4 | Le filtre de contenu doit bloquer les insultes courantes | Sécurité | Haute |
| V07.5 | Le pseudo de l'émetteur doit être clairement affiché | Ergonomie | Haute |
| V07.6 | Le chat ne doit pas bloquer le jeu (saisie en overlay) | Ergonomie | Haute |
| V07.7 | Les messages doivent être horodatés | Fonctionnel | Basse |

### Exigences non fonctionnelles
- **Modération** : Filtrage automatique du contenu
- **Ergonomie** : Chat discret mais accessible
- **Performance** : Pas d'impact sur le framerate

---

## CU08 - Configurer les paramètres

### Identification
- **ID** : CU08
- **Nom** : Configurer les paramètres du jeu
- **Acteur principal** : Joueur
- **Acteurs secondaires** : Système de configuration
- **Type** : Secondaire, confort

### Description
Le joueur personnalise les paramètres graphiques, audio et de contrôle.

### Préconditions
- Le jeu est lancé

### Déclencheur
Le joueur accède au menu "Options" ou "Paramètres"

### Scénario nominal

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Joueur | Clique sur "Options" dans le menu principal ou pause |
| 2 | Système | Affiche les catégories (Graphismes, Audio, Contrôles, Gameplay) |
| 3 | Joueur | Sélectionne une catégorie |
| 4 | Système | Affiche les paramètres de la catégorie avec valeurs actuelles |
| 5 | Joueur | Modifie un ou plusieurs paramètres |
| 6 | Système | Applique les changements en temps réel (si possible) |
| 7 | Joueur | Clique sur "Sauvegarder" ou "Appliquer" |
| 8 | Système | Sauvegarde la configuration dans un fichier |
| 9 | Système | Redémarre les composants nécessaires (ex: moteur rendu) |
| 10 | Système | Retourne au menu précédent |

### Scénarios alternatifs

**A1 : Annulation des modifications**
- À l'étape 7, le joueur clique sur "Annuler"
- Le système restaure les valeurs précédentes
- Aucun changement n'est sauvegardé
- Retour au menu

**A2 : Valeur invalide**
- À l'étape 5, le joueur entre une valeur hors limites
- Le système affiche un message d'erreur et la plage valide
- Retour à l'étape 5

**A3 : Redémarrage requis**
- À l'étape 9, certains paramètres nécessitent un redémarrage complet
- Le système affiche "Redémarrage requis pour appliquer"
- Le joueur peut continuer mais les changements seront effectifs au prochain lancement

**A4 : Réinitialisation par défaut**
- À n'importe quelle étape, le joueur clique "Paramètres par défaut"
- Le système restaure toutes les valeurs d'usine
- Demande de confirmation
- Suite du scénario

### Postconditions
- **Succès** : Les paramètres sont sauvegardés et appliqués
- **Annulation** : Retour aux paramètres précédents

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V08.1 | Les paramètres doivent être sauvegardés dans un fichier config.ini | Fonctionnel | Haute |
| V08.2 | Les modifications graphiques doivent s'appliquer en < 1s | Performance | Moyenne |
| V08.3 | Résolutions supportées : au moins 3 (720p, 1080p, 1440p) | Fonctionnel | Moyenne |
| V08.4 | Volume audio réglable de 0 à 100% | Fonctionnel | Haute |
| V08.5 | Toutes les touches doivent être reconfigurables | Fonctionnel | Moyenne |
| V08.6 | Détection automatique des manettes (optionnel) | Ergonomie | Basse |
| V08.7 | Un bouton "Tester" pour les contrôles | Ergonomie | Basse |
| V08.8 | Les paramètres doivent persister entre les sessions | Fonctionnel | Haute |

### Exigences non fonctionnelles
- **Utilisabilité** : Interface claire et organisée
- **Fiabilité** : Pas de corruption du fichier de configuration
- **Compatibilité** : Support de différentes résolutions et périphériques

---

## CU09 - Gérer les ennemis (IA)

### Identification
- **ID** : CU09
- **Nom** : Gérer le comportement des ennemis (IA)
- **Acteur principal** : Système IA
- **Acteurs secondaires** : Système de pathfinding, Joueur (cible)
- **Type** : Interne, critique

### Description
Le système IA gère les comportements des ennemis (patrouille, détection, poursuite, attaque, fuite).

### Préconditions
- Une partie est en cours
- Des ennemis sont présents sur la carte
- L'ennemi est vivant (HP > 0)

### Déclencheur
La boucle de jeu met à jour l'IA (tick ~30 Hz)

### Scénario nominal (Machine à états)

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Système IA | Évalue l'état actuel de l'ennemi |
| 2 | Système IA | **État PATROUILLE** : Se déplace sur un chemin prédéfini |
| 3 | Système IA | Vérifie si le joueur est dans le champ de vision (FOV 120°, distance < 10m) |
| 4 | Système IA | Détection positive → Transition vers état ALERTE |
| 5 | Système IA | **État ALERTE** : S'arrête et regarde le joueur 1 seconde |
| 6 | Système IA | Transition vers état POURSUITE |
| 7 | Système IA | **État POURSUITE** : Calcule le chemin vers le joueur (Dijkstra/BFS) |
| 8 | Système IA | Suit le chemin calculé |
| 9 | Système IA | Vérifie la distance au joueur |
| 10 | Système IA | Distance < 2m → Transition vers état ATTAQUE |
| 11 | Système IA | **État ATTAQUE** : Inflige des dégâts au joueur (20 HP toutes les 2s) |
| 12 | Système | Le joueur perd des HP |
| 13 | Système IA | Vérifie si le joueur s'éloigne ou si l'ennemi prend trop de dégâts |
| 14 | Système IA | Conditions de fuite atteintes → Transition vers état FUITE |
| 15 | Système IA | **État FUITE** : Calcule le chemin opposé au joueur |
| 16 | Système IA | Fuit jusqu'à distance sécurisée (> 15m) |
| 17 | Système IA | Retour à état PATROUILLE |

### Scénarios alternatifs

**A1 : Joueur hors de vue pendant la poursuite**
- À l'étape 8, le joueur sort du champ de vision
- L'IA continue vers la dernière position connue
- Si le joueur n'est pas retrouvé en 5 secondes : retour PATROUILLE

**A2 : Ennemi bloqué par un obstacle**
- À l'étape 8, le pathfinding échoue (pas de chemin)
- L'IA tente un recalcul avec un autre algorithme
- Si échec persistant : retour PATROUILLE

**A3 : Mort de l'ennemi**
- À n'importe quelle étape, HP <= 0
- Transition vers état MORT
- Animation de mort
- Suppression de l'entité après 5 secondes

**A4 : Plusieurs ennemis coordonnés**
- À l'étape 7, plusieurs ennemis détectent le joueur
- Comportement de groupe : encerclement
- Communication simple entre IA (positions)

**A5 : Ennemi boss (comportement spécial)**
- Les boss ont des états supplémentaires
- Attaques spéciales, phases multiples
- Règles de combat différentes

### Postconditions
- L'ennemi est dans un état cohérent
- Les actions sont synchronisées avec le rendu
- Les dégâts sont appliqués si en état ATTAQUE

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V09.1 | Le pathfinding doit calculer un chemin en < 100ms | Performance | Haute |
| V09.2 | La détection doit être cohérente (FOV 120°, distance 10m) | Fonctionnel | Haute |
| V09.3 | Les ennemis ne doivent pas se bloquer entre eux | Fonctionnel | Moyenne |
| V09.4 | Les transitions d'état doivent être fluides (pas de saccades) | Ergonomie | Moyenne |
| V09.5 | L'IA doit gérer au moins 10 ennemis simultanément sans lag | Performance | Haute |
| V09.6 | Les ennemis doivent éviter les murs (pas de clipping) | Fonctionnel | Haute |
| V09.7 | La difficulté doit être ajustable (vitesse, dégâts, HP) | Fonctionnel | Moyenne |
| V09.8 | Les ennemis morts ne doivent plus consommer de CPU | Performance | Moyenne |

### Exigences non fonctionnelles
- **Performance** : IA optimisée, pas d'impact sur le framerate
- **Équilibrage** : Difficulté progressive et juste
- **Variété** : Différents types d'ennemis avec comportements variés

---

## CU10 - Synchroniser l'état du jeu (Multijoueur)

### Identification
- **ID** : CU10
- **Nom** : Synchroniser l'état du jeu entre joueurs
- **Acteur principal** : Système réseau
- **Acteurs secondaires** : Serveur (mode CS), Clients
- **Type** : Interne, critique (multijoueur)

### Description
Le système synchronise en temps réel les positions, actions et états de tous les joueurs et entités.

### Préconditions
- Une partie multijoueur est en cours
- Au moins 2 joueurs sont connectés
- La connexion réseau est active

### Déclencheur
La boucle de synchronisation (tick ~30 Hz)

### Scénario nominal (Mode Client-Serveur)

| Étape | Acteur | Action |
|-------|--------|--------|
| 1 | Client | Détecte un changement d'état local (mouvement, tir, etc.) |
| 2 | Client | Envoie un paquet UPDATE au serveur {id, type, data, timestamp} |
| 3 | Serveur | Reçoit le paquet |
| 4 | Serveur | Valide la légitimité de l'action (anti-triche) |
| 5 | Serveur | Met à jour l'état autoritatif du monde |
| 6 | Serveur | Prépare les paquets de synchronisation pour tous les clients |
| 7 | Serveur | Envoie les mises à jour à tous les clients (broadcast) |
| 8 | Clients | Reçoivent les paquets de synchronisation |
| 9 | Clients | Mettent à jour leurs états locaux (positions joueurs, ennemis, etc.) |
| 10 | Clients | Interpolent les positions pour la fluidité |
| 11 | Système | Affiche les changements à l'écran |

### Scénarios alternatifs

**A1 : Paquet perdu**
- À l'étape 3, le paquet n'arrive jamais au serveur (perte réseau)
- Le client attend un ACK pendant 200ms
- Timeout → Renvoi du paquet
- Maximum 3 tentatives
- Si échec : interpolation côté client

**A2 : Latence élevée**
- À l'étape 3, RTT > 200ms
- Le serveur estampille le paquet avec le timestamp serveur
- Application de la compensation de lag côté serveur
- Prédiction côté client pour masquer la latence

**A3 : Action invalidée par le serveur**
- À l'étape 4, le serveur détecte une tricherie (ex: vitesse impossible)
- Le serveur rejette l'action
- Envoie une correction au client fautif
- Le client rollback sa position à l'état serveur

**A4 : Synchronisation d'ennemis**
- À l'étape 5, le serveur met à jour les ennemis IA
- Les clients ne calculent pas l'IA, ils reçoivent juste les positions
- Interpolation côté client pour fluidité

**A5 : Mode P2P**
- Pas de serveur central
- Chaque client broadcast ses états à tous les autres
- Résolution de conflits par timestamp ou host authority
- Plus de trafic réseau (n² connexions)

**A6 : Déconnexion temporaire**
- À l'étape 8, un client ne répond plus
- Le serveur attend 10s (heartbeat timeout)
- Déconnexion du joueur fantôme
- Notification aux autres clients

### Postconditions
- Tous les clients ont un état cohérent du monde
- Les différences sont minimes (< 100ms de latence perçue)
- Aucune incohérence majeure (positions, HP, etc.)

### Conditions de validation

| ID | Condition | Type | Critique |
|----|-----------|------|----------|
| V10.1 | La synchronisation doit se faire au minimum 20 fois/seconde | Performance | Haute |
| V10.2 | La latence client-serveur-client doit être < 100ms (LAN) | Performance | Haute |
| V10.3 | Les positions doivent être synchronisées avec précision +/- 0.1 unité | Fonctionnel | Haute |
| V10.4 | Le serveur doit détecter les vitesses impossibles (anti-triche) | Sécurité | Critique |
| V10.5 | En P2P, un mécanisme de résolution de conflits doit exister | Fonctionnel | Haute |
| V10.6 | Les paquets doivent être compressés pour économiser la bande passante | Performance | Moyenne |
| V10.7 | Interpolation linéaire pour positions, extrapolation pour prédiction | Fonctionnel | Moyenne |
| V10.8 | Support de 2 à 8 joueurs simultanés sans dégradation | Scalabilité | Haute |
| V10.9 | Heartbeat toutes les 2-5 secondes pour détecter les déconnexions | Fiabilité | Haute |

### Exigences non fonctionnelles
- **Performance** : Bande passante optimisée, pas de goulets
- **Fiabilité** : Gestion des pertes de paquets, reconnexion
- **Sécurité** : Validation serveur, chiffrement (optionnel)
- **Scalabilité** : Support de plusieurs parties simultanées

---

## 5. Matrice de traçabilité

Cette matrice lie les cas d'utilisation aux fonctionnalités du système et aux exigences.

| CU | Fonctionnalité principale | Phase | Priorité | Acteurs | Dépendances |
|----|---------------------------|-------|----------|---------|-------------|
| CU01 | Jeu solo | 1 (MVP) | Critique | Joueur, IA, Générateur | - |
| CU02 | Créer partie multi | 4 | Haute | Joueur, Serveur/Réseau | CU01 |
| CU03 | Rejoindre partie multi | 4 | Haute | Joueur, Serveur/Réseau | CU02 |
| CU04 | Déplacements | 1 (MVP) | Critique | Joueur, Rendu | - |
| CU05 | Combat | 2 | Critique | Joueur, IA, Serveur | CU04 |
| CU06 | Dialogues PNJ | 5 (opt.) | Basse | Joueur, IA (LLM) | - |
| CU07 | Chat multijoueur | 4 | Moyenne | Joueur, Réseau | CU02/CU03 |
| CU08 | Configuration | 3 | Moyenne | Joueur | - |
| CU09 | IA ennemis | 2 | Haute | Système IA | CU01 |
| CU10 | Synchronisation | 4 | Critique | Réseau, Serveur | CU02/CU03 |

---

## Récapitulatif des conditions de validation par priorité

### 🔴 **Critique** (Bloquant si non respecté)

| ID | Condition | CU associé |
|----|-----------|------------|
| V01.5 | Le joueur doit spawner dans une zone libre | CU01 |
| V02.5 | Tous les joueurs doivent avoir la même carte | CU02 |
| V03.4 | La reconstruction locale doit être identique à l'original | CU03 |
| V03.6 | Les données de partie doivent être reçues intégralement | CU03 |
| V04.5 | Le joueur ne doit pas traverser les murs | CU04 |
| V05.4 | En multi, le serveur doit valider tous les tirs | CU05 |
| V10.4 | Le serveur doit détecter les vitesses impossibles | CU10 |

### 🟠 **Haute** (Fonctionnalité majeure impactée)

| ID | Condition | CU associé |
|----|-----------|------------|
| V01.1 | Génération labyrinthe < 3s | CU01 |
| V01.2 | Labyrinthe parfait | CU01 |
| V01.3 | Framerate >= 30 FPS | CU01 |
| V01.6 | Latence contrôles < 50ms | CU01 |
| V02.1 | Socket ouvert < 2s | CU02 |
| V04.1 | Latence input → affichage < 50ms | CU04 |
| V04.6 | Synchronisation déplacements < 100ms | CU04 |
| V05.1 | Raycast précis | CU05 |
| V05.5 | Décompte munitions correct | CU05 |
| V09.1 | Pathfinding < 100ms | CU09 |
| V09.5 | 10+ ennemis sans lag | CU09 |
| V10.1 | Synchronisation 20+ Hz | CU10 |
| V10.2 | Latence < 100ms (LAN) | CU10 |

### 🟡 **Moyenne** (Améliore l'expérience)

| ID | Condition | CU associé |
|----|-----------|------------|
| V01.4 | Ennemis à 5+ cases du spawn | CU01 |
| V04.2 | Mouvement fluide | CU04 |
| V05.6 | Réaction IA < 200ms | CU05 |
| V06.6 | Interruption dialogue possible | CU06 |
| V07.3 | Anti-spam (5+ msg/3s) | CU07 |
| V09.3 | Pas de blocage entre ennemis | CU09 |

### 🟢 **Basse** (Confort, optionnel)

| ID | Condition | CU associé |
|----|-----------|------------|
| V04.4 | Vitesse configurable | CU04 |
| V04.7 | Support manettes | CU04 |
| V05.8 | Corps visible temporairement | CU05 |
| V06.5 | Historique dialogue | CU06 |
| V07.7 | Messages horodatés | CU07 |
| V08.6 | Détection auto manettes | CU08 |

---

## Conclusion

Ce document définit **10 cas d'utilisation** couvrant toutes les fonctionnalités du système, avec :
- **Scénarios détaillés** (nominal + alternatifs)
- **70+ conditions de validation** mesurables
- **Priorisation** (critique → basse)
- **Matrice de traçabilité** liant CU, phases et dépendances

Ces spécifications constituent la base pour :
1. Le **développement** (implémentation guidée)
2. Les **tests** (validation des conditions)
3. La **documentation** utilisateur
4. La **planification** (roadmap par phases)

---

**Version** : 1.0  
**Date** : 4 novembre 2025  
**Auteurs** : Équipe ProjetTutoreDOOM

