package ict.um.pharmaceutical.coreapi.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class ReviseDistributorPriceCommand {
    @TargetAggregateIdentifier
    private final String batchID;
    private final double newPrice;

    public ReviseDistributorPriceCommand(String batchID, double newPrice) {
        this.batchID = batchID;
        this.newPrice = newPrice;
    }

    // Getters
    public String getBatchID() { return batchID; }
    public double getNewPrice() { return newPrice; }
}
