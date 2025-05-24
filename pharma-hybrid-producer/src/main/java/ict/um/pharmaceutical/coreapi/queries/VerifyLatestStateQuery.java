package ict.um.pharmaceutical.coreapi.queries;

public class VerifyLatestStateQuery {
    private final String batchID;

    public VerifyLatestStateQuery(String batchID) {
        this.batchID = batchID;
    }

    // Getter
    public String getBatchID() { return batchID; }
}
