package ict.um.pharmaceutical.coreapi.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class ReportDeliveryCommand {
    @TargetAggregateIdentifier
    private final String batchID;
    private final String toEntity;

    public ReportDeliveryCommand(String batchID, String toEntity) {
        this.batchID = batchID;
        this.toEntity = toEntity;
    }

    // Getters
    public String getBatchID() { return batchID; }
    public String getToEntity() { return toEntity; }
}

