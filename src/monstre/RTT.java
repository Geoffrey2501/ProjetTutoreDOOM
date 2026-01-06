package monstre;

import java.util.ArrayList;

public class RTT {
    private Map map;

    private int MAX_ITERATIONS = 1000;

    private static final int DEFAULT_MAX_DISTANCE_POINT = 50;
    private static final int RAYON_RECHERCHE = 80;  // Rayon pour rewiring RRT*

    private ArrayList<Noeud> noeuds;
    private Noeud debut;
    private Noeud fin;

    public RTT(Map map, int maxDistancePoint) {
        this.map = map;
        this.noeuds = new ArrayList<>();
    }

    public Noeud trouverChemin(int startX, int startY, int endX, int endY) {
        noeuds.clear();
        debut = new Noeud(startX, startY);
        debut.setCout(0);  // Le départ a un coût de 0
        fin = new Noeud(endX, endY);
        noeuds.add(debut);

        Noeud meilleurVersLaFin = null;

        for (int i = 0; i < MAX_ITERATIONS; i += 1) {

            //Générer un point aléatoire
            int [] coordAleatoires = getCoordonneesAleatoires(endX, endY);
            //Trouver le nœud le plus proche
            Noeud plusProche = trouverPlusProche(coordAleatoires[0], coordAleatoires[1]);

            //Créer un nouveau nœud vers le point aléatoire
            Noeud nouveau = creerNoeudVers(plusProche, coordAleatoires[0], coordAleatoires[1]);

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

        return fin.getParent() != null ? fin : null;
    }

    private Noeud trouverPlusProche(int x, int y) {
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

    private Noeud creerNoeudVers(Noeud depuis, int versX, int versY) {
        double dx = versX - depuis.getX();
        double dy = versY - depuis.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= DEFAULT_MAX_DISTANCE_POINT) {
            return new Noeud(versX, versY);
        }

        int newX = (int) (depuis.getX() + (dx / distance) * DEFAULT_MAX_DISTANCE_POINT);
        int newY = (int) (depuis.getY() + (dy / distance) * DEFAULT_MAX_DISTANCE_POINT);
        return new Noeud(newX, newY);
    }

    // RRT* : Trouver le meilleur parent parmi les voisins
    private Noeud trouverMeilleurParent(Noeud nouveau) {
        Noeud meilleurParent = null;
        double meilleurCout = Double.MAX_VALUE;

        for (Noeud n : noeuds) {
            double distance = calculerDistance(n, nouveau);
            if (distance <= RAYON_RECHERCHE) {
                double coutPotentiel = n.getCout() + distance;
                if (coutPotentiel < meilleurCout) {
                    meilleurCout = coutPotentiel;
                    meilleurParent = n;
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
                    double nouveauCout = nouveau.getCout() + distance;
                    if (nouveauCout < n.getCout()) {
                        // Reconnecter n via nouveau (meilleur chemin trouvé)
                        n.setParent(nouveau);
                        n.setCout(nouveauCout);
                    }
                }
            }
        }
    }

    private int[] getCoordonneesAleatoires(int endX, int endY) {
        int randX, randY;
        if (Math.random() < 0.1) {  // 10% de chance d'aller vers la fin
            randX = endX;
            randY = endY;
        } else {
            randX = (int) (Math.random() * map.getLargeur());
            randY = (int) (Math.random() * map.getHauteur());
        }
        return new int[] {randX, randY};
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
        int[] coordCourant = courant.getCoordonnees();
        int[] coordN = n.getCoordonnees();
        return Math.sqrt(Math.pow(coordCourant[0] - coordN[0], 2) + Math.pow(coordCourant[1] - coordN[1], 2));
    }
}
