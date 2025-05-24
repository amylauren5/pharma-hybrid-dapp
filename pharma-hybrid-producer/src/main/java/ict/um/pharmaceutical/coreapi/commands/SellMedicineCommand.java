package ict.um.pharmaceutical.coreapi.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class SellMedicineCommand {
    @TargetAggregateIdentifier
    private final String batchID;
    private final String toAddress;
    private final String to;
    private final int quantity;

    public SellMedicineCommand(String batchID, String toAddress, String to, int quantity) {
        this.batchID = batchID;
        this.toAddress = toAddress;
        this.to = to;
        this.quantity = quantity;
    }

    public String getBatchID() {
        return batchID;
    }

    public String getToAddress() {
        return toAddress;
    }

    public String getTo() { return to; }

    public int getQuantity() {
        return quantity;
    }
}

