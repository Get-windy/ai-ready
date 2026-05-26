package cn.aiedge.transaction.cdc;

import java.util.List;

/**
 * Interface for Change Data Capture Processor
 * Handles database change events and processes them according to business logic
 */
public interface CdcProcessor {
    
    /**
     * Process a single database change event
     * @param event the database change event to process
     */
    void processEvent(DatabaseChangeEvent event);
    
    /**
     * Process a batch of database change events
     * @param events list of database change events to process
     */
    void processEvents(List<DatabaseChangeEvent> events);
    
    /**
     * Initialize the CDC processor
     */
    void initialize();
    
    /**
     * Clean up resources when shutting down
     */
    void cleanup();
    
    /**
     * Get the name of this processor
     * @return processor name
     */
    String getName();
}