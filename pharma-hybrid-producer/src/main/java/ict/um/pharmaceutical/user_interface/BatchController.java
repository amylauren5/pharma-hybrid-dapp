package ict.um.pharmaceutical.user_interface;

import ict.um.pharmaceutical.coreapi.commands.*;
import ict.um.pharmaceutical.coreapi.queries.*;
import ict.um.pharmaceutical.query_model.pharma_submitted.PharmaSubmittedView;
import ict.um.pharmaceutical.query_model.pharma_cached.PharmaCachedView;
import ict.um.pharmaceutical.services.HashingService;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequestMapping("/batch")
@RestController
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);
    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final HashingService hashingService;

    // Constructor
    @Autowired
    public BatchController(CommandGateway commandGateway,
                           QueryGateway queryGateway,
                           HashingService hashingService) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
        this.hashingService = hashingService;
    }

    // POST Mapping to Upload a Batch of medicine
    @PostMapping("/upload/{address}/{code}/{name}/{desc}/{expr}/{price}/{quantity}")
    public CompletableFuture<ResponseEntity<Object>> uploadMedicineBatch(
            @PathVariable("address") String address,
            @PathVariable("code") String productCode,
            @PathVariable("name") String productName,
            @PathVariable("desc") String productDescription,
            @PathVariable("expr") String expirationDate,
            @PathVariable("price") Double price,
            @PathVariable("quantity") int quantity) {

        try {
            String batchID = UUID.randomUUID().toString(); // Generate batchId

            // User is valid, proceed to send the command
            UploadBatchCommand command = new UploadBatchCommand(
                    batchID, address, productCode,
                    productName, productDescription,
                    expirationDate, price, quantity
            );

            // Compute and set data hash
            String dataHash = hashingService.computeInitialDataHash(command);
            command.setDataHash(dataHash);

            // Send the command and return the batchId in the response
            return commandGateway.send(command)
                    .thenApply(result -> new ResponseEntity<>(Map.of("batchId", batchID, "status", "Batch uploaded successfully"), HttpStatus.CREATED));

        } catch (DateTimeParseException e) {
            logger.error("Invalid date format: {}", expirationDate);
            return CompletableFuture.completedFuture(new ResponseEntity<>("Invalid date format. Expected format: dd-MM-yyyy", HttpStatus.BAD_REQUEST));
        }
    }

    // POST Mapping to allow distributor to purchase medicine from manufacturer
    @PostMapping("{batchID}/distributor/{address}/quantity/{quantity}")
    public CompletableFuture<ResponseEntity<Object>> purchaseMedicineFromManufacturer(
            @PathVariable("address") String toAddress,
            @PathVariable("batchID") String batchID,
            @PathVariable("quantity") int quantity) {

        // User is valid, proceed to send the command
        SellMedicineCommand command = new SellMedicineCommand(
                batchID, toAddress, "Distributor", quantity
        );

        // Send the command and return the batchId in the response
        return commandGateway.send(command)
                .thenApply(result -> new ResponseEntity<>(Map.of("batchId", batchID, "status", "Medicine purchased from manufacturer"), HttpStatus.OK));
    }

    // POST Mapping for manufacturer to alert distributor of shipment
    @PostMapping("/{batchID}/manufacturer/shipped")
    public CompletableFuture<ResponseEntity<Object>> reportShipmentToDistributor(
            @PathVariable("batchID") String batchID) {

        // User is valid, proceed to send the command
        ReportShipmentCommand command = new ReportShipmentCommand(batchID, "Distributor");

        // Send the command and return the batchId in the response
        return commandGateway.send(command)
                .thenApply(result -> new ResponseEntity<>(Map.of("batchId", batchID, "status", "Shipment reported to distributor"), HttpStatus.OK));
    }

    // POST Mapping for distributor to alert manufacturer of delivery
    @PostMapping("/{batchID}/distributor/delivered")
    public CompletableFuture<ResponseEntity<Object>> reportDeliveryToManufacturer(
            @PathVariable("batchID") String batchID) {

        // User is valid, proceed to send the command
        ReportDeliveryCommand command = new ReportDeliveryCommand(batchID, "Distributor");

        // Send the command and return the batchId in the response
        return commandGateway.send(command)
                .thenApply(result -> new ResponseEntity<>(Map.of("batchId", batchID, "status", "Delivery reported to manufacturer"), HttpStatus.OK));
    }

    // POST Mapping for distributor to revise batch price
    @PostMapping("/{batchID}/distributor/price/{price}")
    public CompletableFuture<ResponseEntity<Object>> reviseDistributorPrice(
            @PathVariable("batchID") String batchID,
            @PathVariable("price") double price) {

        // User is valid, proceed to send the command
        ReviseDistributorPriceCommand command = new ReviseDistributorPriceCommand(batchID, price);

        // Send the command and return the batchId in the response
        return commandGateway.send(command)
                .thenApply(result -> new ResponseEntity<>(Map.of("batchId", batchID, "status", "Price revised by distributor"), HttpStatus.OK));
    }

    // POST Mapping to allow consumer to purchase medicine from distributor
    @PostMapping("{batchID}/consumer/{address}/quantity/{quantity}")
    public CompletableFuture<ResponseEntity<Object>> purchaseMedicineFromDistributor(
            @PathVariable("address") String toAddress,
            @PathVariable("batchID") String batchID,
            @PathVariable("quantity") int quantity) {

        // User is valid, proceed to send the command
        SellMedicineCommand command = new SellMedicineCommand(
                batchID, toAddress, "Consumer", quantity
        );

        // Send the command and return the batchId in the response
        return commandGateway.send(command)
                .thenApply(result -> new ResponseEntity<>(Map.of("batchId", batchID, "status", "Medicine purchased from distributor"), HttpStatus.OK));
    }

    // POST Mapping for distributor to alert receiver of shipment
    @PostMapping("/{batchID}/distributor/shipped")
    public CompletableFuture<ResponseEntity<Object>> reportShipmentToConsumer(
            @PathVariable("batchID") String batchID) {

        // User is valid, proceed to send the command
        ReportShipmentCommand command = new ReportShipmentCommand(batchID, "Consumer");

        // Send the command and return the batchId in the response
        return commandGateway.send(command)
                .thenApply(result -> new ResponseEntity<>(Map.of("batchId", batchID, "status", "Shipment reported to consumer"), HttpStatus.OK));
    }

    // POST Mapping for consumer to alert distributor of delivery
    @PostMapping("/{batchID}/consumer/delivered")
    public CompletableFuture<ResponseEntity<Object>> reportDeliveryToDistributor(
            @PathVariable("batchID") String batchID) {

        // User is valid, proceed to send the command
        ReportDeliveryCommand command = new ReportDeliveryCommand(
                batchID, "Consumer"
        );

        // Send the command and return the batchId in the response
        return commandGateway.send(command)
                .thenApply(result -> new ResponseEntity<>(Map.of("batchId", batchID, "status", "Delivery reported to distributor"), HttpStatus.OK));
    }

    // POST Mapping to cancel medicine sale upon failed shipment
    @PostMapping("/{batchID}/distributor/cancelled")
    public CompletableFuture<ResponseEntity<Object>> cancelMedicineSaleToReceiver(
            @PathVariable("batchID") String batchID) {

        // User is valid, proceed to send the command
        CancelMedicineSaleCommand command = new CancelMedicineSaleCommand(batchID);

        // Send the command and return the batchId in the response
        return commandGateway.send(command)
                .thenApply(result -> new ResponseEntity<>(Map.of("batchId", batchID, "status", "Medicine sale cancelled"), HttpStatus.OK));
    }

    @GetMapping("/{batchID}")
    public CompletableFuture<ResponseEntity<?>> getBatchDetails(@PathVariable("batchID") String batchID) {
        return queryGateway.query(
                new GetCacheByBatchIDQuery(batchID),
                ResponseTypes.instanceOf(PharmaSubmittedView.class)  // Expect a raw Map, not a DTO
        ).thenApply(response -> {
            if (response == null) {
                logger.warn("No batch found for batchID: {}", batchID);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            logger.info("Batch details received: {}", response);
            return new ResponseEntity<>(
                    Map.of("code", response.getProductCode(),
                            "name", response.getProductName(),
                            "desc", response.getProductDescription(),
                            "price", response.getManufacturerPrice(),
                            "quantity", response.getQuantity()
                    ),
                    HttpStatus.OK);
        }).exceptionally(ex -> {
            logger.error("Error fetching batch details for batch ID {}: {}", batchID, ex.getMessage());
            return new ResponseEntity<>(Map.of("error", "Error during retrieval"), HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }

    @GetMapping("/{batchID}/final")
    public CompletableFuture<ResponseEntity<?>> getSubmitted(@PathVariable("batchID") String batchID) {
        return queryGateway.query(
                new GetSubmittedByBatchIDQuery(batchID),
                ResponseTypes.instanceOf(PharmaCachedView.class)  // Expect a raw Map, not a DTO
        ).thenApply(response -> {
            if (response.getBatchID() == null) {
                logger.warn("No submitted entry found for batchID: {}", batchID);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            logger.info("Submitted details received: {}", response);
            return new ResponseEntity<>(
                    Map.of("batchID", response.getBatchID(),
                            "fromEntity", response.getFromEntity(),
                            "toEntity", response.getToEntity() == null ? "No Entity" : response.getToEntity(),
                            "finalityTimestamp", response.getFinalityTimestamp()

                    ),
                    HttpStatus.OK);
        }).exceptionally(ex -> {
            logger.error("Error fetching submitted entry for batch ID {}: {}", batchID, ex.getMessage());
            return new ResponseEntity<>(Map.of("error", "Error during retrieval"), HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }

    // Get specific batch state
    @GetMapping("/{batchID}/states")
    public CompletableFuture<ResponseEntity<?>> getBatchStates(@PathVariable("batchID") String batchID) {
        return queryGateway.query(
                new GetCachedBatchStatesQuery(batchID), ResponseTypes.instanceOf(String.class)
        ).thenApply(result -> {
            if (result == null) {
                logger.warn("No states found for batch ID: {}", batchID);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            logger.info("Batch states found for batch ID: {}", batchID);
            return new ResponseEntity<>(result, HttpStatus.OK); // Return the state wrapped in a ResponseEntity
        }).exceptionally(ex -> {
            logger.error("Error fetching batch state for batch ID {}: {}", batchID, ex.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Return internal server error if exception occurs
        });
    }

    @GetMapping("/{batchID}/verify-hash")
    public CompletableFuture<ResponseEntity<?>> verifyDataHash(@PathVariable("batchID") String batchID) {
        return queryGateway.query(
                new VerifyDataHashQuery(batchID),
                ResponseTypes.instanceOf(String.class) // Expect a raw Map response
        ).thenApply(response -> {
            logger.info("Hash verification result received: {}", response);
            return new ResponseEntity<>(
                    Map.of("status", response),
                    HttpStatus.OK);
        });
    }

    @GetMapping("/{batchID}/verify-state")
    public CompletableFuture<ResponseEntity<?>> verifyLatestState(@PathVariable("batchID") String batchID) {
        return queryGateway.query(
                new VerifyLatestStateQuery(batchID),
                ResponseTypes.instanceOf(String.class) // Expect a raw Map response
        ).thenApply(response -> {
            logger.info("Latest state verification result received: {}", response);
            return new ResponseEntity<>(
                    Map.of("status", response),
                    HttpStatus.OK);
        });
    }

    @GetMapping("/{batchID}/verify-states")
    public CompletableFuture<ResponseEntity<?>> verifyAllStates(@PathVariable("batchID") String batchID) {
        return queryGateway.query(
                new VerifyAllStatesQuery(batchID),
                ResponseTypes.instanceOf(String.class) // Expect a raw Map response
        ).thenApply(response -> {
            logger.info("All states verification result received: {}", response);
            return new ResponseEntity<>(
                    Map.of("status", response),
                    HttpStatus.OK);
        });
    }
}
