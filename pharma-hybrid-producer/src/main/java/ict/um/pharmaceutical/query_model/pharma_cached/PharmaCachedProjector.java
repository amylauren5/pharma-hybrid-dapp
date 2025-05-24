package ict.um.pharmaceutical.query_model.pharma_cached;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ict.um.pharmaceutical.coreapi.events.*;
import ict.um.pharmaceutical.coreapi.queries.GetSubmittedByBatchIDQuery;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class PharmaCachedProjector {

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_SECONDS = 5; // Fixed delay between retries (5 seconds)

    private static final Logger logger = LoggerFactory.getLogger(PharmaCachedProjector.class);
    private final PharmaCachedViewRepository pharmaCachedViewRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // Constructor injection
    @Autowired
    public PharmaCachedProjector(PharmaCachedViewRepository pharmaCachedViewRepository,
                                 RabbitTemplate rabbitTemplate,
                                 ObjectMapper objectMapper) {
        this.pharmaCachedViewRepository = pharmaCachedViewRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendToBroker(Object event, String queue) throws InterruptedException {

        // Delay for 10 seconds
        logger.info("Sending to broker delayed for 10 seconds...");
        Thread.sleep(10000);

        String jsonString = null;
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                // Convert event to JSON string
                jsonString = objectMapper.writeValueAsString(event);

                // Send the message with persistence
                rabbitTemplate.convertAndSend("", queue, jsonString, new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT); // Ensure message persistence
                        return message;
                    }
                });

                logger.info("Event [{}] sent successfully to queue [{}]", event.getClass().getSimpleName(), queue);
                return; // Exit if message is sent successfully

            } catch (AmqpException | JsonProcessingException e) {
                // Log the error and retry
                logger.error("Failed to send event [{}] to queue [{}]: {}. Retrying... (Attempt {}/{}).",
                        event.getClass().getSimpleName(), queue, e.getMessage(), retryCount + 1, MAX_RETRIES);

                // Sleep before retrying
                try {
                    TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt(); // Reset the interrupt flag
                }
                retryCount++;
            }
        }

        // Log a final error after exhausting retries
        logger.error("Failed to send event [{}] to queue [{}] after {} retries.", event.getClass().getSimpleName(), queue, MAX_RETRIES);
    }

    @EventHandler
    public void on(BatchUploadedEvent event) throws InterruptedException {
        sendToBroker(event, "blockchain.upload.queue");
    }

    @EventHandler
    public void on(MedicineSoldEvent event) throws InterruptedException {
        sendToBroker(event, "blockchain.sell.queue");
    }

    @EventHandler
    public void on(ShipmentReportedEvent event) throws InterruptedException {
        sendToBroker(event, "blockchain.shipment.queue");
    }

    @EventHandler
    public void on(DeliveryReportedEvent event) throws InterruptedException {
        sendToBroker(event, "blockchain.delivery.queue");
    }

    @EventHandler
    public void on(MedicineSaleCancelledEvent event) throws InterruptedException {
        sendToBroker(event, "blockchain.cancel.queue");
    }

    private PharmaCachedView fetchCachedView(String batchID){
        Optional<PharmaCachedView> submittedViewOptional = pharmaCachedViewRepository.findById(batchID);

        if (submittedViewOptional.isEmpty()) {
            logger.warn("No batch found for batchID: {}", batchID);
            throw new NoSuchElementException("Batch not found for ID: " + batchID);
        }

        PharmaCachedView submittedView = submittedViewOptional.get();
        logger.info("Batch found in submitted: {}", submittedView);
        return submittedView;
    }

    @QueryHandler
    public PharmaCachedView handle(GetSubmittedByBatchIDQuery query) {
        return fetchCachedView(query.getBatchID());
    }
}
