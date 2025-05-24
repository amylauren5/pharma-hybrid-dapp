package ict.um.pharmaceutical.services;

import ict.um.pharmaceutical.coreapi.commands.UploadBatchCommand;
import ict.um.pharmaceutical.query_model.pharma_submitted.PharmaSubmittedView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class HashingService {

    private static final Logger logger = LoggerFactory.getLogger(HashingService.class);
    private final BlockchainReadService blockchainReadService;

    @Autowired
    public HashingService(BlockchainReadService blockchainReadService) {
        this.blockchainReadService = blockchainReadService;
    }

    // Function to be used by aggregate
    public String computeInitialDataHash(UploadBatchCommand command) {
        String productDetails = command.getBatchID() +
                command.getProductCode() +
                command.getProductName() +
                (command.getProductDescription() != null ? command.getProductDescription() : "") +
                command.getExpirationDate() +
                command.getPrice();

        return hashString(productDetails);
    }

    // Function to be used by projector
    public String reconstructDataHash(PharmaSubmittedView pharmaSubmittedView) {
        String data = pharmaSubmittedView.getBatchID() +
                pharmaSubmittedView.getProductCode() +
                pharmaSubmittedView.getProductName() +
                (pharmaSubmittedView.getProductDescription() != null ? pharmaSubmittedView.getProductDescription() : "") +
                pharmaSubmittedView.getExpirationDate() +
                pharmaSubmittedView.getManufacturerPrice();

        return hashString(data);
    }

    // Function to be used by the controller
    public CompletableFuture<Map<String, Map<String, Boolean>>> verifyDataHash(PharmaSubmittedView pharmaSubmittedView) {
        // Reconstructed data hash
        String reconstructedDataHash = reconstructDataHash(pharmaSubmittedView);

        // Fetch data hash from blockchain for the batch ID
        return blockchainReadService.getDataHashFromBlockchain(pharmaSubmittedView.getBatchID())
                .thenApply(blockchainHash -> {
                    Map<String, Map<String, Boolean>> result = new HashMap<>();
                    Map<String, Boolean> innerResult = new HashMap<>();

                    if (blockchainHash == null || blockchainHash.isEmpty()) {
                        logger.warn("Blockchain hash is null or empty for batch ID: {}", pharmaSubmittedView.getBatchID());
                        innerResult.put("Error: Blockchain hash is empty", false);
                    } else {
                        boolean isMatched = blockchainHash.equals(reconstructedDataHash);
                        innerResult.put(blockchainHash, isMatched);

                        if (isMatched) {
                            logger.info("Reconstructed data hash matched blockchain hash for batch ID: {}", pharmaSubmittedView.getBatchID());
                        } else {
                            logger.warn("Data hash mismatch for batch ID: {}. Reconstructed: {}, Blockchain: {}",
                                    pharmaSubmittedView.getBatchID(), reconstructedDataHash, blockchainHash);
                        }
                    }

                    result.put(reconstructedDataHash, innerResult);
                    return result;
                })
                .exceptionally(ex -> {
                    logger.error("Error fetching data hash from blockchain for batch ID: {}: {}", pharmaSubmittedView.getBatchID(), ex.getMessage());

                    Map<String, Map<String, Boolean>> errorResult = new HashMap<>();
                    Map<String, Boolean> innerError = new HashMap<>();
                    innerError.put("Error fetching blockchain data", false);
                    errorResult.put(reconstructedDataHash, innerError);

                    return errorResult;
                });
    }


    private String hashString(String input) {
        return hashBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    private String hashBytes(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
