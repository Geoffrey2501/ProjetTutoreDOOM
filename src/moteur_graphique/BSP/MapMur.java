package moteur_graphique.BSP;

public class MapMur {
    private Mur[] murs;

    public MapMur(Mur[] murs) {
        this.murs = murs;
    }

    //TODO: faire un constructeur qui charge une map à partir d'un fichier texte
    //pour cyprian ?

    public void setMurs(Mur[] murs) {
        this.murs = murs;
    }

    public Mur[] getMurs() {
        return murs;
    }
}
