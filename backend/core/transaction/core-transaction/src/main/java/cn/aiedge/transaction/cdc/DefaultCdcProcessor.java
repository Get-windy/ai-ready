package cn.aiedge.transaction.cdc;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default implementation of CdcProcessor
 * Processes database change events and manages their lifecycle
 */
@Slf4j
public class DefaultCdcProcessor implements CdcProcessor {
    
    private final CdcEventPublisher eventPublisher;
    private final EventSerializer eventSerializer;
    private ExecutorService executorService;
    private boolean initialized = false;
    
    public DefaultCdcProcessor(CdcEventPublisher eventPublisher, EventSerializer eventSerializer) {
        this.eventPublisher = eventPublisher;
        this.eventSerializer = eventSerializer;
    }
    
    @Override
    public void processEvent(DatabaseChangeEvent event) {
        if (!initialized) {
            initialize();
        }
        
        log.debug("Processing CDC event: {} | Operation: {} | Table: {}", 
                event.getEventId(), 
                event.getOperationType(), 
                event.getTableName());
        
        try {
            // Validate the event
            validateEvent(event);
            
            // Enrich the event if needed
            enrichEvent(event);
            
            // Publish the event
            eventPublisher.publishEvent(event);
            
            log.info("Successfully processed CDC event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error processing CDC event: {}", event.getEventId(), e);
            handleEventProcessingError(event, e);
        }
    }
    
    @Override
    public void processEvents(List<DatabaseChangeEvent> events) {
        if (!initialized) {
            initialize();
        }
        
        log.debug("Processing batch of {} CDC events", events.size());
        
        // Process events in parallel for better performance
        List<CompletableFuture<Void>> futures = events.stream()
                .map(event -> CompletableFuture.runAsync(() -> processEvent(event), executorService))
                .toList();
        
        // Wait for all events to be processed
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        log.info("Successfully processed batch of {} CDC events", events.size());
    }
    
    @Override
    public void initialize() {
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "cdc-processor-thread");
                t.setDaemon(true);
                return t;
            }
        );
        
        if (eventPublisher != null) {
            eventPublisher.initialize();
        }
        
        this.initialized = true;
        log.info("DefaultCdcProcessor initialized with {} threads", 
                Runtime.getRuntime().availableProcessors());
    }
    
    @Override
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (eventPublisher != null) {
            eventPublisher.close();
        }
        
        this.initialized = false;
        log.info("DefaultCdcProcessor cleaned up");
    }
    
    @Override
    public String getName() {
        return "DefaultCdcProcessor";
    }
    
    /**
     * Validate the database change event
     * @param event the event to validate
     */
    private void validateEvent(DatabaseChangeEvent event) {
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        
        if (event.getOperationType() == null) {
            throw new IllegalArgumentException("Operation type cannot be null");
        }
        
        if (event.getTableName() == null || event.getTableName().trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
    }
    
    /**
     * Enrich the event with additional information if needed
     * @param event the event to enrich
     */
    private void enrichEvent(DatabaseChangeEvent event) {
        // Add any missing contextual information
        if (event.getSource() == null) {
            event.setSource("core-transaction-cdc");
        }
        
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            event.setEventId(java.util.UUID.randomUUID().toString());
        }
    }
    
    /**
     * Handle errors during event processing
     * @param event the event that failed to process
     * @param error the error that occurred
     */
    private void handleEventProcessingError(DatabaseChangeEvent event, Exception error) {
        log.error("Error processing event {}: {}", event.getEventId(), error.getMessage());
        
        // In a real implementation, you might want to:
        // - Store the failed event in a dead letter queue
        // - Retry the event processing
        // - Alert monitoring systems
        // - Log detailed error information
        
        // For now, we'll just log the error
        log.error("Event processing failed for event: {}", event.getEventId(), error);
    }
}