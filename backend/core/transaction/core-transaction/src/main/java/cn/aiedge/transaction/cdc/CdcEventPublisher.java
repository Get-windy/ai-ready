package cn.aiedge.transaction.cdc;

/**
 * Interface for publishing CDC events to a message broker or queue
 */
public interface CdcEventPublisher {
    
    /**
     * Publish a single database change event
     * @param event the event to publish
     */
    void publishEvent(DatabaseChangeEvent event);
    
    /**
     * Publish a batch of database change events
     * @param events the events to publish
     */
    void publishEvents(java.util.List<DatabaseChangeEvent> events);
    
    /**
     * Publish a serialized database change event
     * @param serializedEvent the serialized event to publish
     * @param eventType the type of event
     */
    void publishSerializedEvent(String serializedEvent, String eventType);
    
    /**
     * Initialize the publisher
     */
    void initialize();
    
    /**
     * Close and clean up resources
     */
    void close();
}