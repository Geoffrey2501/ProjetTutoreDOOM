package monstre;

import java.util.ArrayList;
import moteur_graphique.CollisionStrategy;

public class RRT {
    private CollisionStrategy collision;

    private int MAX_ITERATIONS = 2000;

    private final double DEFAULT_MAX_DISTANCE_POINT = 1.0;
    private final double RAYON_RECHERCHE = 1.5;  // Rayon pour rewiring RRT*
    private double rayonMonstre = Monstre.RAYON;  // Rayon du monstre pour les collisions

    private ArrayList<Noeud> noeuds = new ArrayList<>();
    private Noeud debut;
    private Noeud fin;
    private double mapLargeur = 100.0;
    private double mapHauteur = 100.0;

    public RRT(CollisionStrategy collision) {
        this.collision = collision;
    }

    public Noeud trouverChemin(double startX, double startY, double endX, double endY) {
        noeuds.clear();
        // Ne pas effacer l'arbre existant - on l'étend
        debut = new Noeud(startX, startY);
        debut.setCout(0);
        fin = new Noeud(endX, endY);

        // Ajouter le debut seulement si l'arbre est vide
        if (noeuds.isEmpty()) {
            noeuds.add(debut);
        }

        Noeud meilleurVersLaFin = null;

        for (int i = 0; i < MAX_ITERATIONS; i += 1) {


            //Générer un point aléatoire
            double [] coordAleatoires = getCoordonneesAleatoires(endX, endY);
            //Trouver le nœud le plus proche
            Noeud plusProche = trouverPlusProche(coordAleatoires[0], coordAleatoires[1]);

            //Créer un nouveau nœud vers le point aléatoire
            Noeud nouveau = creerNoeudVers(plusProche, coordAleatoires[0], coordAleatoires[1]);

            // Si le nœud est dans un mur ou le segment traverse un mur, on passe
            if (nouveau == null) {
                continue;
            }

            //Trouver le meilleur parent dans le voisinage
            Noeud meilleurParent = trouverMeilleurParent(nouveau);
            if (meilleurParent == null) meilleurParent = plusProche;

            //Connecter au meilleur parent
            double coutNouveau = meilleurParent.getCout() + calculerDistance(meilleurParent, nouveau);
            nouveau.setParent(meilleurParent);
            nouveau.setCout(coutNouveau);
            nouveau.ajouterVoisin(meilleurParent);
            meilleurParent.ajouterVoisin(nouveau);
            noeuds.add(nouveau);

            //Optimiser les connexions des nœuds voisins (RRT*)
            optimiserConnexionNoeuds(nouveau);

            //vérifier si on peut atteindre la fin
            double distanceFin = calculerDistance(nouveau, fin);
            if (distanceFin <= DEFAULT_MAX_DISTANCE_POINT) {
                // Vérifier que le segment ne traverse pas un mur (avec le rayon du monstre)
                if (!traverseMurAvecRayon(nouveau.getX(), nouveau.getY(), fin.getX(), fin.getY(), rayonMonstre)) {
                    double coutViaNouveau = nouveau.getCout() + distanceFin;
                    if (meilleurVersLaFin == null || coutViaNouveau < fin.getCout()) {
                        fin.setParent(nouveau);
                        fin.setCout(coutViaNouveau);
                        if (meilleurVersLaFin == null) {
                            noeuds.add(fin);
                            nouveau.ajouterVoisin(fin);
                            fin.ajouterVoisin(nouveau);
                        }
                        meilleurVersLaFin = nouveau;
                    }
                }
            }
        }

        return fin.getParent() != null ? fin : null;
    }

    private Noeud trouverPlusProche(double x, double y) {
        Noeud plusProche = null;
        double distanceMin = Double.MAX_VALUE;
        for (Noeud n : noeuds) {
            double distance = Math.sqrt(Math.pow(n.getX() - x, 2) + Math.pow(n.getY() - y, 2));
            if (distance < distanceMin) {
                distanceMin = distance;
                plusProche = n;
            }
        }
        return plusProche;
    }

    private Noeud creerNoeudVers(Noeud depuis, double versX, double versY) {
        double dx = versX - depuis.getX();
        double dy = versY - depuis.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        double newX, newY;
        if (distance <= DEFAULT_MAX_DISTANCE_POINT) {
            newX = versX;
            newY = versY;
        } else {
            newX = depuis.getX() + (dx / distance) * DEFAULT_MAX_DISTANCE_POINT;
            newY = depuis.getY() + (dy / distance) * DEFAULT_MAX_DISTANCE_POINT;
        }

        // Vérifier si le nouveau nœud est dans un mur (avec le rayon du monstre)
        if (collision.isColliding(newX, newY, rayonMonstre)) {
            return null;
        }

        // Vérifier si le segment traverse un mur
        if (traverseMurAvecRayon(depuis.getX(), depuis.getY(), newX, newY, rayonMonstre)) {
            return null;
        }

        return new Noeud(newX, newY);
    }

    // Vérifie si un segment traverse un mur en échantillonnant
    private boolean traverseMurAvecRayon(double x1, double y1, double x2, double y2, double rayon) {
        double dist = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        int steps = (int) Math.max(1, dist * 5); // 5 points per unit
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double cx = x1 + t * (x2 - x1);
            double cy = y1 + t * (y2 - y1);
            if (collision.isColliding(cx, cy, rayon)) {
                return true;
            }
        }
        return false;
    }

    // RRT* : Trouver le meilleur parent parmi les voisins
    private Noeud trouverMeilleurParent(Noeud nouveau) {
        Noeud meilleurParent = null;
        double meilleurCout = Double.MAX_VALUE;

        for (Noeud n : noeuds) {
            double distance = calculerDistance(n, nouveau);
            if (distance <= RAYON_RECHERCHE) {
                if (!traverseMurAvecRayon(n.getX(), n.getY(), nouveau.getX(), nouveau.getY(), rayonMonstre)) {
                    double coutPotentiel = n.getCout() + distance;
                    if (coutPotentiel < meilleurCout) {
                        meilleurCout = coutPotentiel;
                        meilleurParent = n;
                    }
                }
            }
        }
        return meilleurParent;
    }

    // RRT* : Rewiring - réoptimiser les connexions des voisins
    private void optimiserConnexionNoeuds(Noeud nouveau) {
        for (Noeud n : noeuds) {
            if (n != nouveau && n != debut) {
                double distance = calculerDistance(nouveau, n);
                if (distance <= RAYON_RECHERCHE) {
                    if (!traverseMurAvecRayon(nouveau.getX(), nouveau.getY(), n.getX(), n.getY(), rayonMonstre)) {
                        double nouveauCout = nouveau.getCout() + distance;
                        if (nouveauCout < n.getCout()) {
                            n.setParent(nouveau);
                            n.setCout(nouveauCout);
                        }
                    }
                }
            }
        }
    }

    private double[] getCoordonneesAleatoires(double endX, double endY) {
        double randX, randY;
        if (Math.random() < 0.1) {
            randX = endX;
            randY = endY;
        } else {
            do {
                randX = Math.random() * mapLargeur;
                randY = Math.random() * mapHauteur;
            } while (collision.isColliding(randX, randY, rayonMonstre));
        }
        return new double[] {randX, randY};
    }

    public ArrayList<Noeud> getNoeuds() {
        return noeuds;
    }

    public Noeud getDebut() {
        return debut;
    }

    public Noeud getFin() {
        return fin;
    }

    private double calculerDistance(Noeud courant, Noeud n) {
        double[] coordCourant = courant.getCoordonnees();
        double[] coordN = n.getCoordonnees();
        return Math.sqrt(Math.pow(coordCourant[0] - coordN[0], 2) + Math.pow(coordCourant[1] - coordN[1], 2));
    }
}
