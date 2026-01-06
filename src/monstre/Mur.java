package monstre;

public class Mur {
    public int x1, y1, x2, y2;

    public Mur(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public boolean esDansMur(int x, int y) {
        return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) &&
                y >= Math.min(y1, y2) && y <= Math.max(y1, y2);
    }

    public boolean esDansMurSegment(int x1, int y1, int x2, int y2) {

        int[] vecteurSeg = {x2 - x1, y2 - y1};

        int[] vecteurMur = {this.x2 - this.x1, this.y2 - this.y1};

        //calcul du déterminant
        if (!(Math.max(x1, x2) < Math.min(this.x1, this.x2) || Math.max(this.x1, this.x2) < Math.min(x1, x2) ||
                Math.max(y1, y2) < Math.min(this.y1, this.y2) || Math.max(this.y1, this.y2) < Math.min(y1, y2))) {
            int det = vecteurSeg[0] * vecteurMur[1] - vecteurSeg[1] * vecteurMur[0];
            if (det != 0) {
                //calcul des paramètres t et u
                double t = ((x1 - this.x1) * vecteurMur[1] - (y1 - this.y1) * vecteurMur[0]) / (double) det;
                double u = ((x1 - this.x1) * vecteurSeg[1] - (y1 - this.y1) * vecteurSeg[0]) / (double) det;

                //vérification de l'intersection
                if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
                    return true; //les segments se croisent
                }
            }


        }
        return false;
    }
}