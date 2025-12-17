package Reseau;

/**
 * Classe représentant les informations d'un pair dans le réseau P2P
 * Stocke l'identifiant, l'adresse IP et le port d'un nœud P2P.
 * Permet la sérialisation et désérialisation au format "peerId@host:port"
 */
public class PeerInfo {
    private final String peerId;
    private final String host;
    private final int port;

    /**
     * Constructeur de PeerInfo
     *
     * @param peerId Identifiant unique du pair (ex: "J1", "J2")
     * @param host   Adresse IP du pair (ex: "localhost", "192.168.1.10")
     * @param port   Port d'écoute du pair (ex: 5001)
     */
    public PeerInfo(String peerId, String host, int port) {
        this.peerId = peerId;
        this.host = host;
        this.port = port;
    }

    /**
     * Obtenir l'identifiant du pair
     *
     * @return Identifiant du pair
     */
    public String getPeerId() {
        return peerId;
    }

    /**
     * Obtenir l'adresse IP du pair
     *
     * @return Adresse IP du pair
     */
    public String getHost() {
        return host;
    }

    /**
     * Obtenir le port du pair
     *
     * @return Port du pair
     */
    public int getPort() {
        return port;
    }

    /**
     * Convertir en chaîne de caractères au format "peerId@host:port"
     *
     * @return Représentation textuelle du pair
     */
    @Override
    public String toString() {
        return peerId + "@" + host + ":" + port;
    }

    /**
     * Créer un objet PeerInfo à partir d'une chaîne au format "peerId@host:port"
     *
     * @param s Chaîne à parser
     * @return Objet PeerInfo ou null si le format est invalide
     */
    public static PeerInfo fromString(String s) {
        try {
            String[] parts = s.split("@");
            if (parts.length != 2) return null;

            String peerId = parts[0];
            String[] hostPort = parts[1].split(":");
            if (hostPort.length != 2) return null;

            String host = hostPort[0];
            int port = Integer.parseInt(hostPort[1]);

            return new PeerInfo(peerId, host, port);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Vérifier l'égalité entre deux PeerInfo
     *
     * @param obj Objet à comparer
     * @return true si les PeerInfo sont identiques
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PeerInfo peerInfo = (PeerInfo) obj;
        return port == peerInfo.port &&
               peerId.equals(peerInfo.peerId) &&
               host.equals(peerInfo.host);
    }

    /**
     * Calculer le hashCode du PeerInfo
     *
     * @return Hash code
     */
    @Override
    public int hashCode() {
        int result = peerId.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + port;
        return result;
    }
}

