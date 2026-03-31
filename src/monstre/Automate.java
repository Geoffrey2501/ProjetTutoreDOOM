package monstre;

public class Automate{

    public enum Etat {
        ATTENTE,
        PATROUILLE,
        POURSUITE
    }

    private Etat etatActuel = Etat.ATTENTE;

    private long tempsAttente = 0;
    private long delaiAttente = 2000;
    private long dernierTemps = System.currentTimeMillis();

    private double lastTargetX = -1;
    private double lastTargetY = -1;
    private final double SEUIL_MOUVEMENT = 10.0; // Distance minimum pour justifier un recalcul

    private final Monstre monstre;

    public  Automate(Monstre monstre) {
        this.monstre = monstre;
    }

    public void update() {
        long tempsActuel = System.currentTimeMillis();
        long deltaTemps = tempsActuel - dernierTemps;
        dernierTemps = tempsActuel;

        switch (etatActuel) {
            case ATTENTE:
                updateAttente(deltaTemps);
                break;

            case PATROUILLE:
                updatePatrouille();
                break;

            case POURSUITE:
                updatePoursuite();
                break;
        }
    }

    private void updateAttente(long deltaTemps) {
        tempsAttente += deltaTemps;

        if (monstre.cibleDetectee()) {
            transitionVers(Etat.POURSUITE);
            return;
        }

        if (tempsAttente >= delaiAttente) {
            transitionVers(Etat.PATROUILLE);
        }
    }

    private void updatePatrouille() {
        if (monstre.cibleDetectee()) {
            transitionVers(Etat.POURSUITE);
            return;
        }

        monstre.updatePatrouille(); // ta méthode existante
    }

    private void updatePoursuite() {
        if (!monstre.cibleDetectee()) {
            transitionVers(Etat.ATTENTE);
            return;
        }

        monstre.updatePoursuite(); // ta méthode existante
    }

    private void transitionVers(Etat nouvelEtat) {
        if (etatActuel == nouvelEtat) return;

        System.out.println("[Automate] Transition: " + etatActuel + " -> " + nouvelEtat);

        etatActuel = nouvelEtat;
        tempsAttente = 0;

        // Actions d'entrée simples sans créer de nouvelles méthodes
        if (nouvelEtat == Etat.PATROUILLE) {
            monstre.reprendrePatrouille(); // tu peux déplacer ton code existant ici
        }

        if (nouvelEtat == Etat.ATTENTE) {
            monstre.resetSteering();
        }

        if (nouvelEtat == Etat.POURSUITE) {
            monstre.resetSteering();
        }
    }

    public Etat getEtat() {
        return etatActuel;
    }
}
