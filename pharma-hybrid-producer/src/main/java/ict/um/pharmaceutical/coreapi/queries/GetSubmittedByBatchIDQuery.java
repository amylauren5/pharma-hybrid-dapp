package ict.um.pharmaceutical.coreapi.queries;

public class GetSubmittedByBatchIDQuery {
    private final String batchID;

    public GetSubmittedByBatchIDQuery(String batchID) {
        this.batchID = batchID;
    }

    // Getter
    public String getBatchID() { return batchID; }
}
