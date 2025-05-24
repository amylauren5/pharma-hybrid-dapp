package ict.um.pharmaceutical.coreapi.queries;

public class VerifyAllStatesQuery {
    private final String batchID;

    public VerifyAllStatesQuery(String batchID) {
        this.batchID = batchID;
    }

    // Getter
    public String getBatchID() { return batchID; }
}
