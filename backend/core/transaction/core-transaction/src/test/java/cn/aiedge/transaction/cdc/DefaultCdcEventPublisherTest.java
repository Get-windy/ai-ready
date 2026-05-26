package cn.aiedge.transaction.cdc;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DefaultCdcEventPublisherTest {
    
    private DefaultCdcEventPublisher publisher;
    private DatabaseChangeEvent sampleEvent;
    
    @BeforeEach
    void setUp() {
        publisher = new DefaultCdcEventPublisher();
        publisher.initialize();
        
        // Create a sample event
        sampleEvent = new DatabaseChangeEvent();
        sampleEvent.setEventId("test-event-123");
        sampleEvent.setOperationType(DatabaseChangeEvent.OperationType.INSERT);
        sampleEvent.setTableName("users");
        sampleEvent.setTimestamp(LocalDateTime.now());
        sampleEvent.setSource("test-source");
        
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put("id", 1);
        sampleEvent.setPrimaryKey(primaryKey);
    }
    
    @Test
    void testInitialize() {
        DefaultCdcEventPublisher newPublisher = new DefaultCdcEventPublisher();
        assertDoesNotThrow(() -> newPublisher.initialize());
        assertTrue(true); // Initialization should complete without exception
    }
    
    @Test
    void testPublishEvent() throws JsonProcessingException {
        // Mock the serializer to avoid actual serialization in tests
        EventSerializer mockSerializer = mock(EventSerializer.class);
        when(mockSerializer.serialize(any(DatabaseChangeEvent.class))).thenReturn("{\"test\":\"data\"}");
        
        // Replace the serializer in the publisher
        publisher = new DefaultCdcEventPublisher();
        // Since the publisher doesn't have a setter for serializer, we test the actual implementation
        // which uses a new EventSerializer() when not injected
        
        assertDoesNotThrow(() -> publisher.publishEvent(sampleEvent));
    }
    
    @Test
    void testPublishEventsBatch() {
        List<DatabaseChangeEvent> events = Arrays.asList(
            createSampleEvent("event-1"),
            createSampleEvent("event-2"),
            createSampleEvent("event-3")
        );
        
        assertDoesNotThrow(() -> publisher.publishEvents(events));
    }
    
    @Test
    void testPublishSerializedEvent() {
        assertDoesNotThrow(() -> publisher.publishSerializedEvent("{\"test\":\"data\"}", "test-type"));
    }
    
    @Test
    void testClose() {
        assertDoesNotThrow(() -> publisher.close());
    }
    
    @Test
    void testPublishEventWithNullEvent() {
        assertThrows(RuntimeException.class, () -> {
            publisher.publishEvent(null);
        });
    }
    
    @Test
    void testPublishEventsWithNullList() {
        assertDoesNotThrow(() -> publisher.publishEvents(null));
    }
    
    private DatabaseChangeEvent createSampleEvent(String eventId) {
        DatabaseChangeEvent event = new DatabaseChangeEvent();
        event.setEventId(eventId);
        event.setOperationType(DatabaseChangeEvent.OperationType.INSERT);
        event.setTableName("users");
        event.setTimestamp(LocalDateTime.now());
        event.setSource("test-source");
        
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put("id", 1);
        event.setPrimaryKey(primaryKey);
        
        return event;
    }
}