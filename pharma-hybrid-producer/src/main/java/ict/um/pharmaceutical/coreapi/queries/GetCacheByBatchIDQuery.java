package ict.um.pharmaceutical.coreapi.queries;

public class GetCacheByBatchIDQuery {
    private final String batchID;

    public GetCacheByBatchIDQuery(String batchID) {
        this.batchID = batchID;
    }

    // Getter
    public String getBatchID() { return batchID; }
}
