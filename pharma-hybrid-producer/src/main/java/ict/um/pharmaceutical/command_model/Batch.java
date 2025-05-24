package ict.um.pharmaceutical.command_model;

import ict.um.pharmaceutical.coreapi.commands.*;
import ict.um.pharmaceutical.coreapi.enums.State;
import ict.um.pharmaceutical.coreapi.events.*;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aggregate
public class Batch {

    private static final Logger logger = LoggerFactory.getLogger(Batch.class);

    @AggregateIdentifier
    private String batchID;
    private String manufacturerAddress;
    private String dataHash;
    private int manufacturerQuantity;
    private int distributorQuantity;
    private int quantitySoldToDistributor;
    private int quantitySoldToReceiver;
    private double distributorPrice;
    private State state;

    // Default constructor for Axon
    public Batch() {

    }

    @CommandHandler
    public Batch(UploadBatchCommand command) {
        validateUploadBatchCommand(command);
        // Apply event after validation
        AggregateLifecycle.apply(new BatchUploadedEvent(
                command.getAddress(),
                command.getBatchID(),
                command.getProductCode(),
                command.getProductName(),
                command.getProductDescription(),
                command.getExpirationDate(),
                command.getPrice(),
                command.getQuantity(),
                command.getDataHash(),
                command.getState()
        ));
    }

    private void validateUploadBatchCommand(UploadBatchCommand command) {
        if (command.getBatchID() == null || command.getBatchID().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty.");
        }
        if (command.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }

    @CommandHandler
    public void handle(SellMedicineCommand command) {
        validateSellMedicineCommand(command);
        AggregateLifecycle.apply(new MedicineSoldEvent(
                command.getBatchID(),
                command.getToAddress(),
                command.getTo(),
                command.getQuantity()
        ));
    }

    private void validateSellMedicineCommand(SellMedicineCommand command) {
        logger.info("Current state in sell medicine: {}", state);
        if (this.batchID == null) {
            throw new IllegalStateException("Batch must be uploaded before selling medicine.");
        }
        if (command.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (command.getQuantity() > this.manufacturerQuantity) {
            throw new IllegalStateException("Not enough stock to sell!");
        }
    }

    @CommandHandler
    public void handle(ReportShipmentCommand command) {
        validateReportShipmentCommand(command);
        AggregateLifecycle.apply(new ShipmentReportedEvent(
                command.getBatchID(),
                command.getToEntity(),
                getQuantitySold(command.getToEntity())
        ));
    }

    private void validateReportShipmentCommand(ReportShipmentCommand command) {
        logger.info("Current state in report shipment: {}", state);
        if (this.batchID == null) {
            throw new IllegalStateException("Batch must be uploaded before reporting shipment.");
        }
        if (!(state == State.SOLD_TO_DISTRIBUTOR || state == State.SOLD_TO_CONSUMER)) {
            throw new IllegalStateException("Batch has not been sold and cannot be shipped.");
        }
        if (getQuantitySold(command.getToEntity()) <= 0) {
            throw new IllegalStateException("Quantity sold to " + command.getToEntity() + " cannot be zero.");
        }
    }

    private int getQuantitySold(String toEntity) {
        return "Distributor".equals(toEntity) ? quantitySoldToDistributor : quantitySoldToReceiver;
    }

    @CommandHandler
    public void handle(ReportDeliveryCommand command) {
        validateReportDeliveryCommand(command);
        AggregateLifecycle.apply(new DeliveryReportedEvent(
                command.getBatchID(),
                command.getToEntity()
        ));
    }

    private void validateReportDeliveryCommand(ReportDeliveryCommand command) {
        logger.info("Current state in report delivery: {}", state);
        if (this.batchID == null) {
            throw new IllegalStateException("Batch must be uploaded before reporting delivery.");
        }
        if (state != State.SHIPPED_TO_DISTRIBUTOR && state != State.SHIPPED_TO_CONSUMER) {
            throw new IllegalStateException("Batch must be shipped before it can be delivered.");
        }
    }

    @CommandHandler
    public void handle(ReviseDistributorPriceCommand command) {
        validateReviseDistributorPriceCommand(command);
        AggregateLifecycle.apply(new DistributorPriceRevisedEvent(
                command.getBatchID(),
                command.getNewPrice()
        ));
    }

    private void validateReviseDistributorPriceCommand(ReviseDistributorPriceCommand command) {
        logger.info("Current state in revise distributor price: {}", state);
        if (this.batchID == null) {
            throw new IllegalStateException("Batch must be uploaded before revising distributor price.");
        }
        if (state != State.DELIVERED_TO_DISTRIBUTOR) {
            throw new IllegalStateException("Batch must be delivered to distributor before price can be revised.");
        }
    }

    @CommandHandler
    public void handle(CancelMedicineSaleCommand command) {
        validateCancelMedicineSaleCommand(command);
        AggregateLifecycle.apply(new MedicineSaleCancelledEvent(command.getBatchID()));
    }

    private void validateCancelMedicineSaleCommand(CancelMedicineSaleCommand command) {
        logger.info("Current state in cancel medicine sale: {}", state);
        if (this.batchID == null) {
            throw new IllegalStateException("Batch must be uploaded before cancelling medicine sale.");
        }
        if (state != State.SOLD_TO_CONSUMER) {
            throw new IllegalStateException("Batch must be sold to receiver before it can be cancelled.");
        }
    }

    @EventSourcingHandler
    public void on(BatchUploadedEvent event) {
        this.manufacturerAddress = event.getManufacturerAddress();
        this.batchID = event.getBatchID();
        this.dataHash = event.getDataHash();
        this.state = event.getState();
        this.manufacturerQuantity = event.getQuantity();
        this.distributorQuantity = 0;
        this.distributorPrice = 0;
        logger.info("Batch uploaded: {} with quantity {}", batchID, manufacturerQuantity);
    }

    @EventSourcingHandler
    public void on(MedicineSoldEvent event) {
        if(event.getToEntity().equals("Distributor")){
            this.state = State.SOLD_TO_DISTRIBUTOR;
            this.quantitySoldToDistributor += event.getQuantity();
        } else if(event.getToEntity().equals("Consumer")){
            this.state = State.SOLD_TO_CONSUMER;
            this.quantitySoldToReceiver += event.getQuantity();

        }
        logger.info("Medicine sold: {} units.", event.getQuantity());
    }

    @EventSourcingHandler
    public void on(ShipmentReportedEvent event) {
        // Update state and quantity based on the entity receiving the shipment
        if(event.getToEntity().equals("Distributor")){
            this.state = State.SHIPPED_TO_DISTRIBUTOR;
            this.manufacturerQuantity -= quantitySoldToDistributor;
        } else if(event.getToEntity().equals("Consumer")){
            this.state = State.SHIPPED_TO_CONSUMER;
            this.distributorQuantity -= quantitySoldToReceiver;
        }
        logger.info("Batch {} has been shipped to {}", this.batchID, event.getToEntity());
    }

    @EventSourcingHandler
    public void on(DeliveryReportedEvent event) {
        // Update state based on the entity receiving the delivery
        if(event.getToEntity().equals("Distributor")){
            this.state = State.DELIVERED_TO_DISTRIBUTOR;
        } else if(event.getToEntity().equals("Consumer")){
            this.state = State.DELIVERED_TO_CONSUMER;
        }
        logger.info("Batch {} has been delivered to {}", this.batchID, event.getToEntity());
    }

    @EventSourcingHandler
    public void on(MedicineSaleCancelledEvent event) {
        if(state == State.SOLD_TO_CONSUMER) {
            this.state = State.CANCELLED_BY_CONSUMER;
            quantitySoldToReceiver = 0; // Reset quantity
        }
        logger.info("Medicine sale {} has been cancelled", this.batchID);
    }

    @EventSourcingHandler
    public void on(DistributorPriceRevisedEvent event) {
        if(state == State.DELIVERED_TO_DISTRIBUTOR) {
            this.distributorPrice = event.getNewPrice();
        }
        logger.info("Distributor price of batch ID {} has been revised", this.batchID);
    }
}

