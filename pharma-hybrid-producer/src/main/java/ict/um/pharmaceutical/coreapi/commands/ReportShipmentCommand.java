package ict.um.pharmaceutical.coreapi.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class ReportShipmentCommand {
    @TargetAggregateIdentifier
    private final String batchID;
    private final String toEntity;

    public ReportShipmentCommand(String batchID, String toEntity) {
        this.batchID = batchID;
        this.toEntity = toEntity;
    }

    public String getBatchID() {
        return batchID;
    }

    public String getToEntity() {
        return toEntity;
    }
}
