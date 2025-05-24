package ict.um.pharmaceutical.coreapi.queries;

public class VerifyDataHashQuery {
    private final String batchID;

    public VerifyDataHashQuery(String batchID) {
        this.batchID = batchID;
    }

    // Getter
    public String getBatchID() { return batchID; }
}
