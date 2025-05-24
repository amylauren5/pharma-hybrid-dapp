package ict.um.pharmaceutical.coreapi.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class CancelMedicineSaleCommand {
    @TargetAggregateIdentifier
    private final String batchID;

    public CancelMedicineSaleCommand(String batchID) {
        this.batchID = batchID;
    }

    // Getter
    public String getBatchID() { return batchID; }
}

