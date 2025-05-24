package ict.um.pharmaceutical.coreapi.commands;

import ict.um.pharmaceutical.coreapi.enums.State;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class UploadBatchCommand {
    @TargetAggregateIdentifier
    private final String batchID;
    private final String address;
    private final String productCode;
    private final String productName;
    private final String productDescription;
    private final String expirationDate;
    private final double price;
    private final int quantity;
    private final State state;
    private String dataHash;

    public UploadBatchCommand(String batchId, String address, String productCode,
                              String productName, String productDescription,
                              String expirationDate, double price, int quantity) {
        this.batchID = batchId;
        this.address = address;
        this.productCode = productCode;
        this.productName = productName;
        this.productDescription = productDescription;
        this.expirationDate = expirationDate;
        this.price = price;
        this.quantity = quantity;
        this.state = State.MANUFACTURED;
        this.dataHash = null;
    }

    // Getters
    public String getBatchID() { return batchID; }
    public String getAddress() { return address; }
    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public String getProductDescription() { return productDescription; }
    public String getExpirationDate() { return expirationDate; }
    public double getPrice() { return price; }
    public State getState() { return state; }
    public int getQuantity() { return quantity; }
    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }
}
