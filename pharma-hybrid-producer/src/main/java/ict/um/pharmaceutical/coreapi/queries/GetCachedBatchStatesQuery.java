package ict.um.pharmaceutical.coreapi.queries;

public class GetCachedBatchStatesQuery {
    private final String batchID;

    public GetCachedBatchStatesQuery(String batchID) {
        this.batchID = batchID;
    }

    // Getter
    public String getBatchID() { return batchID; }
}

