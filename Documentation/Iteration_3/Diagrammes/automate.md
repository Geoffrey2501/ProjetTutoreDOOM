L'automate va avoir plusieurs états, et va pouvoir faire des transitions entre ces états. 
Voici les états possibles pour l'automate :
- `Attente`: L'automate est en attente d'une action.
- `Patrouille`: L'automate est en train de patrouiller dans le labyrinthe entre un point A et B.
- `Poursuite`: L'automate est en train de poursuivre une cible.

Voici les transitions possibles entre les états de l'automate :
- `Attente` -> `Patrouille`: L'automate commence à patrouiller après un certain temps d'attente.
- `Patrouille` -> `Attente`: L'automate retourne en attente après avoir terminé sa patrouille.
- `Attente` -> `Poursuite`: L'automate commence à poursuivre une cible après l'avoir détectée.
- `Poursuite` -> `Attente`: L'automate retourne en attente après avoir perdu la cible ou après l'avoir capturée.
- `Patrouille` -> `Poursuite`: L'automate commence à poursuivre une cible après l'avoir détectée pendant sa patrouille.

Voir le diagramme de l'automate Automate.puml.


Code de l'automate en pseudo-code :

```
Algorithme Automate
Etat : Attente, Patrouille, Poursuite
    
Tant que vrai faire // boucle de jeux
      Si Etat == Attente alors
            Attendre un certain temps
            Etat = Patrouille
      
      Sinon si Etat == Patrouille alors
          Patrouiller entre les points A et B
          Si une cible est détectée alors
              Etat = Poursuite  
          Sinon si la patrouille est terminée alors
              Etat = Attente
          Sinon
              Patrouiller entre les points A et B
               
      Sinon si Etat == Poursuite alors
          Poursuivre la cible
          Si la cible est perdue ou capturée alors
              Etat = Attente
```


```

```



