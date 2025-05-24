package ict.um.pharmaceutical.services;

import ict.um.pharmaceutical.web3j_wrappers.PharmaTrackingContract;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class BlockchainReadService {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainReadService.class);
    private final Web3j web3j;
    private final Credentials credentials;
    private PharmaTrackingContract pharmaTrackingContract;
    private final String contractAddress;

    @Autowired
    public BlockchainReadService(@Value("${web3.provider}") String web3Provider,
                                 @Value("${private.key}") String privateKey,
                                 @Value("${contract.address}") String contractAddress) {

        if (privateKey == null || privateKey.isEmpty()) {
            throw new IllegalArgumentException("Private key is not set. Please check your application.properties.");
        }

        this.web3j = Web3j.build(new HttpService(web3Provider));
        this.credentials = Credentials.create(privateKey);
        this.contractAddress = contractAddress;
    }

    @PostConstruct
    public void initContract() {
        try {
            // Validate contract address
            if (!WalletUtils.isValidAddress(contractAddress)) {
                throw new IllegalArgumentException("Invalid contract address.");
            }

            logger.info("Contract address: " + contractAddress);

            BigInteger balance = web3j.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).send().getBalance();
            if (balance.compareTo(BigInteger.ZERO) <= 0) {
                logger.warn("The wallet {} has zero ETH balance. Transactions may fail.", credentials.getAddress());
            }

            // Load contract
            this.pharmaTrackingContract = PharmaTrackingContract.load(
                    contractAddress, web3j, credentials, new DefaultGasProvider());

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize contract: ", e);
        }
    }


    public CompletableFuture<String> getBatchFromBlockchain(String batchID) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pharmaTrackingContract.call_getBatchTracking(batchID)
                .sendAsync()
                .thenAccept(trackingStruct -> future.complete(String.valueOf(trackingStruct)))
                .exceptionally(ex -> {
                    future.completeExceptionally(ex);
                    return null;
                });
        return future;
    }

    public CompletableFuture<String> getDataHashFromBlockchain(String batchID) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pharmaTrackingContract.call_getDataHash(batchID)
                .sendAsync()
                .thenAccept(future::complete)
                .exceptionally(ex -> {
                    future.completeExceptionally(ex);
                    return null;
                });
        return future;
    }

    public CompletableFuture<BigInteger> getLatestStateFromBlockchain(String batchID) {
        return pharmaTrackingContract.call_getLatestBatchState(batchID).sendAsync();
    }

    public CompletableFuture<List> getAllStatesFromBlockchain(String batchID) {
        return pharmaTrackingContract.call_getAllBatchStates(batchID).sendAsync();
    }
}

