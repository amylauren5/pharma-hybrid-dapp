package ict.um.pharmaceutical.query_model.pharma_submitted;

import ict.um.pharmaceutical.coreapi.enums.State;
import ict.um.pharmaceutical.coreapi.events.*;
import ict.um.pharmaceutical.coreapi.queries.*;
import ict.um.pharmaceutical.services.HashingService;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ict.um.pharmaceutical.services.BlockchainReadService;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class PharmaSubmittedProjector {

    private static final Logger logger = LoggerFactory.getLogger(PharmaSubmittedProjector.class);
    private final PharmaSubmittedViewRepository pharmaSubmittedViewRepository;

    // For direct queries and verification from the blockchain
    private final BlockchainReadService blockchainReadService;
    private final HashingService hashingService;

    // State mapping to simplify setting the state based on toEntity
    private static final Map<String, State> TO_ENTITY_STATE_MAP = Map.of(
            "Distributor", State.SOLD_TO_DISTRIBUTOR,
            "Consumer", State.SOLD_TO_CONSUMER
    );

    // Constructor to set up
    @Autowired
    public PharmaSubmittedProjector(PharmaSubmittedViewRepository pharmaSubmittedViewRepository,
                                    BlockchainReadService blockchainReadService,
                                    HashingService hashingService) {
        this.pharmaSubmittedViewRepository = pharmaSubmittedViewRepository;
        this.blockchainReadService = blockchainReadService;
        this.hashingService = hashingService;
    }

    // METHOD TO SAVE TO REPOSITORY
    private void savePharmaSubmittedView(PharmaSubmittedView view) {
        try {
            pharmaSubmittedViewRepository.save(view);
            logger.info("PharmaCachedView saved for batch ID: {}", view.getBatchID());
        } catch (Exception e) {
            logger.error("Failed to save PharmaCachedView for batch ID: {} due to {}", view.getBatchID(), e.getMessage(), e);
        }
    }

    @EventHandler
    public void on(BatchUploadedEvent event) {
        logger.info("Processing BatchUploadedEvent: {}", event);

        // Get unix timestamp
        long unixTimestamp = Instant.now().toEpochMilli();

        List<BigInteger> states = new ArrayList<>();
        states.add(event.getState().getValue());

        // Save PharmaCachedView entry immediately (assuming transaction will be mined)
        PharmaSubmittedView pharmaSubmittedView = new PharmaSubmittedView(
                new ArrayList<String>(),
                unixTimestamp,
                "Manufacturer",
                null,
                event.getBatchID(),
                event.getProductCode(),
                event.getProductName(),
                event.getProductDescription(),
                event.getExpirationDate(),
                event.getPrice(),
                0.0,
                event.getQuantity(),
                event.getManufacturerAddress(),
                null,
                null,
                states
        );

        // Save to repository
        logger.info("Saving PharmaCachedView: batchId={}, fromEntity={}", pharmaSubmittedView.getBatchID(), pharmaSubmittedView.getFromEntity());
        savePharmaSubmittedView(pharmaSubmittedView);
    }

    @EventHandler
    public void on(MedicineSoldEvent event) {
        logger.info("Processing MedicineSoldEvent: {}", event);
        Optional<PharmaSubmittedView> pharmaCachedViewOpt = pharmaSubmittedViewRepository.findById(event.getBatchID());

        pharmaCachedViewOpt.ifPresent(pharmaSubmittedView -> {
            String to = event.getToEntity();
            State newState = TO_ENTITY_STATE_MAP.get(to);
            if (newState == null) {
                logger.warn("toEntity is invalid in PharmaCachedView: {}", to);
                return; // Early exit to prevent further processing
            }
            // Update current state in database
            pharmaSubmittedView.addState(newState);
            pharmaSubmittedView.setToEntity(event.getToEntity());
            if(event.getToEntity().equals("Distributor")) {
                pharmaSubmittedView.setDistributorPrice(pharmaSubmittedView.getManufacturerPrice());
                pharmaSubmittedView.setDistributorAddress(event.getToAddress());
            } else if(event.getToEntity().equals("Receiver")) {
                pharmaSubmittedView.setFromEntity("Distributor");
                pharmaSubmittedView.setReceiverAddress(event.getToAddress());
            }
            logger.info("Updating PharmaCachedView: batchID={}, state={}", pharmaSubmittedView.getBatchID(), pharmaSubmittedView.getStates());
            savePharmaSubmittedView(pharmaSubmittedView);
        });
    }

    @EventHandler
    public void on(ShipmentReportedEvent event) {
        logger.info("Processing ShipmentReportedEvent: {}", event);
        PharmaSubmittedView pharmaSubmittedView = pharmaSubmittedViewRepository.findById(event.getBatchID()).orElse(null);
        if (pharmaSubmittedView != null) {
            // Update state
            if ("Distributor".equalsIgnoreCase(event.getToEntity())) {
                pharmaSubmittedView.addState(State.SHIPPED_TO_DISTRIBUTOR);
            } else if ("Receiver".equalsIgnoreCase(event.getToEntity())) {
                pharmaSubmittedView.addState(State.SHIPPED_TO_CONSUMER);
            }
            // Update quantity
            pharmaSubmittedView.setQuantity(pharmaSubmittedView.getQuantity() - event.getQuantitySold());
            // Save to repository
            savePharmaSubmittedView(pharmaSubmittedView);
        }
    }

    @EventHandler
    public void on(DeliveryReportedEvent event) {
        logger.info("Processing DeliveryReportedEvent: {}", event);
        PharmaSubmittedView pharmaSubmittedView = pharmaSubmittedViewRepository.findById(event.getBatchID()).orElse(null);
        if (pharmaSubmittedView != null) {
            if (event.getToEntity().equalsIgnoreCase("Distributor")) {
                pharmaSubmittedView.addState(State.DELIVERED_TO_DISTRIBUTOR);
                logger.info("Batch {} delivered to Distributor", event.getBatchID());
            } else if (event.getToEntity().equalsIgnoreCase("Receiver")) {
                pharmaSubmittedView.addState(State.DELIVERED_TO_CONSUMER);
                logger.info("Batch {} delivered to Receiver (Pharmacy/Client)", event.getBatchID());
            }
            savePharmaSubmittedView(pharmaSubmittedView);
        }
    }

    @EventHandler
    public void on(DistributorPriceRevisedEvent event) {
        logger.info("Processing DistributorPriceRevisedEvent: {}", event);
        PharmaSubmittedView pharmaSubmittedView = pharmaSubmittedViewRepository.findById(event.getBatchID()).orElse(null);
        if (pharmaSubmittedView != null) {
            pharmaSubmittedView.setDistributorPrice(event.getNewPrice());
            savePharmaSubmittedView(pharmaSubmittedView);
        }
    }

    @EventHandler
    public void on(MedicineSaleCancelledEvent event) {
        logger.info("Processing MedicineSaleCancelledEvent: {}", event);
        PharmaSubmittedView pharmaSubmittedView = pharmaSubmittedViewRepository.findById(event.getBatchID()).orElse(null);
        if (pharmaSubmittedView != null) {
            pharmaSubmittedView.addState(State.CANCELLED_BY_CONSUMER);
            savePharmaSubmittedView(pharmaSubmittedView);
            logger.info("Batch {} cancelled by receiver", event.getBatchID());
        }
    }

    private PharmaSubmittedView fetchCachedView(String batchID){
        Optional<PharmaSubmittedView> cachedViewOptional = pharmaSubmittedViewRepository.findById(batchID);

        if (cachedViewOptional.isEmpty()) {
            logger.warn("No batch found for batchID: {}", batchID);
            throw new NoSuchElementException("Batch not found for ID: " + batchID);
        }

        PharmaSubmittedView cachedView = cachedViewOptional.get();
        logger.info("Batch found in cache: {}", cachedView);

        return cachedView;
    }

    @QueryHandler
    public PharmaSubmittedView handle(GetCacheByBatchIDQuery query) {
        logger.info("Handling batch details query: {}", query);
        return fetchCachedView(query.getBatchID());
    }

    @QueryHandler
    public String handle(GetCachedBatchStatesQuery query) {
        logger.info("Handling cached batch states query: {}", query);
        return fetchCachedView(query.getBatchID()).getStateString();
    }

    @QueryHandler
    public String handle(VerifyDataHashQuery query){
        logger.info("Handling verify data hash query: {}", query);

        // Fetch cached view first
        PharmaSubmittedView cachedView = fetchCachedView(query.getBatchID());

        // Perform data hash verification
        Map<String, Map<String, Boolean>> verificationResult = hashingService.verifyDataHash(cachedView).join();

        // Extract hash details
        String localHash = hashingService.reconstructDataHash(cachedView);
        String blockchainHash = verificationResult.keySet().stream().findFirst().orElse(null);
        Boolean isVerified = verificationResult.get(localHash).get(blockchainHash);

        if(blockchainHash == null){
            return "Data hash not found on blockchain";
        } else {
            if(isVerified){
                return "Data hash verified";
            } else {
                return "Data hash not verified";
            }
        }
    }

    @QueryHandler
    public CompletableFuture<String> handle(VerifyAllStatesQuery query) {
        logger.info("Handling verify all states query: {}", query);

        // Fetch cached view first
        PharmaSubmittedView cachedView = fetchCachedView(query.getBatchID());
        List<BigInteger> localStates = cachedView.getStates();

        // Check if local states are empty
        if (localStates.isEmpty()) {
            logger.warn("No local states found for batch ID: {}", query.getBatchID());
            return CompletableFuture.completedFuture("No local states found.");
        }

        return blockchainReadService.getAllStatesFromBlockchain(query.getBatchID())
                .thenApply(blockchainStates -> {
                    // Cast raw List to List<BigInteger>
                    List<BigInteger> blockchainStatesList;
                    try {
                        blockchainStatesList = (List<BigInteger>) blockchainStates;
                    } catch (ClassCastException e) {
                        logger.error("Error casting blockchain states to List<BigInteger> for batch ID: {}", query.getBatchID(), e);
                        throw new RuntimeException("Invalid blockchain states format.");
                    }

                    // Compare local states with blockchain states
                    boolean statesMatch = localStates.equals(blockchainStatesList);

                    if (statesMatch) {
                        logger.info("States verified for batch ID: {}", query.getBatchID());
                        return "States verified successfully.";
                    } else {
                        logger.warn("States do not match for batch ID: {}", query.getBatchID());
                        return "States verification failed.";
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Error verifying states for batch ID: {}", query.getBatchID(), ex);
                    return "An error occurred while verifying states.";
                });
    }


    @QueryHandler
    public CompletableFuture<String> handle(VerifyLatestStateQuery query) {
        logger.info("Handling verify latest state query: {}", query);

        PharmaSubmittedView cachedView = fetchCachedView(query.getBatchID());
        List<BigInteger> states = cachedView.getStates();

        if (states.isEmpty()) {
            logger.warn("No states found for cached view with batch ID: {}", query.getBatchID());
            return CompletableFuture.completedFuture("No states found for the cached view.");
        }

        BigInteger localState = states.getLast(); // Safely get last state

        return blockchainReadService.getLatestStateFromBlockchain(query.getBatchID())
                .thenApply(blockchainState -> {
                    boolean statesMatch = localState.equals(blockchainState);

                    if (statesMatch) {
                        logger.info("Latest state verified for batch ID: {}", query.getBatchID());
                        return "Latest state verified successfully.";
                    } else {
                        logger.warn("Latest states do not match for batch ID: {}", query.getBatchID());
                        return "Latest state verification failed.";
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Error verifying latest state for batch ID: {}", query.getBatchID(), ex);
                    return "An error occurred while verifying the latest state.";
                });
    }
}

