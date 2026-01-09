package moteur_graphique.BSP;

public class NoeudBSP {
    NoeudBSP gauche = null;
    NoeudBSP droit = null;
    Mur mur = null;

    public NoeudBSP(Mur mur) {
        this.mur = mur;
    }

    public Mur getMurDiviseur() {
        return mur;
    }
}
