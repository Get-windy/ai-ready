package cn.aiedge.transaction.cdc;

import cn.aiedge.transaction.service.TransactionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Default implementation of CdcEventPublisher
 * Uses internal messaging or external message queue for publishing events
 */
@Slf4j
public class DefaultCdcEventPublisher implements CdcEventPublisher {
    
    @Autowired(required = false)
    private TransactionManager transactionManager;
    
    private EventSerializer eventSerializer;
    
    private boolean initialized = false;
    
    public DefaultCdcEventPublisher() {
        // Default constructor
    }
    
    @Override
    public void publishEvent(DatabaseChangeEvent event) {
        if (!initialized) {
            initialize();
        }
        
        try {
            String serializedEvent = eventSerializer.serialize(event);
            log.debug("Publishing CDC event: {}", event.getEventId());
            
            // In a real implementation, this would publish to Kafka, RabbitMQ, or similar
            // For now, we'll simulate the publishing by logging
            log.info("Published CDC event: {} | Operation: {} | Table: {} | Source: {}", 
                    event.getEventId(), 
                    event.getOperationType(), 
                    event.getTableName(), 
                    event.getSource());
            
            // Trigger any registered listeners
            notifyListeners(event);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize database change event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to serialize event", e);
        } catch (Exception e) {
            log.error("Failed to publish database change event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
    
    @Override
    public void publishEvents(List<DatabaseChangeEvent> events) {
        if (!initialized) {
            initialize();
        }
        
        log.debug("Publishing batch of {} CDC events", events.size());
        
        for (DatabaseChangeEvent event : events) {
            publishEvent(event);
        }
    }
    
    @Override
    public void publishSerializedEvent(String serializedEvent, String eventType) {
        if (!initialized) {
            initialize();
        }
        
        log.info("Publishing pre-serialized CDC event of type: {}", eventType);
        // In a real implementation, this would publish to the message queue directly
        log.debug("Serialized event content: {}", serializedEvent);
    }
    
    @Override
    public void initialize() {
        this.eventSerializer = new EventSerializer(); // fallback if not injected
        this.initialized = true;
        log.info("DefaultCdcEventPublisher initialized");
    }
    
    @Override
    public void close() {
        log.info("DefaultCdcEventPublisher closed");
        this.initialized = false;
    }
    
    /**
     * Notify any registered listeners about the event
     * @param event the event to notify about
     */
    private void notifyListeners(DatabaseChangeEvent event) {
        // In a real implementation, this would notify registered listeners
        // For example, it might trigger transaction coordination logic
        if (transactionManager != null) {
            // Process transaction-related logic if needed
            log.debug("Notifying transaction manager about CDC event: {}", event.getEventId());
        }
    }
}